import com.github.eikek.sbt.openapi._
import scala.sys.process._
import com.typesafe.sbt.SbtGit.GitKeys._

val skipWebappBuild = settingKey[Boolean]("Whether to skip building the webjar")
val elmCompileMode = settingKey[ElmCompileMode]("How to compile elm sources")
val makeWebjar = taskKey[Seq[File]]("build the webjar")

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := "2.13.2",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-feature",
    "-Xfatal-warnings", // fail when there are warnings
    "-unchecked",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ),
  skipWebappBuild := false,
  Compile / console / scalacOptions := Seq()
)

val testSettings = Seq(
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  libraryDependencies ++= Dependencies.testing
)

val elmSettings = Seq(
  elmCompileMode := ElmCompileMode.Debug,
  Compile/resourceGenerators += (Def.task {
    val logger = streams.value.log
    val skip = skipWebappBuild.value
    if (skip) Seq.empty
    else compileElm(logger
      , (Compile/baseDirectory).value
      , (Compile/resourceManaged).value
      , name.value
      , version.value
      , elmCompileMode.value)
  }).taskValue,
  watchSources += Watched.WatchSource(
    (Compile/sourceDirectory).value/"elm"
      , FileFilter.globFilter("*.elm")
      , HiddenFileFilter
  )
)

val webjarSettings = Seq(
  Compile/resourceGenerators += (Def.task {
    val logger = streams.value.log
    val skip = skipWebappBuild.value
    if (skip) Seq.empty
    else copyWebjarResources(
      Seq((Compile / sourceDirectory).value/"webjar", (Compile/resourceDirectory).value/"openapi.yml")
      , (Compile/resourceManaged).value
      , name.value
      , version.value
      , logger
    )
  }).taskValue,
  Compile/sourceGenerators += (Def.task {
    createWebjarSource(Dependencies.webjars, (Compile/sourceManaged).value)
  }).taskValue,
  Compile/unmanagedResourceDirectories ++= Seq((Compile/resourceDirectory).value.getParentFile/"templates"),
  watchSources += Watched.WatchSource(
    (Compile / sourceDirectory).value/"webjar"
      , FileFilter.globFilter("*.js") || FileFilter.globFilter("*.css")
      , HiddenFileFilter
  ),
  makeWebjar := {
    val logger = streams.value.log
    val rs = copyWebjarResources(
      Seq((Compile / sourceDirectory).value/"webjar", (Compile/resourceDirectory).value/"openapi.yml")
      , (Compile/resourceManaged).value
      , name.value
      , version.value
      , logger
    )
    val elm = compileElm(logger
      , (Compile/baseDirectory).value
      , (Compile/resourceManaged).value
      , name.value
      , version.value
      , elmCompileMode.value
    )
    rs ++ elm
  }
)

val debianSettings = Seq(
  maintainer := "Eike Kettner <eike.kettner@posteo.de>",
  packageSummary := description.value,
  packageDescription := description.value,
  Universal / mappings += {
    val conf = (Compile / resourceDirectory).value / "reference.conf"
    if (!conf.exists) {
      sys.error(s"File $conf not found")
    }
    conf -> "conf/webact.conf"
  },
  bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/webact.conf""""
)

lazy val root = (project in file(".")).
  enablePlugins(OpenApiSchema
    , BuildInfoPlugin
    , JavaServerAppPackaging
    , DebianPlugin
    , SystemdPlugin).
  settings(sharedSettings).
  settings(testSettings).
  settings(elmSettings).
  settings(webjarSettings).
  settings(debianSettings).
  settings(
    name := "webact",
    description := "Execute and manage scripts from the web",
    libraryDependencies ++= Dependencies.http4s ++
      Dependencies.fs2 ++
      Dependencies.circe ++
      Dependencies.fastparse ++
      Dependencies.pureconfig ++
      Dependencies.logging ++
      Dependencies.yamusca ++
      Dependencies.calev ++
      Dependencies.emil ++
      Dependencies.webjars,
    reStart / javaOptions := (run / javaOptions).value  ++ Seq("-Dwebact.script-dir=target/scripts", s"-Dconfig.file=${baseDirectory.value/"dev.conf"}"),
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

def compileElm(logger: Logger, wd: File, outBase: File, artifact: String, version: String, mode: ElmCompileMode): Seq[File] = {
  logger.info("Compile elm files ...")
  val target = outBase/"META-INF"/"resources"/"webjars"/artifact/version/"webact-app.js"
  if (!target.exists) {
    val cmd = Seq("elm", "make") ++ mode.flags ++ Seq("--output", target.toString)
    val proc = Process(cmd ++ Seq(wd/"src"/"main"/"elm"/"Main.elm").map(_.toString), Some(wd))
    val out = proc.!!
    logger.info(out)
  } else {
    logger.info(s"Target $target already exists")
  }
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

addCommandAlias("make-without-webapp",
  "; set skipWebappBuild := true ;root/openapiCodegen ;root/compile"
)
addCommandAlias("make-webapp-only",
  """;set skipWebappBuild := false
     ;set root/elmCompileMode := ElmCompileMode.Production
  ;root/openapiCodegen
  ;makeWebjar
  ;set Compile / sourceGenerators := Seq.empty
  ;set Compile / sources := Seq.empty
  ;package
  """
)

addCommandAlias("make", ";set root/elmCompileMode := ElmCompileMode.Production ;root/openapiCodegen ;root/Test/compile")
addCommandAlias("make-zip", ";root/Universal/packageBin")
addCommandAlias("make-deb", ";root/Debian/packageBin")
addCommandAlias("make-pkg", ";clean ;make ;make-zip ;make-deb")
