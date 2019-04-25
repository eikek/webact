package webact.app

import fs2._
import cats.effect.{ContextShift, Timer, Sync, Concurrent}
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.Traverse
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.collection.JavaConverters._
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.nio.file.attribute.{PosixFilePermission => Perm}
import java.util.concurrent._
import java.time._
import org.slf4j._

import webact.config._
import File._

final class ScriptAppImpl[F[_]: Concurrent](cfg: Config, blockingEc: ExecutionContext, executing: Ref[F, Map[String, Instant]])
  (implicit C: ContextShift[F], T: Timer[F]) extends ScriptApp[F] {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private val filePerms = Set(
    Perm.OWNER_READ, Perm.OWNER_WRITE, Perm.OWNER_EXECUTE,
    Perm.GROUP_READ, Perm.GROUP_EXECUTE,
    Perm.OTHERS_READ
  ).asJava

  private val schedules: ConcurrentMap[String, ScheduleData[F]] =
    new ConcurrentHashMap[String, ScheduleData[F]]()

  def listAll: Stream[F, Script[F]] =
    cfg.scriptDir.listFiles.
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
        Stream.eval(Concurrent[F].start(schedule(name, sc.meta.getHeadOr(Key.Schedule, ""))))).
        compile.drain).
        map(_ => ())
  }

  def findOutput(name: String): F[Option[Output]] = Sync[F].delay {
    OS.findOutput(name, cfg)
  }

  def execute(name: String, args: Seq[Path], deletArgsAfterRun: Boolean): F[F[Option[(Script[F], Output)]]] = {
    def runProcess: F[Option[Output]] =
      Sync[F].bracket(
        executing.update(m => m.updated(name, Instant.now)))(
        _ => C.evalOn(blockingEc)(Sync[F].delay(OS.execute(cfg.scriptDir.resolve(name), args, cfg))))(
        _ => executing.update(m => m - name))

    val exe: F[Option[(Script[F], Output)]] =
      Stream.eval(find(name)).
        unNoneTerminate.
        evalMap(sc => runProcess.map(_.map(o => (sc, o)))).
        unNoneTerminate.
        evalTap({ case (sc, out) => schedule(name, sc.meta.getHeadOr(Key.Schedule, "")).map(_ => ()) }).
        onFinalize(deleteAll(args)).
        compile.last

    Concurrent[F].start(exe).map(_.join)
  }

  def isExecuting(name: String): F[Option[Instant]] =
    executing.get.map(_.get(name))

  def findSchedule(name: String): Option[ScheduleData[F]] =
    Option(schedules.get(name))


  def cancelSchedule(name: String): F[Unit] =
    findSchedule(name).
      map(sch => sch.cancel).
      getOrElse(().pure[F])

  def scheduleRun(name: String, when: FiniteDuration): F[ScheduleData[F]] =
    cancelSchedule(name) >>
    Sync[F].delay(logger.info(s"Scheduling next run of $name in $when (at ${LocalDateTime.now.plus(Duration.ofNanos(when.toNanos))})")) >>
    Concurrent[F].start(T.sleep(when).flatMap(_ => execute(name, Seq.empty, deletArgsAfterRun = true))).
      map(_.cancel).
      map({ fu =>
        val cancel: F[Unit] =
          Sync[F].delay(logger.debug(s"Cancel current schedule for $name")) >>
          fu >>
          Sync[F].delay(schedules.remove(name)).map(_ => ())
        val data = ScheduleData(name, cancel, LocalDateTime.now.plus(Duration.ofMillis(when.toMillis)))
        schedules.put(name, data)
        data
      })

  def schedule(name: String, timer: String): F[Option[ScheduleData[F]]] =
    cancelSchedule(name) >> //cancel so that empty timer strimg deactives scheduled run
    Stream.eval(TimerCal.nextTrigger(timer)).
      unNoneTerminate.
      evalMap(fd => scheduleRun(name, fd)).
      compile.last

  def init: F[Unit] =
    Concurrent[F].start((
      listAll.
        evalMap(sc => schedule(sc.meta.getHeadOr(Key.Name, ""), sc.meta.getHeadOr(Key.Schedule, ""))).
        compile.drain
    )).
      map(_.join).
      map(_ => ())

  private def deleteAll(paths: Seq[Path]): F[Unit] =
    for {
      _  <- Sync[F].delay(logger.debug(s"Deleting files ${paths.mkString(", ")}"))
      _  <- Traverse[List].sequence(paths.toList.map(p => p.delete))
    } yield ()

}

object ScriptAppImpl {

  def create[F[_]: Concurrent](cfg: Config
    , blockingEc: ExecutionContext)
    (implicit C: ContextShift[F], T: Timer[F]): F[ScriptApp[F]] =
    for {
      ref <- Ref.of[F, Map[String, Instant]](Map.empty)
    } yield new ScriptAppImpl(cfg, blockingEc, ref)
}
