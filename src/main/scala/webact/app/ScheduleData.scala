package webact.app

import java.time._

case class ScheduleData[F[_]](name: String, cancel: F[Unit], time: LocalDateTime)
