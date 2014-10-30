package io.garuda.codec.pdu

import java.util.{Calendar, SimpleTimeZone}

import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 03.10.13
 * Time: 13:58
 * To change this template use File | Settings | File Templates.
 */
class SmppTimeSpec extends Specification {

  "SmppTime#apply() (object) as a constructor for absolute SmppTime" should {

    "create absolute SmppTime with correct time zone given positive UTC offset" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, 12)
      val expectedTimeZone = new SimpleTimeZone(3 * 60 * 60 * 1000, "UTC+03:00")

      absoluteSmppTime.timezone.get mustEqual expectedTimeZone
    }

    "create absolute SmppTime with correct time zone given negative UTC offset" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, -12)
      val expectedTimeZone = new SimpleTimeZone(-3 * 60 * 60 * 1000, "UTC-03:00")

      absoluteSmppTime.timezone.get mustEqual expectedTimeZone
    }
  }

  "SmppTime#time() (class), if absolute" should {

    "correctly represent itself as a Calendar instance" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, 12)
      val expectedTimeZone = new SimpleTimeZone(3 * 60 * 60 * 1000, "UTC+03:00")
      val expectedCalenarRepresentation = Calendar.getInstance(expectedTimeZone)
      expectedCalenarRepresentation.clear()
      expectedCalenarRepresentation.set(2009, 11, 7, 23, 9, 7)
      expectedCalenarRepresentation.set(Calendar.MILLISECOND, 100 * 6)

      val actualCalendarRepresentation = absoluteSmppTime.time().get

      actualCalendarRepresentation mustEqual expectedCalenarRepresentation
    }
  }

  "SmppTime#time() (class), if relative" should {

    "return None" in {
      val relativeSmppTime = SmppTime(9, 12, 7, 23, 9, 7)

      relativeSmppTime.time mustEqual None
    }
  }

  "SmppTime#toString() (class), if absolute" should {

    "correctly represent itself as an SMPP time string with positive whole-hours UTC offset" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, 12)
      val expectedStringRep = "091207230907612+"

      absoluteSmppTime.toString() mustEqual expectedStringRep
    }

    "correctly represent itself as an SMPP time string with positive fractional-hours UTC offset" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, 7)
      val expectedStringRep = "091207230907607+"

      absoluteSmppTime.toString() mustEqual expectedStringRep
    }

    "correctly represent itself as an SMPP time string with negative whole-hours UTC offset" in {
      val absoluteSmppTime = SmppTime(9, 12, 7, 23, 9, 7, 6, -12)
      val expectedStringRep = "091207230907612-"

      absoluteSmppTime.toString() mustEqual expectedStringRep
    }

    "correctly represent itself as an SMPP time string with negative fractional-hours UTC offset" in {
      val absoluteSmppTime = SmppTime(54, 1, 12, 1, 59, 59, 0, -7)
      val expectedStringRep = "540112015959007-"

      absoluteSmppTime.toString() mustEqual expectedStringRep
    }
  }

  "SmppTime (object) as a parser for absolute SmppTime given a string" should {

    "correctly parse a valid absolute SMPP time string with positive UTC offset" in {
      val validAbsoluteSmppTimeString = "091207230907612+"
      val expectedResult = SmppTime(9, 12, 7, 23, 9, 7, 6, 12)

      val actualResult = SmppTime(validAbsoluteSmppTimeString)

      actualResult.get mustEqual expectedResult
    }

    "correctly parse a valid absolute SMPP time string with negative UTC offset" in {
      val validAbsoluteSmppTimeString = "010106020906003-"
      val expectedResult = SmppTime(1, 1, 6, 2, 9, 6, 0, -3)

      val actualResult = SmppTime(validAbsoluteSmppTimeString)

      actualResult.get mustEqual expectedResult
    }

    "correctly parse a valid absolute SMPP time string with leading and trailing whitespace" in {
      val validAbsoluteSmppTimeString = " \t520729125959920+ \t"
      val expectedResult = SmppTime(52, 7, 29, 12, 59, 59, 9, 20)

      val actualResult = SmppTime(validAbsoluteSmppTimeString)

      actualResult.get mustEqual expectedResult
    }

    "recognize a malformed absolute SMPP time string with illegal offset modifier" in {
      val malformedAbsoluteSmppTimeString = "241102112345603#"

      SmppTime(malformedAbsoluteSmppTimeString) must throwA[IllegalArgumentException]
    }

    "recognize a wellformed absolute SMPP time string with illegal month" in {
      val malformedAbsoluteSmppTimeString = "240002112345603+"

      SmppTime(malformedAbsoluteSmppTimeString) must throwA[IllegalArgumentException]
    }
  }

  "SmppTime (object) as a parser for relative SmppTime given a string" should {

    "correctly parse a valid relative SMPP time string" in {
      val validRelativeSmppTimeString = "091207230907000R"
      val expectedResult = SmppTime(9, 12, 7, 23, 9, 7)

      val actualResult = SmppTime(validRelativeSmppTimeString)

      actualResult.get mustEqual expectedResult
    }

    "correctly parse a valid relative SMPP time string with leading and trailing whitespace" in {
      val validRelativeSmppTimeString = " \t520729125959000R \t"
      val expectedResult = SmppTime(52, 7, 29, 12, 59, 59)

      val actualResult = SmppTime(validRelativeSmppTimeString)

      actualResult.get mustEqual expectedResult
    }

    "recognize a malformed relative SMPP time string with illegal relative modifier" in {
      val malformedRelativeSmppTimeString = "241102112345000r"

      SmppTime(malformedRelativeSmppTimeString) must throwA[IllegalArgumentException]
    }

    "recognize a wellformed relative SMPP time string with illegal month" in {
      val malformedRelativeSmppTimeString = "240002112345000R"

      SmppTime(malformedRelativeSmppTimeString) must throwA[IllegalArgumentException]
    }
  }
}
