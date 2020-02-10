package webact.app

case class MetaHeader(values: Map[Key, List[String]]) {

  def updated(k: Key, v: List[String]): MetaHeader =
    MetaHeader(values.updated(k, v))

  def set(k: Key, v: String): MetaHeader =
    updated(k, List(v))

  def get(k: Key): List[String] =
    values.get(k).getOrElse(Nil)

  def getHead(k: Key): Option[String] =
    get(k).headOption

  def getHeadOr(k: Key, default: => String): String =
    getHead(k).getOrElse(default)

  def ++(other: MetaHeader): MetaHeader =
    MetaHeader(values ++ other.values)
}

object MetaHeader {

  def apply(v: (Key, String)*): MetaHeader =
    MetaHeader(
      v.groupBy(_._1).map({ case (k, vp) => (k -> vp.map(_._2).toList) })
    )

  def from(values: Seq[(String, String)]): MetaHeader =
    MetaHeader(
      values
        .groupBy(_._1)
        .flatMap({
          case (k, v) =>
            Key.from(k).map(key => List((key -> v.map(_._2).toList))).getOrElse(Nil)
        })
        .toMap
    )
}
