package webact

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.nio.file.{Files, Paths}
import org.slf4j._
import webact.config.Config

object Main extends IOApp {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val blockingEc: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def run(args: List[String]) = {
    args match {
      case file :: Nil =>
        val path = Paths.get(file).toAbsolutePath.normalize
        logger.info(s"Using given config file: $path")
        System.setProperty("config.file", file)
      case _ =>
        Option(System.getProperty("config.file")) match {
          case Some(f) if f.nonEmpty =>
            val path = Paths.get(f).toAbsolutePath.normalize
            if (!Files.exists(path)) {
              logger.info(s"Not using config file '$f' because it doesn't exist")
              System.clearProperty("config.file")
            } else {
              logger.info(s"Using config file from system properties: $f")
            }
          case _ =>
        }
    }

    val cfg = Config.default
    WebactServer.stream[IO](cfg, blockingEc).compile.drain.as(ExitCode.Success)
  }
}
