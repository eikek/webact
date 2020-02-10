package webact.app

import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.io.StringWriter
import java.time._
import org.slf4j._
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import cats.data.Validated
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.parser._

import webact.config._
import File._

object OS {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val pathSep = java.io.File.pathSeparator

  def execute(script: Path, args: Seq[Path], cfg: Config): Option[Output] = {
    val meta = readMeta(script).set(Key.Name, script.getFileName.toString)
    if (meta.get(Key.Enabled).exists(_.equalsIgnoreCase("true"))) {
      val tmpDir = Files.createDirectories(cfg.tmpDir.resolve(script.getFileName))
      val out    = tmpDir.resolve("stdout.txt").toAbsolutePath.normalize
      Files.deleteIfExists(out)
      val err = tmpDir.resolve("stderr.txt").toAbsolutePath.normalize
      Files.deleteIfExists(err)
      val runjson     = tmpDir.resolve("run.json").toAbsolutePath.normalize
      val existingRun = readRunjson(runjson)

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
        Some(tmpDir.toFile),
        env.toSeq: _*
      )
      val procLogger = new ProcLogger(out, err)
      val output = Try(proc ! procLogger) match {
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
            out,
            err
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
            out,
            err
          ).updateCounter(existingRun)
      }

      writeRunjson(output, runjson)
      Try(runHooks(meta, output, cfg))
      Some(output)
    } else {
      logger.info(s"Skip execution of $script, because it is not enabled.")
      None
    }
  }

  def findOutput(name: String, cfg: Config): Option[Output] = {
    val meta = cfg.tmpDir / name / "run.json"
    if (meta.exists) readRunjson(meta)
    else None
  }

  private def runHooks(meta: MetaHeader, output: Output, cfg: Config): Unit = {
    def read(f: Path): String =
      new String(Files.readAllBytes(f), File.defaultCharset)

    val name = meta.getHead(Key.Name).getOrElse("<unknown>")
    val recipients =
      (meta.get(Key.NotifyErrorMail).filter(_ => output.failure) ++
        meta.get(Key.NotifyMail)).map(_.trim).filter(_.nonEmpty).map(s => MailSender.Mail(s))
    logger.debug(s"Got $recipients recipients to notify.")

    if (recipients.nonEmpty) {
      val subject = meta.getHeadOr(Key.NotifySubject, s"[${cfg.appName}] Run ${name}") match {
        case s => s + (if (output.failure) ": FAILED" else "")
      }
      val text = {
        if (output.success) read(output.stdout)
        else {
          "--- stdout ---\n\n" +
            read(output.stdout) +
            "\n\n--- stderr ---\n\n" +
            read(output.stderr)
        }
      }
      val msg = MailSender.Message(
        recipients,
        MailSender.Mail(cfg.smtp.sender),
        subject,
        text,
        Some(cfg.appName)
      )
      MailSender.SmtpClient(cfg.smtp).send(msg) match {
        case Validated.Invalid(err) =>
          logger.error(s"Error sending notification mails: $err", err.head)
        case Validated.Valid(_) =>
          logger.info(s"Sent notification mails")
      }
    }
  }

  private def readMeta(script: Path): MetaHeader =
    if (Files.exists(script)) {
      MetaParser.parseMeta(new String(Files.readAllBytes(script)))
    } else {
      MetaHeader(Key.Enabled -> "false")
    }

  private def readRunjson(file: Path): Option[Output] =
    if (Files.exists(file)) {
      parse(new String(Files.readAllBytes(file)))
        .getOrElse(Json.Null)
        .as[Output]
        .map(Some(_))
        .getOrElse({
          logger.warn(s"Cannot read meta.json: $file!")
          None
        })
    } else {
      None
    }

  private def writeRunjson(out: Output, file: Path): Unit = {
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
}
