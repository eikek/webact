package webact.app

import fs2._
import cats.effect.{ContextShift, Timer, Sync, Concurrent}
import cats.implicits._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.collection.JavaConverters._
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.nio.file.attribute.{PosixFilePermission => Perm}
import java.util.stream.Collectors
import java.util.concurrent._
import java.time._
import org.slf4j._

import webact.config._

final class ScriptAppImpl[F[_]: Concurrent](cfg: Config, blockingEc: ExecutionContext)
  (implicit C: ContextShift[F], T: Timer[F]) extends ScriptApp[F] {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private val filePerms = Set(
    Perm.OWNER_READ, Perm.OWNER_WRITE, Perm.OWNER_EXECUTE,
    Perm.GROUP_READ, Perm.GROUP_EXECUTE,
    Perm.OTHERS_READ
  ).asJava

  private val schedules: ConcurrentMap[String, F[Unit]] =
    new ConcurrentHashMap[String, F[Unit]]()

  def listAll: Stream[F, Script[F]] =
    Stream.bracket(Sync[F].delay(Files.list(cfg.scriptDir)))(s => Sync[F].delay(s.close)).
      flatMap(s => Stream.emits(s.collect(Collectors.toList()).asScala)).
      filter(f => !Files.isDirectory(f)).
      evalMap(f => Script.fromFile(f, blockingEc))

  def find(name: String): F[Option[Script[F]]] = {
    val none: Option[Script[F]] = None
    Option(cfg.scriptDir.resolve(name)).
      filter(f => Files.exists(f)).
      map(f => Script.fromFile(f, blockingEc)).
      map(sf => {
        logger.debug(s"Found script ${name} in ${cfg.scriptDir}")
        sf.map(Option(_))
      }).
      getOrElse({
        logger.debug(s"Not found script: ${name} in ${cfg.scriptDir}")
        none.pure[F]
      })
  }

  def store(name: String, bytes: Stream[F, Byte]): F[Unit] = {
    val file = cfg.scriptDir.resolve(name)
    Sync[F].delay(logger.info(s"Storing new version of $name")) >>
    Script.fromBytes(bytes).
      flatMap(sc => (Stream.eval(Sync[F].delay(Files.createDirectories(file.getParent))) ++ sc.content.
        through(fs2.io.file.writeAll(file, blockingEc, List(CREATE, WRITE, TRUNCATE_EXISTING))) ++
        Stream.eval(Sync[F].delay(Files.setPosixFilePermissions(file, filePerms))) ++
        Stream.eval(Concurrent[F].start(schedule(name, sc.get(Key.Schedule))))).
        compile.drain).
        map(_ => ())
  }

  def findOutput(name: String): F[Option[Output]] = Sync[F].delay {
    OS.findOutput(name, cfg)
  }

  def execute(name: String, args: Seq[Path]): F[F[Option[Output]]] = {
    val exe: F[Option[Output]] =
      Stream.eval(find(name)).
        unNoneTerminate.
        evalMap(sc => C.evalOn(blockingEc)(Sync[F].delay(OS.execute(cfg.scriptDir.resolve(name), args, cfg)).map(o => (o, sc)))).
        evalMap({ case (out, sc) => schedule(name, sc.get(Key.Schedule)).map(_ => out) }).
        compile.last.map(_.flatten)

    Concurrent[F].start(exe).map(_.join)
  }

  def cancelSchedule(name: String): F[Unit] =
    Option(schedules.get(name)).
      map(fu => Sync[F].delay(logger.debug(s"Cancel current schedule for $name")) >> fu).
      getOrElse(().pure[F])

  def scheduleRun(name: String, when: FiniteDuration): F[F[Unit]] =
    cancelSchedule(name) >>
    Sync[F].delay(logger.info(s"Scheduling next run of $name in $when (at ${LocalDateTime.now.plus(Duration.ofNanos(when.toNanos))})")) >>
    Concurrent[F].start(T.sleep(when).flatMap(_ => execute(name, Seq.empty))).
      map(_.cancel).
      map({ fu =>
        schedules.put(name, fu)
        fu
      })

  def schedule(name: String, timer: String): F[F[Unit]] =
    cancelSchedule(name) >>
    Stream.eval(TimerCal.nextTrigger(timer)).
      unNoneTerminate.
      evalMap(fd => scheduleRun(name, fd)).
      compile.last.
      map(opt => opt.getOrElse(().pure[F]))

  def init: F[Unit] =
    Concurrent[F].start((
      listAll.
        evalMap(sc => schedule(sc.get(Key.Name), sc.get(Key.Schedule))).
        compile.drain
    )).
      map(_.join).
      map(_ => ())
}

object ScriptAppImpl {

  def create[F[_]: Concurrent](cfg: Config, blockingEc: ExecutionContext)(implicit C: ContextShift[F], T: Timer[F]): ScriptApp[F] =
    new ScriptAppImpl(cfg, blockingEc)
}
