import com.github.eikek.sbt.openapi._
import scala.sys.process._
import com.typesafe.sbt.SbtGit.GitKeys._

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
  enablePlugins(OpenApiSchema, BuildInfoPlugin).
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
      Dependencies.pureconfig ++
      Dependencies.logging ++
      Dependencies.yamusca ++
      Dependencies.webjars,
    javaOptions in reStart := (javaOptions in run).value  ++ Seq("-Dwebact.script-dir=target/scripts"),
    Compile/resourceGenerators += (Def.task {
      copyWebjarResources(Seq((sourceDirectory in Compile).value/"webjar", (Compile/resourceDirectory).value/"openapi.yml")
        , (Compile/resourceManaged).value
        , name.value
        , version.value
        , streams.value.log
      )
    }).taskValue,
    Compile/resourceGenerators += (Def.task {
      compileElm(streams.value.log
        , (Compile/baseDirectory).value
        , (Compile/resourceManaged).value
        , name.value
        , version.value)
    }).taskValue,
    Compile/unmanagedResourceDirectories ++= Seq((Compile/resourceDirectory).value.getParentFile/"templates"),
    Compile/sourceGenerators += (Def.task {
      createWebjarSource(Dependencies.webjars, (Compile/sourceManaged).value)
    }).taskValue,
    addCompilerPlugin(Dependencies.kindProjectorPlugin),
    addCompilerPlugin(Dependencies.betterMonadicFor),
    openapiTargetLanguage := Language.Scala,
    openapiPackage := Pkg("webact.model"),
    openapiSpec := (Compile/resourceDirectory).value/"openapi.yml",
    openapiScalaConfig := ScalaConfig().withJson(ScalaJson.circeSemiauto),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, gitHeadCommit, gitHeadCommitDate, gitUncommittedChanges, gitDescribedVersion),
    buildInfoPackage := "webact",
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoOptions += BuildInfoOption.BuildTime
  )

def copyWebjarResources(src: Seq[File], base: File, artifact: String, version: String, logger: Logger): Seq[File] = {
  val targetDir = base/"META-INF"/"resources"/"webjars"/artifact/version
  src.flatMap { dir =>
    if (dir.isDirectory) {
      val files = (dir ** "*").filter(_.isFile).get pair Path.relativeTo(dir)
      files.map { case (f, name) =>
        val target = targetDir/name
        logger.info(s"Copy $f -> $target")
        IO.createDirectories(Seq(target.getParentFile))
        IO.copy(Seq(f -> target))
        target
      }
    } else {
      val target = targetDir/dir.name
      logger.info(s"Copy $dir -> $target")
      IO.createDirectories(Seq(target.getParentFile))
      IO.copy(Seq(dir -> target))
      Seq(target)
    }
  }
}

def compileElm(logger: Logger, wd: File, outBase: File, artifact: String, version: String): Seq[File] = {
  logger.info("Compile elm files ...")
  val target = outBase/"META-INF"/"resources"/"webjars"/artifact/version/"webact-app.js"
  val proc = Process(Seq("elm", "make", "--optimize", "--output", target.toString) ++ Seq(wd/"src"/"main"/"elm"/"Main.elm").map(_.toString), Some(wd))
  val out = proc.!!
  logger.info(out)
  Seq(target)
}

def createWebjarSource(wj: Seq[ModuleID], out: File): Seq[File] = {
  val target = out/"Webjars.scala"
  val fields = wj.map(m => s"""val ${m.name.toLowerCase.filter(_ != '-')} = "/${m.name}/${m.revision}" """).mkString("\n\n")
  val content = s"""package webact
    |object Webjars {
    |$fields
    |}
    |""".stripMargin

  IO.write(target, content)
  Seq(target)
}
