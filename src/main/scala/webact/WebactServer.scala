package webact

import cats.effect._
import cats.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import fs2.Stream

import org.http4s.server.middleware.Logger
import org.http4s.server.Router
import webact.config.Config
import webact.app._

object WebactServer {

  def stream[F[_]: ConcurrentEffect](
      cfg: Config,
      blocker: Blocker
  )(implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    val app = for {
      scriptApp <- ScriptAppImpl.create[F](cfg, blocker)
      _         <- scriptApp.init
      _         <- scriptApp.startMonitoring

      httpApp = Router(
        "/api/info" -> InfoRoutes.infoRoutes(cfg),
        "/api/v1" -> (ScriptJsonRoutes.routes[F](scriptApp, blocker, cfg) <+> ScriptDataRoutes
          .routes[F](scriptApp, blocker, cfg)),
        "/app/assets" -> WebjarRoutes.appRoutes[F](blocker, cfg),
        "/app"        -> TemplateRoutes.indexRoutes[F](blocker, cfg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(false, false)(httpApp)

    } yield finalHttpApp

    Stream
      .eval(app)
      .flatMap(httpApp =>
        BlazeServerBuilder[F]
          .bindHttp(cfg.bind.port, cfg.bind.host)
          .withHttpApp(httpApp)
          .serve
      )

  }.drain
}
