package webact.app

import fs2._
import scala.concurrent.duration.FiniteDuration
import java.nio.file.Path
import java.time.Instant

trait ScriptApp[F[_]] {

  def listAll: Stream[F, Script[F]]

  def find(name: String): F[Option[Script[F]]]

  def store(name: String, bytes: Stream[F, Byte]): F[Unit]

  def execute(name: String, args: Seq[Path], deletArgsAfterRun: Boolean): F[F[Option[(Script[F], Output)]]]

  def isExecuting(name: String): F[Option[Instant]]

  def findOutput(name: String): F[Option[Output]]

  def findSchedule(name: String): Option[ScheduleData[F]]

  def scheduleRun(name: String, when: FiniteDuration): F[ScheduleData[F]]

  def schedule(name: String, timer: String): F[Option[ScheduleData[F]]]

  def init: F[Unit]

  def startMonitoring: F[F[Unit]]
}
