package webact

import cats.effect.{ConcurrentEffect, Timer, ContextShift}
//import cats.implicits._
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
    val scriptApp = ScriptAppImpl.create[F](cfg, blockingEc)
    val httpApp = Router(
      "/api/v1" -> ScriptRoutes.scriptRoutes[F](scriptApp, blockingEc, cfg),
      "/app/assets" -> WebjarRoutes.appRoutes[F](blockingEc, cfg),
      "/app" -> TemplateRoutes.indexRoutes[F](blockingEc, cfg)
    ).orNotFound
      // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    val server = BlazeServerBuilder[F]
      .bindHttp(cfg.bind.port, cfg.bind.host)
      .withHttpApp(finalHttpApp)
      .serve

    Stream.eval(scriptApp.init) ++ server
  }.drain
}
