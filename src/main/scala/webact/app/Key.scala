package webact.app

sealed trait Key {
  def name: String
}

object Key {

  private[app] abstract class Base extends Key { self: Product =>
    val name = self.productPrefix
  }

  case object Name extends Base
  case object Category extends Base
  case object SuccessCodes extends Base
  case object Size extends Base
  case object Description extends Base
  case object LastMod extends Base
  case object Schedule extends Base
  case object Enabled extends Base
  case object NotifyMail extends Base
  case object NotifyErrorMail extends Base

  val all = Set(Name
    , Category
    , SuccessCodes
    , Size
    , Description
    , LastMod
    , Schedule
    , Enabled
    , NotifyMail
    , NotifyErrorMail
  )

  def from(s: String): Option[Key] =
    all.find(k => k.name.equalsIgnoreCase(s))
}
