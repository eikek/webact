package webact

import fs2.Stream
import fs2.io.file
import cats.Traverse
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.headers._
import org.http4s.multipart._
import java.nio.file.Path
import org.slf4j._

import webact.config._
import webact.app.File._

object RequestArguments {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def apply[F[_]: Sync](script: String, req: Request[F], cfg: Config, blocker: Blocker)(
      implicit C: ContextShift[F]
  ): F[Seq[Path]] =
    req
      .attemptAs[Multipart[F]]
      .fold(
        _ => fromBody(script, req, cfg, blocker),
        mp => fromMultipart(script, mp, req, cfg, blocker)
      )
      .flatMap(identity)

  def fromMultipart[F[_]: Sync](
      script: String,
      mp: Multipart[F],
      req: Request[F],
      cfg: Config,
      blocker: Blocker
  )(implicit C: ContextShift[F]): F[Seq[Path]] =
    for {
      _ <- Sync[F].delay(
        logger.info(s"Creating arguments from multipart request for script $script")
      )
      dir  <- (cfg.tmpDir / script).mkdirs
      reqf <- requestFile(req, dir)
      pfs  <- Traverse[Vector].sequence(mp.parts.map(p => makeFile(p.body, p.name, dir, blocker)))
    } yield Seq(reqf) ++ pfs

  def fromBody[F[_]: Sync](script: String, req: Request[F], cfg: Config, blocker: Blocker)(
      implicit C: ContextShift[F]
  ): F[Seq[Path]] = {
    val fname = req.headers.get(`Content-Disposition`).flatMap(cd => cd.parameters.get("filename"))
    for {
      _    <- Sync[F].delay(logger.info(s"Creating arguments from basic request for script $script"))
      dir  <- (cfg.tmpDir / script).mkdirs
      reqf <- requestFile(req, dir)
      body <- if (req.method == Method.POST) makeFile(req.body, fname, dir, blocker).map(Seq(_))
      else Seq.empty.pure[F]
    } yield Seq(reqf) ++ body
  }

  private def requestFile[F[_]: Sync](req: Request[F], dir: Path): F[Path] =
    for {
      tmp <- dir.newTempFile("request-file-XXXXX", "json")
      src <- tmp.replaceContent(RequestDetails(req))
      f   <- makeFinalFile(src, dir, Some("request"))
    } yield f

  private def makeFile[F[_]: Sync](
      bytes: Stream[F, Byte],
      name: Option[String],
      dir: Path,
      blocker: Blocker
  )(implicit C: ContextShift[F]): F[Path] =
    for {
      tmp <- dir.newTempFile("arg-file-XXXXX", "out")
      _   <- Sync[F].delay(logger.debug(s"Make argument file $tmp"))
      _   <- bytes.through(file.writeAll(tmp, blocker, Seq.empty)).compile.drain
      f   <- makeFinalFile(tmp, dir, name)
    } yield f

  private def makeFinalFile[F[_]: Sync](tmp: Path, dir: Path, name: Option[String]): F[Path] =
    for {
      sha   <- tmp.sha256
      fname <- name.map(n => sha + "-" + base64(n)).getOrElse(sha).pure[F]
      file  <- tmp.moveTo(dir / fname)
      _     <- Sync[F].delay(logger.debug(s"Created argument file $file"))
    } yield file

  private def base64(plain: String): String =
    java.util.Base64.getEncoder.encodeToString(plain.getBytes(defaultCharset))

  case class RequestDetails(
      uri: String,
      method: String,
      headers: Map[String, String],
      params: Map[String, Seq[String]],
      remoteAddr: Option[String],
      remoteHost: Option[String],
      remotePort: Option[Int]
  )

  object RequestDetails {
    implicit val jsonDecoder: Decoder[RequestDetails] = deriveDecoder[RequestDetails]
    implicit val jsonEncoder: Encoder[RequestDetails] = deriveEncoder[RequestDetails]

    def apply[F[_]](req: Request[F]): RequestDetails =
      RequestDetails(
        req.uri.renderString,
        req.method.name,
        req.headers.toList.map(h => (h.name.value, h.value)).toMap,
        req.multiParams,
        req.remoteAddr,
        req.remoteHost,
        req.remotePort
      )
  }
}
