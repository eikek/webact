package webact.app

import fs2._
import cats.Applicative
import cats.effect._
import cats.implicits._
import java.nio.file.{Files, Path}

case class Script[F[_]](content: Stream[F, Byte], meta: MetaHeader) {

  def update(key: Key, value: String) =
    Script(content, meta.set(key, value))

  def update(m: MetaHeader): Script[F] =
    Script(content, meta ++ m)

  def asUtf8: Stream[F, String] =
    content.through(fs2.text.utf8Decode)
}

object Script {

  def fromFile[F[_]: Sync](file: Path, blocker: Blocker)(
      implicit C: ContextShift[F]
  ): F[Script[F]] =
    fromBytes(fs2.io.file.readAll(file, blocker, 64 * 1024)).map(
      _.update(
        MetaHeader(
          Key.Name -> file.getFileName.toString,
          Key.Size -> Files.size(file).toString,
          Key.LastMod -> Files.getLastModifiedTime(file).toInstant.toEpochMilli.toString
        )
      )
    )

  def fromBytes[F[_]: Sync](bytes: Stream[F, Byte]): F[Script[F]] = {
    val content = bytes
    val meta = bytes
      .take(64 * 1024)
      .through(fs2.text.utf8Decode)
      .compile
      .lastOrError
      .map(MetaParser.parseMeta)
    meta.map(m => Script(content, m))
  }

  def fromString[F[_]: Applicative](content: String): F[Script[F]] =
    Script(Stream.emits(content.getBytes).covary[F], MetaParser.parseMeta(content))
      .pure[F]
}
