package webact.app

import fs2._
import scala.concurrent.duration.FiniteDuration
import java.nio.file.Path

trait ScriptApp[F[_]] {

  def listAll: Stream[F, Script[F]]

  def find(name: String): F[Option[Script[F]]]

  def store(name: String, bytes: Stream[F, Byte]): F[Unit]

  def execute(name: String, args: Seq[Path]): F[F[Option[Output]]]

  def findOutput(name: String): F[Option[Output]]

  def cancelSchedule(name: String): F[Unit]

  def scheduleRun(name: String, when: FiniteDuration): F[F[Unit]]

  def schedule(name: String, timer: String): F[F[Unit]]

  def init: F[Unit]
}
