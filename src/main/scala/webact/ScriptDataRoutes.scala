package webact

import fs2.Stream
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import webact.app._
import webact.config._

object ScriptDataRoutes {
  val `text/plain` = new MediaType("text", "plain")
  val noCache      = `Cache-Control`(CacheDirective.`no-cache`())

  def routes[F[_]: Sync](S: ScriptApp[F], blocker: Blocker, cfg: Config)(
      implicit C: ContextShift[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "scripts" / name / "run" =>
        for {
          args <- RequestArguments(name, req, cfg, blocker)
          _    <- S.execute(name, args, deletArgsAfterRun = true)
          resp <- Ok()
        } yield resp

      case req @ POST -> Root / "scripts" / name / "runsync" =>
        for {
          args   <- RequestArguments(name, req, cfg, blocker)
          fout   <- S.execute(name, args, deletArgsAfterRun = true)
          optout <- fout
          resp   <- optout.map(makeResponse(dsl, req, blocker)).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "runsync" =>
        for {
          args   <- RequestArguments(name, req, cfg, blocker)
          fout   <- S.execute(name, args, deletArgsAfterRun = true)
          optout <- fout
          resp   <- optout.map(makeResponse(dsl, req, blocker)).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stdout" =>
        for {
          out <- S.findOutput(name)
          resp <- out
            .map(o =>
              StaticFile
                .fromFile(o.stdout.toFile, blocker, Some(req))
                .map(_.withHeaders(noCache))
                .getOrElseF(NotFound())
            )
            .getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "scripts" / name / "output" / "stderr" =>
        for {
          out <- S.findOutput(name)
          resp <- out
            .map(o =>
              StaticFile
                .fromFile(o.stderr.toFile, blocker, Some(req))
                .map(_.withHeaders(noCache))
                .getOrElseF(NotFound())
            )
            .getOrElse(NotFound())
        } yield resp

      case GET -> Root / "scripts" / name / "content" =>
        implicit val enc: EntityEncoder[F, Stream[F, String]] =
          EntityEncoder
            .streamEncoder[F, String](EntityEncoder.stringEncoder[F])
            .withContentType(`Content-Type`(`text/plain`))
        for {
          sc <- S.find(name)
          resp <- sc
            .map(o => Ok(o.asUtf8, `Content-Type`(`text/plain`)).map(_.withHeaders(noCache)))
            .getOrElse(NotFound())
        } yield resp

      case req @ PUT -> Root / "scripts" / name =>
        S.store(name, req.body).flatMap(_ => Ok())

      case req @ POST -> Root / "scripts" / name =>
        for {
          mp <- req.as[Multipart[F]]
          resp <- mp.parts
            .find(_.name.contains("script"))
            .map(p => S.store(name, p.body).flatMap(_ => Ok()))
            .getOrElse(BadRequest())
        } yield resp
    }
  }

  private def makeResponse[F[_]: Sync](dsl: Http4sDsl[F], req: Request[F], blocker: Blocker)(
      p: (Script[F], Output)
  )(implicit C: ContextShift[F]) = {
    import dsl._
    val (script, out) = p

    val contentType: MediaType = script.meta
      .getHead(Key.ContentType)
      .map(_.trim)
      .map(MediaType.parse)
      .flatMap(_.toOption)
      .getOrElse(`text/plain`)

    val sentErr = script.meta.getHeadOr(Key.SentStdErr, "Never")

    val file =
      if (sentErr.equalsIgnoreCase("always")) out.stderr.toFile
      else if (sentErr.equalsIgnoreCase("onerror") && !out.success) out.stderr.toFile
      else out.stdout.toFile

    val resp = StaticFile
      .fromFile(file, blocker, Some(req))
      .map(_.withHeaders(noCache).withHeaders(`Content-Type`(contentType)))
      .getOrElseF(NotFound())

    if (out.success) resp
    else if (script.meta.get(Key.BadInputCode).contains(out.returnCode.toString)) {
      resp.map(_.withStatus(Status.BadRequest))
    } else {
      resp.map(_.withStatus(Status.InternalServerError))
    }
  }
}
