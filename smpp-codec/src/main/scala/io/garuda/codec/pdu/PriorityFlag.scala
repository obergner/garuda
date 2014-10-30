package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 03.10.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class PriorityFlag(val value: Byte) extends Ordered[PriorityFlag] {

  def compare(that: PriorityFlag): Int = {
    this.value compare that.value
  }
}

object PriorityFlag {

  case object Zero extends PriorityFlag(0x00)

  case object One extends PriorityFlag(0x01)

  case object Two extends PriorityFlag(0x02)

  case object Three extends PriorityFlag(0x03)

  case object Four extends PriorityFlag(0x04)

  case class Reserved(override val value: Byte) extends PriorityFlag(value) {
    require(value > 0x04, "Reserved PriorityFlags need to have a value greater than 0x04: " + value)
  }

  def apply(value: Byte): PriorityFlag = value match {
    case 0x00 => Zero
    case 0x01 => One
    case 0x02 => Two
    case 0x03 => Three
    case 0x04 => Four
    case value: Byte => Reserved(value)
  }
}
