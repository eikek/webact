package webact.app

import fs2.{Stream =>Fs2Stream}
import cats.effect.Sync
import java.util.Locale
import java.time._
import java.util.concurrent._
import cats.syntax.either._
import scala.concurrent.duration.FiniteDuration
import org.slf4j._
import fastparse._
import NoWhitespace._


// Thu,Fri 2012-*-1,5 11:12:13
case class TimerCal(dow: List[DayOfWeek],
  year: List[Int], month: List[Int], day: List[Int],
  hour: List[Int], minute: List[Int]) {

  lazy val asString: String = {
    def str(l: List[Int], s: Int = 2) = if (l.isEmpty) "*" else l.map(n => (s"%0${s}d").format(n)).mkString(",")
    val days =
      if (dow.isEmpty) ""
      else dow.map(_.getDisplayName(format.TextStyle.SHORT, Locale.ROOT)).mkString(",") + " "
    days + str(year, 4) +"-"+ str(month) +"-"+ str(day) +" "+ str(hour) +":"+ str(minute)
  }

  def triggeredNow = triggered(LocalDateTime.now)

  def triggered(now: LocalDateTime): Boolean = {
    def check[A](values: List[A], needle: A): Boolean =
      values.isEmpty || values.contains(needle)

    check(dow, now.getDayOfWeek) && check(year.map(_.toInt), now.getYear) &&
    check(month.map(m => Month.of(m.toInt)), now.getMonth) &&
    check(day.map(_.toInt), now.getDayOfMonth) &&
    check(hour.map(_.toInt), now.getHour) &&
    check(minute.map(_.toInt), now.getMinute)
  }

  def nextTimers(startYear: Int): Stream[LocalDateTime] = {
    val comb = for {
      y <- if (year.isEmpty) Stream.from(startYear) else year.toStream
      m <- if (month.isEmpty) (1 to 12) else month
      d <- if (day.isEmpty) (1 to 31) else day
      h <- if (hour.isEmpty) (0 to 59) else hour
      min <- if (minute.isEmpty) (0 to 59) else minute
    } yield (y, m, d, h, min)
    //filter out invalid dates
    val dates = comb.flatMap({
      case (y,m,d,h,min) => TimerCal.localDateTime(y,m,d,h,min).toStream
    })
    if (dow.isEmpty) dates
    else dates.filter(ld => dow.contains(ld.getDayOfWeek))
  }

  def nextTrigger(ref: LocalDateTime): Option[LocalDateTime] =
    nextTimers(ref.getYear).find(_ isAfter ref)
}

object TimerCal {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  private object Parser {
    def empty[_: P]: P[Seq[Int]] = P("*").map(_ => Nil)
    def dow[_: P]: P[DayOfWeek] =
      P("Mon").map(_ => DayOfWeek.MONDAY) |
        P("Tue").map(_ => DayOfWeek.TUESDAY) |
        P("Wed").map(_ => DayOfWeek.WEDNESDAY) |
        P("Thu").map(_ => DayOfWeek.THURSDAY) |
        P("Fri").map(_ => DayOfWeek.FRIDAY) |
        P("Sat").map(_ => DayOfWeek.SATURDAY) |
        P("Sun").map(_ => DayOfWeek.SUNDAY)

    def dows[_: P]: P[Seq[DayOfWeek]] = dow.rep(1, sep=",")
    def yearSingle[_: P]: P[Int] = CharIn("0-9").rep(4).!.map(_.toInt)
    def years[_: P]: P[Seq[Int]] = P(empty | yearSingle.rep(1, sep=","))
    def two[_: P]: P[Int] = CharIn("0-9").rep(min=1, max=2).!.map(_.toInt)
    def twos[_: P]: P[Seq[Int]] = P(empty | two.rep(1, sep=","))

    def offset[_: P]: P[Duration] = P("+" ~ P(CharIn("0-9").rep.!).map(_.toInt) ~ P("min"|"h"|"d"|"m").!).map {
      case (num, unit) => unit.toLowerCase match {
        case "h" => Duration.ofHours(num.toLong)
        case "d" => Duration.ofDays(num.toLong)
        case "min" => Duration.ofMinutes(num.toLong)
        case _ => Duration.ofMinutes(num.toLong)
      }
    }
    def offsetTimer[_: P]: P[TimerCal] = offset.map { duration =>
      val now = LocalDateTime.now.plus(duration)
      TimerCal(Nil, List(now.getYear),
        List(now.getMonthValue),
        List(now.getDayOfMonth),
        List(now.getHour),
        List(now.getMinute))
    }

    def stdTimer[_: P]: P[TimerCal] = P((dows ~" ").? ~ years ~ "-" ~ twos ~"-"~ twos ~" "~ twos ~":"~ twos).map {
      case (w, y, m, d, h, min) => TimerCal(
        w.map(_.toList.sorted).getOrElse(Nil),
        y.toList.sorted, m.toList.sorted, d.toList.sorted,
        h.toList.sorted, min.toList.sorted)
    }
    def timer[_: P]: P[TimerCal] = offsetTimer | stdTimer
  }

  def parseTimer(s: String): Either[String, TimerCal] = parse(s, Parser.timer(_)) match {
    case Parsed.Success(c, _) => Right(c)
    case f@Parsed.Failure(_, _, _) =>
      val trace = f.trace()
      Left(s"Cannot parse timer string '$s' at index ${trace.index}: ${trace.msg}")
  }

  def nextTrigger[F[_]: Sync](timer: String): F[Option[FiniteDuration]] =
    Fs2Stream.emit(timer).
      filter(_.trim.nonEmpty).
      map(parseTimer).
      evalTap(e => Sync[F].delay{
        e.left.foreach(err => logger.warn(s"Cannot parse schedule string: $err"))
      }).
      flatMap(_.fold(_ => Fs2Stream.empty, e => Fs2Stream.emit(e))).
      map(t => t.nextTrigger(LocalDateTime.now)).unNoneTerminate.
      map(fd => Duration.between(LocalDateTime.now, fd)).
      map(fd => FiniteDuration(fd.toNanos, TimeUnit.NANOSECONDS)).
      compile.last

  def localDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Option[LocalDateTime] =
    Either.catchOnly[DateTimeException](LocalDateTime.of(year, month, day, hour, minute)).toOption

  lazy val always = TimerCal.parseTimer("*-*-* *:*")
}
