import sbt._

object Dependencies {

  val CirceVersion = "0.13.0"
  val EmilVersion = "0.6.0"
  val FastparseVersion = "2.3.0"
  val Fs2Version = "2.3.0"
  val Http4sVersion = "0.21.4"
  val KindProjectorVersion = "0.10.3"
  val LogbackVersion = "1.2.3"
  val YamuscaVersion = "0.6.2"
  val betterMonadicForVersion = "0.3.1"
  val calevVersion = "0.3.1"
  val dnsJavaVersion = "3.0.2"
  val miniTestVersion = "2.8.2"
  val pureConfigVersion = "0.12.3"

  val emil = Seq(
    "com.github.eikek" %% "emil-common" % EmilVersion,
    "com.github.eikek" %% "emil-javamail" % EmilVersion
  )
  val calev = Seq(
    "com.github.eikek" %% "calev-core" % calevVersion,
    "com.github.eikek" %% "calev-fs2" % calevVersion
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Fs2Version,
    "co.fs2" %% "fs2-io" % Fs2Version
  )

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
    "com.lihaoyi" %% "fastparse" % FastparseVersion
  )

  val yamusca = Seq(
    "com.github.eikek" %% "yamusca-core" % YamuscaVersion
  )

  val webjars = Seq(
    "swagger-ui" -> "3.25.3",
    "Semantic-UI" -> "2.4.1",
    "jquery" -> "3.5.1",
    "highlightjs" -> "9.15.10"
  ).map({case (a, v) => "org.webjars" % a % v })

  val testing = Seq(
    // https://github.com/monix/minitest
    // Apache 2.0
    "io.monix" %% "minitest" % miniTestVersion,
    "io.monix" %% "minitest-laws" % miniTestVersion
  ).map(_ % Test)

  val kindProjectorPlugin = "org.typelevel" %% "kind-projector"     % KindProjectorVersion
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion
}
