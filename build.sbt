import com.github.eikek.sbt.openapi._

val sharedSettings = Seq(
  organization := "com.github.eikek",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-feature",
    "-Ypartial-unification",
    "-Xfatal-warnings", // fail when there are warnings
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) := Seq()
)

val testSettings = Seq(
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  libraryDependencies ++= Dependencies.testing
)

lazy val root = (project in file(".")).
  enablePlugins(OpenApiSchema).
  settings(sharedSettings).
  settings(testSettings).
  settings(
    name := "webact",
    description := "Execute and manage scripts from the web",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= Dependencies.http4s ++
      Dependencies.circe ++
      Dependencies.fastparse ++
      Dependencies.javaxMail ++
      Dependencies.logging ++
      Dependencies.pureconfig,
    javaOptions in reStart := (javaOptions in run).value  ++ Seq("-Dwebact.script-dir=target/scripts"),
    addCompilerPlugin(Dependencies.kindProjectorPlugin),
    addCompilerPlugin(Dependencies.betterMonadicFor),
    openapiTargetLanguage := Language.Scala,
    openapiPackage := Pkg("webact.model"),
    openapiSpec := (Compile/resourceDirectory).value/"openapi.yml",
    openapiScalaConfig := ScalaConfig().withJson(ScalaJson.circeSemiauto)
  )
