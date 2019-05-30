package webact.app

import fastparse._
import NoWhitespace._
import org.slf4j._

object MetaParser {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def parseMeta(content: String): MetaHeader =
    parse(content.replace("\r\n", "\n"), metaMap(_)) match {
      case Parsed.Success(v, _) => v
      case f@Parsed.Failure(a, b ,c) =>
        val trace = f.trace()
        logger.warn(s"Cannot parse script meta data at index ${trace.index}: ${trace.msg}")
        logger.warn(s"Content is: ${content}")
        MetaHeader(Key.Enabled -> "true")
    }

  def newline[_: P] = P("\n")

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
    P(CharsWhile(c => c != '\n').!)

  def keyValue[_: P](offset: Int): P[(String, String)] =
    (skip(offset) ~ " ".rep ~ key ~ " ".rep ~ ":" ~ " ".rep ~ value ~ newline)

  def keyValues[_: P](offset: Int): P[MetaHeader] =
    keyValue(offset).rep.map(makeMap)

  def description[_: P](offset: Int) = {
    (P(P(!webactEnd ~ AnyChar).rep.!).map { str =>
      str.split("\n").
        map(line => if (line.length > offset) line.substring(offset).trim else "\n").
        filter(_.nonEmpty).
        mkString("\n")
    }) ~ webactEnd
  }

  def metaMap[_:P]: P[MetaHeader] =
    gotoStart.flatMap { bol =>
      lineStart.flatMap { bow =>
        val offset = math.max(0, bow - bol - 2)
        P(webactStart ~ keyValues(offset) ~ description(offset)).map {
          case (m0, desc) => m0.updated(Key.Description, List(desc))
        }
      }
    }

  private def makeMap(p: Seq[(String, String)]): MetaHeader =
    MetaHeader.from(p)
}
