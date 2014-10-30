package io.garuda.codec.pdu

import java.util.{Calendar, SimpleTimeZone, TimeZone}

import scala.util.matching.Regex

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 02.10.13
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */
case class SmppTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, tenthSecond: Int,
                    timezone: Option[TimeZone], isRelative: Boolean) {
  require(year >= 0 && year <= 99, "Year needs to be between 0 and 99: " + year)
  require(month >= 1 && month <= 12, "Month needs to be between 1 and 12: " + month)
  require(day >= 1 && day <= 31, "Day need to be between 1 and 31: " + day)
  require(hour >= 0 && hour <= 23, "Hour needs to be between 0 and 23: " + hour)
  require(minute >= 0 && minute <= 59, "Minute needs to be between 0 and 59: " + minute)
  require(second >= 0 && second <= 59, "Second needs to be between 0 and 59: " + second)
  require(tenthSecond >= 0 && tenthSecond <= 9, "TenthSecond needs to be between 0 and 9: " + tenthSecond)
  require(isRelative || timezone.isDefined, "Absolute times must specify a time zone")
  require(!isRelative || timezone.isEmpty, "Relative times must not specify a time zone: " + timezone.get)
  require(!isRelative || tenthSecond == 0, "Relative times must not specify tenths of second: " + tenthSecond)

  val DefaultYearModifier: Int = 2000

  val AbsoluteTimeFormat: String = "%02d%02d%02d%02d%02d%02d%d%02d%c"

  val RelativeTimeFormat: String = "%02d%02d%02d%02d%02d%02d000R"

  def time(): Option[Calendar] = {
    if (isRelative) {
      None
    } else {
      val time: Calendar = Calendar.getInstance()
      time.clear()
      time.setTimeZone(timezone.get)
      time.set(Calendar.YEAR, DefaultYearModifier + year)
      time.set(Calendar.MONTH, month - 1)
      time.set(Calendar.DAY_OF_MONTH, day)
      time.set(Calendar.HOUR, hour)
      time.set(Calendar.MINUTE, minute)
      time.set(Calendar.SECOND, second)
      time.set(Calendar.MILLISECOND, tenthSecond * 100)
      Some(time)
    }
  }

  def timeRelativeTo(referenceTime: Calendar): Option[Calendar] = {
    if (!isRelative) {
      None
    } else {
      val relativeTime: Calendar = referenceTime.clone().asInstanceOf[Calendar]
      relativeTime.add(Calendar.YEAR, DefaultYearModifier + year)
      relativeTime.add(Calendar.MONTH, month - 1)
      relativeTime.add(Calendar.DAY_OF_MONTH, day)
      relativeTime.add(Calendar.HOUR, hour)
      relativeTime.add(Calendar.MINUTE, minute)
      relativeTime.add(Calendar.SECOND, second)
      Some(relativeTime)
    }
  }

  override def toString: String = {
    if (isRelative) toRelativeString else toAbsoluteString
  }

  private[this] def toAbsoluteString: String = {
    val rawUtcOffsetMillis: Int = timezone.get.getRawOffset
    val rawUtcOffsetQuarters: Int = rawUtcOffsetMillis / (15 * 60 * 1000)
    AbsoluteTimeFormat.format(year, month, day, hour, minute, second, tenthSecond,
      scala.math.abs(rawUtcOffsetQuarters),
      if (rawUtcOffsetMillis < 0) '-' else '+')
  }

  private[this] def toRelativeString: String = {
    RelativeTimeFormat.format(year, month, day, hour, minute, second)
  }
}

object SmppTime {

  def apply(timeString: String): Option[SmppTime] = timeString match {
    case "" => None
    case _ => Some(SmppTimeParser.parse(timeString))
  }

  def apply(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, tenthSecond: Int,
            utcOffset: Int): SmppTime = {
    SmppTime(year, month, day, hour, minute, second, tenthSecond, Some(makeTimeZone(utcOffset)), isRelative = false)
  }

  private[this] def makeTimeZone(utcOffset: Int): TimeZone = {
    val absoluteUtcOffset: Int = scala.math.abs(utcOffset)
    val hoursOffset: Int = absoluteUtcOffset / 4
    val minutesOffset: Int = (absoluteUtcOffset - (4 * hoursOffset)) * 15
    val id: String = "UTC%c%02d:%02d".format(if (utcOffset >= 0) '+' else '-', hoursOffset, minutesOffset)
    new SimpleTimeZone(utcOffset * 900000, id)
  }

  def apply(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): SmppTime = {
    SmppTime(year, month, day, hour, minute, second, 0, None, isRelative = true)
  }
}

private[this] object SmppTimeParser {

  private[this] val AbsTimePattern: Regex =
    "^(\\d{2})([0-1][0-9])([0-3][0-9])([0-2][0-9])([0-5][0-9])([0-5][0-9])([0-9])(\\d{2})(\\+|-)$".r

  private[this] val RelTimePattern: Regex =
    "^(\\d{2})([0-1][0-9])([0-3][0-9])([0-2][0-9])([0-5][0-9])([0-5][0-9])000R$".r

  def parse(timeString: String): SmppTime = timeString.trim match {
    case AbsTimePattern(yy, mm, dd, hh, mi, ss, t, nn, p) => makeAbsoluteTime(yy, mm, dd, hh, mi, ss, t, nn, p)
    case RelTimePattern(yy, mm, dd, hh, mi, ss) => makeRelativeTime(yy, mm, dd, hh, mi, ss)
    case _ => throw new IllegalArgumentException("Not a valid SMPP 5.0 time format: " + "\"" + timeString + "\" " +
      "(trimmed: \"" + timeString.trim + "\")")
  }

  private[this] def makeAbsoluteTime(yy: String, mm: String, dd: String, hh: String, mi: String, ss: String,
                                     t: String, nn: String, p: String): SmppTime = {
    val utcOffset: Int = if (p == "-") -nn.toInt else nn.toInt
    SmppTime(yy.toInt, mm.toInt, dd.toInt, hh.toInt, mi.toInt, ss.toInt, t.toInt, utcOffset)
  }

  private[this] def makeRelativeTime(yy: String, mm: String, dd: String, hh: String, mi: String,
                                     ss: String): SmppTime = {
    SmppTime(yy.toInt, mm.toInt, dd.toInt, hh.toInt, mi.toInt, ss.toInt, 0, None, isRelative = true)
  }
}
