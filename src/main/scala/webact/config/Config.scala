package webact.config

import java.nio.file.{Path, Paths}
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.ConvertHelpers._

case class Config(appName: String
  , scriptDir: Path
  , tmpDir: Path
  , inheritPath: Boolean
  , extraPath: Seq[String]
  , env: Map[String, String]
  , bind: Config.Bind
  , smtp: Config.Smtp)

object Config {
  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, KebabCase))

  implicit val pathConvert: ConfigReader[Path] = ConfigReader.fromString[Path](catchReadError(s =>
    if (s.isEmpty) throw new Exception("Empty path is not allowed: "+ s)
    else Paths.get(s).toAbsolutePath.normalize
  ))

  case class Smtp(host: String, port: Int, user: String, password: String, sender: String)

  case class Bind(host: String, port: Int)

  lazy val default: Config = {
    loadConfigOrThrow[Config]("webact")
  }
}
