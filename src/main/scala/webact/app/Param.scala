package webact.app

import fastparse._
import NoWhitespace._
import org.slf4j._

case class Param(name: Option[String], format: Param.Format)

object Param {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  sealed trait Format {
    self: Product =>
    def name: String = productPrefix
  }
  object Format {
    case object Line extends Format
    case object Text extends Format
    case object Password extends Format
    case object File extends Format
    case object Files extends Format
  }

  object Parser {

    def format[_: P]: P[Format] =
      StringInIgnoreCase("line", "text", "password", "file", "files").!.map(_.toLowerCase).map {
        case "line" => Format.Line
        case "text" => Format.Text
        case "password" => Format.Password
        case "file" => Format.File
        case "files" => Format.Files
        case _ => Format.Line
      }

    def nameFormat[_: P]: P[Param] =
      P(CharPred(c => c != '=').rep(1).! ~ "=" ~ format).map {
        case (name, fmt) => Param(Some(name), fmt)
      }

    def formatOnly[_: P]: P[Param] =
      format.map(fmt => Param(None, fmt))

    def param[_: P]: P[Param] =
      nameFormat | formatOnly
  }

  def fromString(str: String): Either[String, Param] =
    parse(str, Parser.param(_)) match {
      case Parsed.Success(v, _) => Right(v)
      case f@Parsed.Failure(a, b ,c) =>
        val trace = f.trace()
        Left(s"Cannot parse param description at index ${trace.index}: ${trace.msg}")
    }

  def fromStringLog(str: String): Option[Param] =
    fromString(str) match {
      case Right(v) =>
        Some(v)
      case Left(msg) =>
        logger.warn(msg)
        None
    }
}
