package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 04.10.13
 * Time: 15:10
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class ReplaceIfPresentFlag(val value: Byte) {

  def asByte: Byte = value
}

object ReplaceIfPresentFlag {

  case object DoNotReplace extends ReplaceIfPresentFlag(0x00)

  case object Replace extends ReplaceIfPresentFlag(0x01)

  case class Reserved(override val value: Byte) extends ReplaceIfPresentFlag(value) {
    require(value > 0x01, "Reserved ReplaceIfPresentFlags need to have a value greater than 0x01: " + value)
  }

  def apply(value: Byte): ReplaceIfPresentFlag = value match {
    case 0x00 => DoNotReplace
    case 0x01 => Replace
    case value: Byte => Reserved(value)
  }
}
