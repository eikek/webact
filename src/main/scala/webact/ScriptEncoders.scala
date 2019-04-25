package webact

import cats.Applicative
import org.http4s.EntityEncoder
import org.http4s.circe._

import webact.model._

trait ScriptEncoders {

  implicit def scriptDetailEntityEncoder[F[_]: Applicative]: EntityEncoder[F, ScriptDetail] =
    jsonEncoderOf

  implicit def scriptOutputEntityEncoder[F[_]: Applicative]: EntityEncoder[F, ScriptOutput] =
    jsonEncoderOf

  implicit def scriptDetailListEntityEncoder[F[_]: Applicative]: EntityEncoder[F, List[ScriptDetail]] =
    jsonEncoderOf

  implicit def scriptInfoEntityEncoder[F[_]: Applicative]: EntityEncoder[F, ScriptInfo] =
    jsonEncoderOf

  implicit def scriptInfoListEntityEncoder[F[_]: Applicative]: EntityEncoder[F, List[ScriptInfo]] =
    jsonEncoderOf

  implicit def runningInfoEntityEncoder[F[_]: Applicative]: EntityEncoder[F, RunningInfo] =
    jsonEncoderOf
}
