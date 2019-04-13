package webact.app

import fastparse._
import NoWhitespace._
import org.slf4j._

object MetaParser {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def parseMeta(content: String): Map[Key, String] =
    parse(content, metaMap(_)) match {
      case Parsed.Success(v, _) => v.collect(Function.unlift({
        case (k, v) =>
          Key.from(k).map(k0 => k0 -> v)
      }))
      case f@Parsed.Failure(a, b ,c) =>
        val trace = f.trace()
        logger.warn(s"Cannot parse script meta data at index ${trace.index}: ${trace.msg}")
        logger.warn(s"Content is: ${content}")
        Map(Key.Enabled -> "false")
    }

  def newline[_: P] = P("\r\n" | "\n")

  def webactStart[_: P] = "<webact>"
  def webactEnd[_: P] = "</webact>"

  def webactStartLine[_: P] =
    P(newline ~ P(!webactStart ~ AnyChar).rep)

  def lineStart[_: P]: P[Int] = P(P(!webactStart ~ AnyChar).rep ~ Index)

  def skip[_: P](count: Int) = AnyChar.rep(exactly = count)

  def gotoStart[_: P]: P[Int] =
    P(!webactStartLine ~ AnyChar).rep ~ newline ~ Index


  def key[_: P]: P[String] =
    P(CharIn("a-z", "A-Z") ~ CharIn("a-z", "A-Z", "0-9", "+_\\-").rep).!

  def value[_: P]: P[String] =
    P(CharsWhile(c => c != '\n' && c != '\r').!)

  def keyValue[_: P](offset: Int): P[(String, String)] =
    (skip(offset) ~ " ".rep ~ key ~ " ".rep ~ P(":") ~ " ".rep ~ value ~ newline)

  def keyValues[_: P](offset: Int): P[Map[String, String]] =
    keyValue(offset).rep.map(_.toMap)

  def description[_: P](offset: Int) = {
    (P(P(!webactEnd ~ AnyChar).rep.!).map { str =>
      str.split("\r\n|\n").
        map(line => if (line.length > offset) line.substring(offset) else "").
        filter(_.nonEmpty).
        mkString("\n")
    }) ~ webactEnd
  }

  def metaMap[_:P]: P[Map[String,String]] =
    gotoStart.flatMap { bol =>
      lineStart.flatMap { bow =>
        val offset = math.max(0, bow - bol - 2)
        P(webactStart ~ newline ~ keyValues(offset) ~ description(offset)).map {
          case (m0, desc) => m0.updated("Description", desc)
        }
      }
    }
}
