package webact.app

import fs2.Stream
import cats.effect._
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.nio.file.StandardCopyOption._
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._
import io.circe._
import io.circe.syntax._

object File {
  val defaultCharset = StandardCharsets.UTF_8

  implicit final class FileOps(p: Path) {
    def /(child: String): Path =
      p.resolve(child)

    def name: String = p.getFileName.toString

    def parent: Path = p.getParent

    def mkdirs[F[_]: Sync]: F[Path] =
      Sync[F].delay(Files.createDirectories(p))

    def exists: Boolean =
      Files.exists(p)

    def isDirectory: Boolean =
      Files.isDirectory(p)

    def replaceContent[F[_]: Sync](cnt: Array[Byte]): F[Path] =
      Sync[F].delay {
        Files.write(p, cnt, CREATE, WRITE, TRUNCATE_EXISTING)
      }

    def replaceContent[F[_]: Sync, A](cnt: A)(implicit d: Encoder[A]): F[Path] =
      replaceContent(cnt.asJson.noSpaces.getBytes(defaultCharset))

    def appendContent[F[_]: Sync](cnt: Array[Byte]): F[Path] =
      Sync[F].delay {
        Files.write(p, cnt, CREATE, WRITE, APPEND)
      }

    def appendContent[F[_]: Sync, A](cnt: A)(implicit e: Encoder[A]): F[Path] =
      appendContent(cnt.asJson.noSpaces.getBytes(defaultCharset))

    def sha256[F[_]: Sync]: F[String] =
      Sync[F].delay {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val in = new java.security.DigestInputStream(Files.newInputStream(p), md)
        val buff = new Array[Byte](64 * 1024)
        while (in.read(buff) != -1) {}
        val sb = new StringBuilder
        for (b <- md.digest) sb.append(Integer.toHexString(b & 0xff))
        sb.toString
      }

    def moveTo[F[_]: Sync](target: Path): F[Path] =
      Sync[F].delay(Files.move(p, target, ATOMIC_MOVE, REPLACE_EXISTING))

    def newTempFile[F[_]: Sync](prefix: String, suffix: String): F[Path] =
      Sync[F].delay(Files.createTempFile(p, prefix, suffix))

    def delete[F[_]: Sync]: F[Boolean] =
      Sync[F].delay(Files.deleteIfExists(p))

    def listFiles[F[_]: Sync]: Stream[F, Path] =
      if (exists)
        Stream
          .bracket(Sync[F].delay(Files.list(p)))(s => Sync[F].delay(s.close))
          .flatMap(s => Stream.fromIterator(s.iterator.asScala))
      else
        Stream.empty
  }

}
