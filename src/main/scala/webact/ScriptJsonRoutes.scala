package webact

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import java.time._

import webact.app._
import webact.model._
import webact.config._

object ScriptJsonRoutes {
  val `text/plain` = new MediaType("text", "plain")
  val noCache = `Cache-Control`(CacheDirective.`no-cache`())

  def routes[F[_]: Sync](
      S: ScriptApp[F],
      blocker: Blocker,
      cfg: Config
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {

      case GET -> Root / "scripts" / name / "output" =>
        for {
          out <- S.findOutput(name)
          resp <- out.map(o => Ok(output(o))).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name =>
        for {
          script <- S.find(name)
          sch <- S.findSchedule(name).pure[F]
          exe <- S.isExecuting(name)
          out <- S.findOutput(name)
          resp <- script
                   .map(s => Ok(ScriptInfo(detail(s, sch, exe), out.map(output(_)))))
                   .getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name / "running" =>
        for {
          exe <- S.isExecuting(name)
          resp <- Ok(
                   RunningInfo(
                     exe.map(i => Duration.between(i, Instant.now).toMillis).getOrElse(0)
                   )
                 )
        } yield resp

      case GET -> Root / "scripts" =>
        S.listAll
          .evalMap(s =>
            s.meta.getHead(Key.Name) match {
              case Some(name) =>
                for {
                  out <- S.findOutput(name)
                  exe <- S.isExecuting(name)
                } yield ScriptInfo(detail(s, S.findSchedule(name), exe), out.map(output))
              case None =>
                val name = s.meta.getHead(Key.Name)
                for {
                  exe <- name.map(S.isExecuting).getOrElse(None.pure[F])
                } yield ScriptInfo(detail(s, name.flatMap(S.findSchedule), exe), None)
            }
          )
          .compile
          .toList
          .flatMap(list => Ok(list.sortBy(_.script.name)))
    }
  }

  private def detail[F[_]](
      script: Script[F],
      sch: Option[ScheduleData[F]],
      executing: Option[Instant]
  ): ScriptDetail =
    ScriptDetail(
      script.meta.getHeadOr(Key.Name, ""),
      script.meta.get(Key.Category),
      script.meta.getHeadOr(Key.LastMod, "0").toLong,
      script.meta.getHeadOr(Key.Description, ""),
      script.meta.getHeadOr(Key.Schedule, ""),
      sch.map(_.time.toString).getOrElse(""),
      executing.map(i => Duration.between(i, Instant.now).toMillis).getOrElse(0),
      script.meta.getHeadOr(Key.Enabled, "false").equalsIgnoreCase("true"),
      script.meta.getHeadOr(Key.NotifyMail, ""),
      script.meta.getHeadOr(Key.NotifyErrorMail, ""),
      script.meta
        .get(Key.Param)
        .flatMap(Param.fromStringLog)
        .map(p => Parameter(p.name.getOrElse(""), p.format.name))
    )

  private def output(o: Output): ScriptOutput =
    ScriptOutput(
      o.runAt.toString,
      o.returnCode,
      o.success,
      o.runTime.toMillis,
      o.runCount,
      o.runSuccess
    )

}
