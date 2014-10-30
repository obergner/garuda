package io.garuda.codec.pdu

import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.09.13
 * Time: 21:52
 * To change this template use File | Settings | File Templates.
 */
class PriorityFlagSpec extends Specification {

  "PriorityFlag as a factory for PriorityFlags given a byte" should {

    "handle Zero correctly" in {
      PriorityFlag(0x00) mustEqual PriorityFlag.Zero
    }

    "handle One correctly" in {
      PriorityFlag(0x01) mustEqual PriorityFlag.One
    }

    "handle Two correctly" in {
      PriorityFlag(0x02) mustEqual PriorityFlag.Two
    }

    "handle Three correctly" in {
      PriorityFlag(0x03) mustEqual PriorityFlag.Three
    }

    "handle Four correctly" in {
      PriorityFlag(0x04) mustEqual PriorityFlag.Four
    }

    "handle a custom PriorityFlag correctly" in {
      val priorityFlagByte: Byte = 0x11
      PriorityFlag(priorityFlagByte) mustEqual PriorityFlag.Reserved(priorityFlagByte)
    }
  }
}

