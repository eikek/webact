package webact.app

import java.time._
import minitest._

object TimerCalSpec extends SimpleTestSuite {

  test("timer parse") {
    assertEquals(
      TimerCal.parseTimer("Mon *-*-* 12:00").right.get,
      TimerCal(List(DayOfWeek.MONDAY), Nil, Nil, Nil, List(12), List(0)))
    assertEquals(
      TimerCal.parseTimer("2016-*-* 7:0").right.get,
      TimerCal(Nil, List(2016), Nil, Nil, List(7), List(0)))
    assertEquals(
      TimerCal.parseTimer("2016-*-* 7,8,12:0").right.get,
      TimerCal(Nil, List(2016), Nil, Nil, List(7,8,12), List(0)))
  }

  test("timer render") {
    assertEquals(
      TimerCal.parseTimer("Mon,Wed *-*-* 12:0").right.get.asString,
      "Mon,Wed *-*-* 12:00")
    assertEquals(
      TimerCal.parseTimer("2016-*-* 7:0").right.get.asString,
      "2016-*-* 07:00")
  }

  test("calc next date") {
    val t1 = TimerCal.parseTimer("2016-*-* 08:0,15").right.get
    assertEquals(
      t1.nextTrigger(ldt(2016, 11, 22, 8, 10)),
      Some(ldt(2016, 11, 22, 8, 15))
    )
    assertEquals(
      t1.nextTrigger(ldt(2016, 11, 22, 9, 10)),
      Some(ldt(2016, 11, 23, 8, 0))
    )
    assertEquals(t1.nextTrigger(ldt(2016, 12, 31, 9, 30)), None)

    val t2 = TimerCal.parseTimer("*-10-* 8,10:0,15").right.get
    assertEquals(t2.nextTrigger(ldt(2016,10,31,11,0)), Some(ldt(2017,10,1,8,0)))

    val t3 = TimerCal.parseTimer("Mon,Thu *-*-* 10:0").right.get
    assertEquals(
      t3.nextTrigger(ldt(2016,11,26,21,50)),
      Some(ldt(2016,11,28,10,0)))
    assertEquals(
      t3.nextTrigger(ldt(2016,11,28,10,15)),
      Some(ldt(2016,12,1,10,0)))
  }

  private def ldt(y: Int, m: Int, d: Int, h: Int, min: Int): LocalDateTime =
    LocalDateTime.of(y, m, d, h, min)
}
