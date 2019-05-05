package webact

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import scala.concurrent.ExecutionContext
import java.time._

import webact.app._
import webact.model._
import webact.config._

object ScriptRoutes {
  val `text/plain` = new MediaType("text", "plain")
  val noCache = `Cache-Control`(CacheDirective.`no-cache`())

  def scriptRoutes[F[_]: Sync](S: ScriptApp[F], blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "scripts" / name / "run" =>
        for {
          args  <- RequestArguments(name, req, cfg, blockingEc)
          _     <- S.execute(name, args, deletArgsAfterRun = true)
          resp  <- Ok()
        } yield resp

      case req @ POST -> Root / "scripts" / name / "runsync" =>
        for {
          args   <- RequestArguments(name, req, cfg, blockingEc)
          fout   <- S.execute(name, args, deletArgsAfterRun = true)
          optout <- fout
          resp   <- optout.map(makeResponse(dsl, req, blockingEc)).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "runsync" =>
        for {
          args   <- RequestArguments(name, req, cfg, blockingEc)
          fout   <- S.execute(name, args, deletArgsAfterRun = true)
          optout <- fout
          resp   <- optout.map(makeResponse(dsl, req, blockingEc)).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name / "output" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => Ok(output(o))).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stdout" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => StaticFile.fromFile(o.stdout.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stderr" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => StaticFile.fromFile(o.stderr.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name =>
        for {
          script <- S.find(name)
          sch    <- S.findSchedule(name).pure[F]
          exe    <- S.isExecuting(name)
          out    <- S.findOutput(name)
          resp   <- script.map(s => Ok(ScriptInfo(detail(s, sch, exe), out.map(output(_))))).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name / "running" =>
        for {
          exe  <- S.isExecuting(name)
          resp <- Ok(RunningInfo(exe.map(i => Duration.between(i, Instant.now).toMillis).getOrElse(0)))
        } yield resp

      case GET -> Root / "scripts" / name / "content" =>
        import EntityEncoder.streamEncoder //ambigous implicits with CirceEncoders
        for {
          sc   <- S.find(name)
          resp <- sc.map(o => Ok(o.content, `Content-Type`(`text/plain`)).
            map(_.withHeaders(noCache))).
            getOrElse(NotFound())
        } yield resp

      case req @ PUT -> Root / "scripts" / name =>
        S.store(name, req.body).flatMap(_ => Ok())

      case req @ POST -> Root / "scripts" / name =>
        for {
          mp    <- req.as[Multipart[F]]
          resp  <- mp.parts.find(_.name.contains("script")).map(p => S.store(name, p.body).flatMap(_ => Ok())).getOrElse(BadRequest())
        } yield resp

      case GET -> Root / "scripts" =>
        S.listAll.
          evalMap(s => s.meta.getHead(Key.Name) match {
            case Some(name) =>
              for {
                out   <- S.findOutput(name)
                exe   <- S.isExecuting(name)
              } yield ScriptInfo(detail(s, S.findSchedule(name), exe), out.map(output))
            case None =>
              val name = s.meta.getHead(Key.Name)
              for {
                exe   <- name.map(S.isExecuting).getOrElse(None.pure[F])
              } yield ScriptInfo(detail(s, name.flatMap(S.findSchedule), exe), None)
          }).
          compile.toList.
          flatMap(list => Ok(list.sortBy(_.script.name)))
    }
  }

  private def detail[F[_]](script: Script[F], sch: Option[ScheduleData[F]], executing: Option[Instant]): ScriptDetail =
    ScriptDetail(script.meta.getHeadOr(Key.Name, "")
      , script.meta.get(Key.Category)
      , script.meta.getHeadOr(Key.LastMod, "0").toLong
      , script.meta.getHeadOr(Key.Description, "")
      , script.meta.getHeadOr(Key.Schedule, "")
      , sch.map(_.time.toString).getOrElse("")
      , executing.map(i => Duration.between(i, Instant.now).toMillis).getOrElse(0)
      , script.meta.getHeadOr(Key.Enabled, "false").equalsIgnoreCase("true")
      , script.meta.getHeadOr(Key.NotifyMail, "")
      , script.meta.getHeadOr(Key.NotifyErrorMail, "")
      , script.meta.get(Key.Param).flatMap(Param.fromStringLog).map(p => Parameter(p.name.getOrElse(""), p.format.name)))

  private def output(o: Output): ScriptOutput =
    ScriptOutput(o.runAt.toString
      , o.returnCode
      , o.success
      , o.runTime.toMillis
      , o.runCount
      , o.runSuccess)

  private def makeResponse[F[_]: Sync](dsl: Http4sDsl[F]
    , req: Request[F]
    , blockingEc: ExecutionContext)
    (p: (Script[F], Output))
    (implicit C: ContextShift[F]) = {
    import dsl._
    val (script, out) = p

    val contentType: MediaType = script.meta.getHead(Key.ContentType).
      map(_.trim).
      map(MediaType.parse).
      flatMap(_.toOption).
      getOrElse(`text/plain`)

    val sentErr = script.meta.getHeadOr(Key.SentStdErr, "Never")

    val file =
      if (sentErr.equalsIgnoreCase("always")) out.stderr.toFile
      else if (sentErr.equalsIgnoreCase("onerror") && !out.success) out.stderr.toFile
      else out.stdout.toFile

    val resp = StaticFile.fromFile(file, blockingEc, Some(req)).
      map(_.withHeaders(noCache).withHeaders(`Content-Type`(contentType))).
      getOrElseF(NotFound())

    if (out.success) resp
    else if (script.meta.get(Key.BadInputCode).contains(out.returnCode.toString)) {
      resp.map(_.withStatus(Status.BadRequest))
    } else {
      resp.map(_.withStatus(Status.InternalServerError))
    }
  }
}
