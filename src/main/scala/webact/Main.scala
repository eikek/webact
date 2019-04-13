package webact

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

import webact.config.Config

object Main extends IOApp {
  val blockingEc: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def run(args: List[String]) = {
    args match {
      case "-f" :: file :: Nil =>
        System.setProperty("config.file", file)
      case _ =>
    }

    val cfg = Config.default
    WebactServer.stream[IO](cfg, blockingEc).compile.drain.as(ExitCode.Success)
  }
}
