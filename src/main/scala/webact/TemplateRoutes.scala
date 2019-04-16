package webact

import fs2._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import _root_.io.circe._
import _root_.io.circe.generic.semiauto._
import _root_.io.circe.syntax._
import yamusca.imports._
import yamusca.implicits._
import scala.concurrent.ExecutionContext
import java.net.URL

import webact.config._

object TemplateRoutes {
  val `text/html` = new MediaType("text", "html")


  def indexRoutes[F[_]: Effect](blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {

    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "index.html" =>
        for {
          src    <- loadResource("/index.html", blockingEc)
          templ  <- parseTemplate(src)
          resp   <- Ok(IndexData(cfg).render(templ), `Content-Type`(`text/html`))
        } yield resp
      case GET -> Root / "doc" =>
        for {
          src    <- loadResource("/doc.html", blockingEc)
          templ  <- parseTemplate(src)
          resp   <- Ok(DocData(cfg).render(templ), `Content-Type`(`text/html`))
        } yield resp
    }
  }

  def loadUrl[F[_]: Sync](url: URL, blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[String] =
    Stream.bracket(Sync[F].delay(url.openStream))(in => Sync[F].delay(in.close)).
      flatMap(in => io.readInputStream(in.pure[F], 64 * 1024, blockingEc, false)).
      through(text.utf8Decode).
      compile.fold("")(_ + _)

  def loadResource[F[_]: Sync](name: String, blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[String] = {
    Option(getClass.getResource(name)) match {
      case None =>
        sys.error("Unknown resource: "+ name)
      case Some(r) =>
        loadUrl(r, blockingEc)
    }
  }

  def parseTemplate[F[_]: Sync](str: String): F[Template] =
    Sync[F].delay {
      mustache.parse(str) match {
        case Right(t) => t
        case Left((_, err)) => sys.error(err)
      }
    }

  case class DocData(swaggerRoot: String, openapiSpec: String)
  object DocData {

    def apply(cfg: Config): DocData =
      DocData("/app/assets" + Webjars.swaggerui, s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/openapi.yml")

    implicit def yamuscaValueConverter: ValueConverter[DocData] =
      ValueConverter.deriveConverter[DocData]
  }

  case class Flags(appName: String, apiBase: String)

  object Flags {
    def apply(cfg: Config): Flags =
      Flags(cfg.appName, "/api/v1")

    implicit val jsonEncoder: Encoder[Flags] =
      deriveEncoder[Flags]
    implicit def yamuscaValueConverter: ValueConverter[Flags] =
      ValueConverter.deriveConverter[Flags]
  }

  case class IndexData(flags: Flags
    , cssUrls: Seq[String]
    , jsUrls: Seq[String]
    , appExtraJs: String
    , flagsJson: String)

  object IndexData {

    def apply(cfg: Config): IndexData =
      IndexData(Flags(cfg)
        , Seq(
          "/app/assets" + Webjars.semanticui + "/semantic.min.css",
          s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/webact.css"
        )
        , Seq(
          "/app/assets" + Webjars.jquery + "/jquery.min.js",
          "/app/assets" + Webjars.semanticui + "/semantic.min.js",
          s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/webact-app.js"
        )
        ,
        s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/webact.js"
          , Flags(cfg).asJson.spaces2 )

    implicit def yamuscaValueConverter: ValueConverter[IndexData] =
      ValueConverter.deriveConverter[IndexData]
  }
}
