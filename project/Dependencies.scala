import sbt._

object Dependencies {

  val Http4sVersion = "0.20.0-RC1"
  val CirceVersion = "0.11.1"
  val LogbackVersion = "1.2.3"
  val pureConfigVersion = "0.10.2"
  val miniTestVersion = "2.3.2"
  val kindProjectorVersion = "0.9.10"
  val betterMonadicForVersion = "0.3.0"
  val javaxMailVersion = "1.6.2"
  val dnsJavaVersion = "2.1.8"

  val http4s = Seq(
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
  )

  val circe = Seq(
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion
  )

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % LogbackVersion
  )

  // https://github.com/melrief/pureconfig
  // MPL 2.0
  val pureconfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  )

  val fastparse = Seq(
    "com.lihaoyi" %% "fastparse" % "2.1.0"
  )

  val javaxMail = Seq(
    "javax.mail" % "javax.mail-api" % javaxMailVersion,
    "com.sun.mail" % "javax.mail" % javaxMailVersion,
    "dnsjava" % "dnsjava" % dnsJavaVersion intransitive()
  )

  val testing = Seq(
    // https://github.com/monix/minitest
    // Apache 2.0
    "io.monix" %% "minitest" % miniTestVersion,
    "io.monix" %% "minitest-laws" % miniTestVersion
  ).map(_ % Test)

  val kindProjectorPlugin = "org.spire-math" %% "kind-projector" % kindProjectorVersion
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion
}
