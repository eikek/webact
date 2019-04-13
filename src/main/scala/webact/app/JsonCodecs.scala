package webact.app

import java.time._
import java.nio.file.{Path, Paths}
import io.circe._

trait JsonCodecs {

  implicit def durationEncoder: Encoder[Duration] =
    Encoder.encodeString.contramap(_.toString)

  implicit def durationDecoder: Decoder[Duration] =
    Decoder.decodeString.map(s => Duration.parse(s))

  implicit def instantEncoder: Encoder[Instant] =
    Encoder.encodeString.contramap(_.toString)

  implicit def instantDecoder: Decoder[Instant] =
    Decoder.decodeString.map(s => Instant.parse(s))

  implicit def pathEncoder: Encoder[Path] =
    Encoder.encodeString.contramap(_.toAbsolutePath.normalize.toString)

  implicit def pathDecoder: Decoder[Path] =
    Decoder.decodeString.map(s => Paths.get(s))

}

object JsonCodecs extends JsonCodecs
