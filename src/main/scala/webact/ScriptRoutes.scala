package webact

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import scala.concurrent.ExecutionContext

import webact.app._
import webact.model._
import webact.config._

object ScriptRoutes extends ScriptEncoders {
  val `text/plain` = new MediaType("text", "plain")

  def scriptRoutes[F[_]: Sync](S: ScriptApp[F], blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "scripts" / name / "run" =>
        RequestArguments.bracket(name, req, cfg, blockingEc) { args =>
          for {
            _    <- S.execute(name, args)
            resp <- Ok()
          } yield resp
        }

      case req @ GET -> Root / "scripts" / name / "run" =>
        RequestArguments.bracket(name, req, cfg, blockingEc) { args =>
          for {
            _    <- S.execute(name, args)
            resp <- Ok()
          } yield resp
        }

      case GET -> Root / "scripts" / name / "output" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => Ok(output(o))).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stdout" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => StaticFile.fromFile(o.stdout.toFile, blockingEc, Some(req)).getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stderr" =>
        for {
          out  <- S.findOutput(name)
          resp <- out.map(o => StaticFile.fromFile(o.stderr.toFile, blockingEc, Some(req)).getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name =>
        for {
          script <- S.find(name)
          resp <- script.map(s => Ok(detail(s))).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name / "content" =>
        for {
          sc   <- S.find(name)
          resp <- sc.map(o => Ok(o.content, `Content-Type`(`text/plain`))).getOrElse(NotFound())
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
          map(detail).
          compile.toList.
          flatMap(list => Ok(list))
    }
  }

  private def detail[F[_]](script: Script[F]): ScriptDetail =
    ScriptDetail(script.get(Key.Name)
      , script.get(Key.Category)
      , script.get(Key.LastMod).toLong
      , script.get(Key.Description)
      , script.get(Key.Schedule)
      , !script.get(Key.Enabled).equalsIgnoreCase("false")
      , script.get(Key.NotifyMail)
      , script.get(Key.NotifyErrorMail))

  private def output(o: Output): ScriptOutput =
    ScriptOutput(o.runAt.toString
      , o.returnCode
      , o.runTime.toMillis
      , o.runCount
      , o.runSuccess)
}
