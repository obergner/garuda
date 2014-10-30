package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 16.08.13
 * Time: 21:18
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class Ton(ton: Byte) {

  def asByte = ton
}

object Ton extends {

  case object Unknown extends Ton(0)

  case object International extends Ton(1)

  case object National extends Ton(2)

  case object NetworkSpecific extends Ton(3)

  case object SubscriberNumber extends Ton(4)

  case object Alphanumeric extends Ton(5)

  case object Abbreviated extends Ton(6)

  def apply(ton: Byte): Ton = ton match {
    case 0 => Unknown
    case 1 => International
    case 2 => National
    case 3 => NetworkSpecific
    case 4 => SubscriberNumber
    case 5 => Alphanumeric
    case 6 => Abbreviated
    case _ => throw new IllegalArgumentException("Not a valid TON (Tag of Number): " + ton)
  }
}
