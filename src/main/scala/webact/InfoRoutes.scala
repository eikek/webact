package webact

import cats.effect._
import org.http4s._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import io.circe._, io.circe.generic.semiauto._

import webact.config._

object InfoRoutes {

  def infoRoutes[F[_]: Sync](cfg: Config): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> (Root / "version") =>
        Ok(VersionInfo())
    }
  }

  case class VersionInfo(version: String = BuildInfo.version
    , builtAtMillis: Long = BuildInfo.builtAtMillis
    , builtAtString: String = BuildInfo.builtAtString
    , gitCommit: String = BuildInfo.gitHeadCommit.getOrElse("")
    , gitVersion: String = BuildInfo.gitDescribedVersion.getOrElse("")
  )

  object VersionInfo {
    implicit val jsonEncoder: Encoder[VersionInfo] = deriveEncoder[VersionInfo]
  }
}
