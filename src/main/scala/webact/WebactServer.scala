package webact

import cats.effect.{ConcurrentEffect, Timer, ContextShift}
import cats.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import fs2.Stream
import scala.concurrent.ExecutionContext

import org.http4s.server.middleware.Logger
import org.http4s.server.Router
import webact.config.Config
import webact.app._

object WebactServer {

  def stream[F[_]: ConcurrentEffect](cfg: Config, blockingEc: ExecutionContext)
    (implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    val app = for {
      scriptApp  <- ScriptAppImpl.create[F](cfg, blockingEc)
      _          <- scriptApp.init
      _          <- scriptApp.startMonitoring

      httpApp = Router(
        "/api/info" -> InfoRoutes.infoRoutes(cfg),
        "/api/v1" -> ScriptRoutes.scriptRoutes[F](scriptApp, blockingEc, cfg),
        "/app/assets" -> WebjarRoutes.appRoutes[F](blockingEc, cfg),
        "/app" -> TemplateRoutes.indexRoutes[F](blockingEc, cfg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(false, false)(httpApp)

    } yield finalHttpApp


    Stream.eval(app).flatMap(httpApp =>
      BlazeServerBuilder[F]
        .bindHttp(cfg.bind.port, cfg.bind.host)
        .withHttpApp(httpApp)
        .serve
    )

  }.drain
}
