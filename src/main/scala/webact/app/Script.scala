package webact.app

import fs2._
import cats.Applicative
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext

case class Script[F[_]](content: Stream[F, Byte], meta: Map[Key, String]) {

  def get(key: Key): String = meta.get(key).getOrElse("")

  def update(key: Key, value: String) =
    Script(content, meta.updated(key, value))

  def update(m: Map[Key, String]): Script[F] =
    Script(content, meta ++ m)
}

object Script {

  def fromFile[F[_]: Sync](file: Path, blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Script[F]] =
    fromBytes(fs2.io.file.readAll(file, blockingEc, 64 * 1024)).
      map(_.update(Map(
        Key.Name -> file.getFileName.toString,
        Key.Size -> Files.size(file).toString,
        Key.LastMod -> Files.getLastModifiedTime(file).toInstant.toEpochMilli.toString
      )))

  def fromBytes[F[_]: Sync](bytes: Stream[F, Byte]): F[Script[F]] = {
    val content = bytes
    val meta = bytes.take(64 * 1024).
      through(fs2.text.utf8Decode).
      compile.lastOrError.
      map(MetaParser.parseMeta)
    meta.map(m => Script(content, m))
  }

  def fromString[F[_]: Applicative](content: String): F[Script[F]] =
    Script(Stream.emits(content.getBytes).covary[F], MetaParser.parseMeta(content)).pure[F]
}
