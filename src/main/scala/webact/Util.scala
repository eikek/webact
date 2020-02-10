package webact

import cats.effect._
import cats.implicits._
import java.util.concurrent.atomic.AtomicReference

object Util {

  def memo[F[_]: Sync, A](fa: => F[A]): F[A] = {
    val ref = new AtomicReference[A]()
    Sync[F].suspend {
      Option(ref.get) match {
        case Some(a) => a.pure[F]
        case None =>
          fa.map { a =>
            ref.set(a)
            a
          }
      }
    }
  }
}
