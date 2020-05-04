package webact.app

import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.io.StringWriter
import java.time._
import org.slf4j._
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import emil._
import emil.builder._
import emil.javamail.syntax._
import cats.effect._

import webact.config._
import File._

object OS {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val pathSep = java.io.File.pathSeparator

  def execute[F[_]: Sync](
      script: Path,
      args: Seq[Path],
      cfg: Config,
      mailer: Emil[F]
  ): F[Option[Output]] = {
    val meta = readMeta(script).set(Key.Name, script.getFileName.toString)
    if (meta.get(Key.Enabled).exists(_.equalsIgnoreCase("true")))
      for {
        env <- prepare(script, cfg)
        out <- runScript[F](script, args, cfg, meta, env)
        _ <- writeRunjson(out, env.runjson)
        _ <- runHooks(meta, out, cfg, mailer)
      } yield Some(out)
    else {
      logger.info(s"Skip execution of $script, because it is not enabled.")
      (None: Option[Output]).pure[F]
    }
  }

  private def prepare[F[_]: Sync](script: Path, cfg: Config): F[RunEnv] =
    Sync[F].delay {
      val tmpDir = Files.createDirectories(cfg.tmpDir.resolve(script.getFileName))
      val out = tmpDir.resolve("stdout.txt").toAbsolutePath.normalize
      Files.deleteIfExists(out)
      val err = tmpDir.resolve("stderr.txt").toAbsolutePath.normalize
      Files.deleteIfExists(err)
      val runjson = tmpDir.resolve("run.json").toAbsolutePath.normalize
      RunEnv(tmpDir, out, err, runjson)
    }

  private def runScript[F[_]: Sync](
      script: Path,
      args: Seq[Path],
      cfg: Config,
      meta: MetaHeader,
      scriptEnv: RunEnv
  ): F[Output] =
    Sync[F].delay {
      val existingRun = readRunjson(scriptEnv.runjson)
      logger.info(s"Executing $script in WD ${cfg.tmpDir}!")
      val inheritedPath = Option(System.getenv("PATH")).filter(_ => cfg.inheritPath).toSeq
      val env = cfg.env.updated(
        "PATH",
        (inheritedPath ++ cfg.extraPath ++ Seq(cfg.scriptDir.toString)).mkString(pathSep)
      )
      logger.debug(s"Environment: $env")
      val started = Instant.now
      val proc = Process(
        Seq(script.toString) ++ args.map(_.toAbsolutePath.normalize.toString),
        Some(scriptEnv.dir.toFile),
        env.toSeq: _*
      )
      val procLogger = new ProcLogger(scriptEnv.stdout, scriptEnv.stderr)
      Try(proc ! procLogger) match {
        case Success(rc) =>
          val successCodes = meta
            .get(Key.SuccessCode)
            .map(_.trim)
            .filter(_.nonEmpty)
            .flatMap(s => Try(s.toInt).toOption)
            .toSet
          logger.debug(s"Got success codes: $successCodes")
          val rcSuccess = successCodes.contains(rc) || (successCodes.isEmpty && rc == 0)
          val output = Output(
            started,
            Duration.between(started, Instant.now),
            rc,
            rcSuccess,
            1,
            if (rcSuccess) 1 else 0,
            scriptEnv.stdout,
            scriptEnv.stderr
          ).updateCounter(existingRun)
          if (output.success) logger.info(s"Script $script run successful: $rc")
          else logger.error(s"Script $script returned with unsuccessful return code: $rc")
          output
        case Failure(ex) =>
          val sw = new StringWriter()
          ex.printStackTrace(new java.io.PrintWriter(sw))
          procLogger.err(sw.toString)
          Output(
            started,
            Duration.between(started, Instant.now),
            Int.MinValue,
            false,
            1,
            0,
            scriptEnv.stdout,
            scriptEnv.stderr
          ).updateCounter(existingRun)
      }
    }

  def findOutput(name: String, cfg: Config): Option[Output] = {
    val meta = cfg.tmpDir / name / "run.json"
    if (meta.exists) readRunjson(meta)
    else None
  }

  private def runHooks[F[_]: Sync](
      meta: MetaHeader,
      output: Output,
      cfg: Config,
      mailer: Emil[F]
  ): F[Unit] = {
    def read(f: Path): String =
      new String(Files.readAllBytes(f), File.defaultCharset)

    val name = meta.getHead(Key.Name).getOrElse("<unknown>")
    val recipients =
      (meta.get(Key.NotifyErrorMail).filter(_ => output.failure) ++
        meta.get(Key.NotifyMail))
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(s => MailAddress.parseUnsafe(s))
    logger.debug(s"Got $recipients recipients to notify.")

    if (recipients.nonEmpty && cfg.smtp.host.nonEmpty) {
      val subject =
        meta.getHeadOr(Key.NotifySubject, s"[${cfg.appName}] Run ${name}") match {
          case s => s + (if (output.failure) ": FAILED" else "")
        }
      val text = {
        if (output.success) read(output.stdout)
        else
          "--- stdout ---\n\n" +
            read(output.stdout) +
            "\n\n--- stderr ---\n\n" +
            read(output.stderr)
      }
      val msg = MailBuilder.build(
        Tos(recipients),
        From(cfg.smtp.sender),
        Subject(subject),
        TextBody[F](text),
        CustomHeader("List-Id", s"<${cfg.appName}>")
      )
      val mailCfg = cfg.smtp.toMailConfig
      logger.info(s"Notify outcome via mail to: ${recipients} via ${mailCfg}")
      mailer(mailCfg).send(msg).attempt.map {
        case Left(err) =>
          logger.error(s"Error sending notification mails", err)
        case Right(_) =>
          logger.info(s"Sent notification mails")
      }
    } else {
      if (cfg.smtp.host.isEmpty) {
        logger.warn("No SMTP settings in config file. Cannot send mail.")
      }
      ().pure[F]
    }
  }

  private def readMeta(script: Path): MetaHeader =
    if (Files.exists(script))
      MetaParser.parseMeta(new String(Files.readAllBytes(script)))
    else
      MetaHeader(Key.Enabled -> "false")

  private def readRunjson(file: Path): Option[Output] =
    if (Files.exists(file))
      parse(new String(Files.readAllBytes(file)))
        .getOrElse(Json.Null)
        .as[Output]
        .map(Some(_))
        .getOrElse {
          logger.warn(s"Cannot read meta.json: $file!")
          None
        }
    else
      None

  private def writeRunjson[F[_]: Sync](out: Output, file: Path): F[Unit] =
    Sync[F].delay {
      val metaData = out.asJson.noSpaces
      logger.debug(s"Write run.json '$metaData' data to $file")
      Files.write(file, metaData.getBytes, CREATE, WRITE, TRUNCATE_EXISTING)
      ()
    }

  final class ProcLogger(outFile: Path, errFile: Path) extends ProcessLogger {
    def buffer[T](f: => T): T = f
    def err(s: => String): Unit = {
      Files.write(errFile, (s + "\n").getBytes, CREATE, WRITE, APPEND)
      logger.error(s)
    }
    def out(s: => String): Unit = {
      Files.write(outFile, (s + "\n").getBytes, CREATE, WRITE, APPEND)
      ()
      //logger.debug(s)
    }
  }

  case class RunEnv(dir: Path, stdout: Path, stderr: Path, runjson: Path)

}
