package webact.app

import fastparse._
import NoWhitespace._
import org.slf4j._

object MetaParser {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def parseMeta(content: String): MetaHeader = {
    val in = cutWebactBlock(content)
    parse(in, metaMap(_)) match {
      case Parsed.Success(v, _) => v
      case f@Parsed.Failure(a, b ,c) =>
        val trace = f.trace()
        logger.warn(s"Cannot parse script meta data at index ${trace.index}: ${trace.msg}")
        logger.warn(s"Content is: ${content}")
        MetaHeader(Key.Enabled -> "true")
    }
  }

  def cutWebactBlock(str: String): String = {
    val block = (str.indexOf("<webact>") match {
      case -1 => ""
      case n =>
        val k = str.lastIndexOf('\n', n) match {
          case -1 => 0
          case x => x
        }
        str.indexOf("</webact>", n) match {
          case -1 => ""
          case m => str.substring(k, m+10).trim
        }
    }).trim
    val prefix = block.indexOf("<webact>")
    if (prefix <= 0) block.replace("\r\n", "\n")
    else block.replace("\r\n", "\n").
      split('\n').
      map(line => if (line.length > prefix) line.substring(prefix) else "").
      mkString("\n")
  }

  def newline[_: P] = P("\n")

  def webactStart[_: P] = "<webact>"
  def webactEnd[_: P] = "</webact>"

  def key[_: P]: P[String] =
    P(CharIn("a-z", "A-Z") ~ CharIn("a-z", "A-Z", "0-9", "+_\\-").rep).!

  def value[_: P]: P[String] =
    P(CharsWhile(c => c != '\n').!)

  def keyValue[_: P]: P[(String, String)] =
    (key ~ " ".rep ~ ":" ~ " ".rep ~ value ~ newline)

  def keyValues[_: P]: P[MetaHeader] =
    keyValue.rep.map(makeMap)

  def description[_: P] = {
    (P(P(!webactEnd ~ AnyChar).rep.!).map { str => str.trim}) ~ webactEnd
  }

  def metaMap[_:P]: P[MetaHeader] =
    P(webactStart ~ newline ~ keyValues ~ description).map {
      case (m0, desc) => m0.updated(Key.Description, List(desc.trim))
    }

  private def makeMap(p: Seq[(String, String)]): MetaHeader =
    MetaHeader.from(p)
}
