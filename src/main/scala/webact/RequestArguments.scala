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
import scala.concurrent.ExecutionContext
import org.slf4j._

import webact.config._
import webact.app.File._

object RequestArguments {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def apply[F[_]: Sync](script: String
    , req: Request[F]
    , cfg: Config
    , blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Seq[Path]] = {
    req.attemptAs[Multipart[F]].fold(
      _ => fromBody(script, req, cfg, blockingEc),
      mp => fromMultipart(script, mp, req, cfg, blockingEc)).
      flatMap(identity)
  }

  def bracket[F[_]: Sync, B](script: String
    , req: Request[F]
    , cfg: Config
    , blockingEc: ExecutionContext)
    (use: Seq[Path] => F[B])
    (implicit C: ContextShift[F]): F[B] =
    Sync[F].bracket(apply(script, req, cfg, blockingEc))(use)(deleteAll[F])

  def  fromMultipart[F[_]: Sync](script: String
    , mp: Multipart[F]
    , req: Request[F]
    , cfg: Config
    , blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Seq[Path]] = {
    for {
      _     <- Sync[F].delay(logger.info(s"Creating arguments from multipart request for script $script"))
      dir   <- (cfg.tmpDir / script).mkdirs
      reqf  <- requestFile(req, dir)
      pfs   <- Traverse[Vector].sequence(mp.parts.map(p => makeFile(p.body, p.name, dir, blockingEc)))
    } yield Seq(reqf) ++ pfs
  }

  def fromBody[F[_]: Sync](script: String
    , req: Request[F]
    , cfg: Config
    , blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Seq[Path]] = {
    val fname = req.headers.get(`Content-Disposition`).
      flatMap(cd => cd.parameters.get("filename"))
    for {
      _     <- Sync[F].delay(logger.info(s"Creating arguments from basic request for script $script"))
      dir   <- (cfg.tmpDir / script).mkdirs
      reqf  <- requestFile(req, dir)
      body  <-
        if (req.method == Method.POST) makeFile(req.body, fname, dir, blockingEc).map(Seq(_))
        else Seq.empty.pure[F]
    } yield Seq(reqf) ++ body
  }

  private def requestFile[F[_]: Sync](req: Request[F], dir: Path): F[Path] = {
    for {
      tmp   <- dir.newTempFile("request-file-XXXXX", "json")
      src   <- tmp.replaceContent(RequestDetails(req))
      f     <- makeFinalFile(src, dir, Some("request"))
    } yield f
  }

  private def makeFile[F[_]: Sync](bytes: Stream[F, Byte]
    , name: Option[String]
    , dir: Path
    , blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Path] = {
    for {
      tmp   <- dir.newTempFile("arg-file-XXXXX", "out")
      _     <- bytes.through(file.writeAll(tmp, blockingEc, Seq.empty)).compile.drain
      f     <- makeFinalFile(tmp, dir, name)
    } yield f
  }

  private def makeFinalFile[F[_]: Sync](tmp: Path, dir: Path, name: Option[String]): F[Path] = {
    for {
      sha   <- tmp.sha256
      fname <- name.map(n => sha + "-" + base64(n)).getOrElse(sha).pure[F]
      file  <- tmp.moveTo(dir / fname)
    } yield file
  }

  private def deleteAll[F[_]: Sync](paths: Seq[Path]): F[Unit] =
    Traverse[List].sequence(paths.toList.map(p => p.delete)).map(_ => ())

  private def base64(plain: String): String =
    java.util.Base64.getEncoder.encodeToString(plain.getBytes(defaultCharset))

  case class RequestDetails( uri: String
    , method: String
    , headers: Map[String, String]
    , params: Map[String, Seq[String]]
    , remoteAddr: Option[String]
    , remoteHost: Option[String]
    , remotePort: Option[Int]
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
