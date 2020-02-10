package webact.app

import java.time._
import java.nio.file.Path
import io.circe._
import io.circe.generic.semiauto._

import JsonCodecs._

case class Output(
    runAt: Instant,
    runTime: Duration,
    returnCode: Int,
    success: Boolean,
    runCount: Int,
    runSuccess: Int,
    stdout: Path,
    stderr: Path
) {

  def updateCounter(other: Output): Output =
    copy(runCount = runCount + other.runCount, runSuccess = runSuccess + other.runSuccess)

  def updateCounter(other: Option[Output]): Output =
    other.map(updateCounter).getOrElse(this)

  def failure: Boolean =
    !success
}

object Output {

  implicit val jsonEncoder: Encoder[Output] = deriveEncoder[Output]
  implicit val jsonDecoder: Decoder[Output] = deriveDecoder[Output]

}
