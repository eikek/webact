package webact

import cats.effect._
import org.http4s._
import org.http4s.HttpRoutes
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{WebjarAsset, Config => WebjarConfig}
import scala.concurrent.ExecutionContext

import webact.config._

object WebjarRoutes {

  def appRoutes[F[_]: Effect](blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {
    webjarService(
      WebjarConfig(
        filter = assetFilter,
        blockingExecutionContext = blockingEc
      )
    )
  }

  def assetFilter(asset: WebjarAsset): Boolean =
    List(".js", ".css", ".html", ".jpg", ".png", ".eot", ".woff", ".woff2", ".svg", ".otf", ".ttf", ".yml").
      exists(e => asset.asset.endsWith(e))

}
