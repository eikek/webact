package webact.app

sealed trait Key {
  def name: String
}

object Key {

  abstract private[app] class Base extends Key { self: Product =>
    val name = self.productPrefix
  }

  case object Name            extends Base
  case object Category        extends Base
  case object SuccessCode     extends Base
  case object Size            extends Base
  case object Description     extends Base
  case object LastMod         extends Base
  case object Schedule        extends Base
  case object Enabled         extends Base
  case object NotifyMail      extends Base
  case object NotifyErrorMail extends Base
  case object NotifySubject   extends Base
  case object ContentType     extends Base
  case object BadInputCode    extends Base
  case object SentStdErr      extends Base
  case object Param           extends Base

  val all = Set(
    Name,
    Category,
    SuccessCode,
    Size,
    Description,
    LastMod,
    Schedule,
    Enabled,
    NotifyMail,
    NotifyErrorMail,
    NotifySubject,
    ContentType,
    BadInputCode,
    SentStdErr,
    Param
  )

  def from(s: String): Option[Key] =
    all.find(k => k.name.equalsIgnoreCase(s))
}
