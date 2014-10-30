package io.garuda.codec.pdu

import io.garuda.codec.InvalidCommandIdException
import io.garuda.codec.pdu.CommandId._
import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 11.08.13
 * Time: 21:45
 * To change this template use File | Settings | File Templates.
 */
class CommandIdSpec extends Specification {

  "CommandId as a factory for CommandIds given an int" should {

    "handle bind_transmitter correctly" in {
      val cmdIdInt = 0x00000002
      CommandId(cmdIdInt) mustEqual bind_transmitter
    }

    "handle bind_transmitter_resp correctly" in {
      val cmdIdInt = 0x80000002
      CommandId(cmdIdInt) mustEqual bind_transmitter_resp
    }

    "handle bind_receiver correctly" in {
      val cmdIdInt = 0x00000001
      CommandId(cmdIdInt) mustEqual bind_receiver
    }

    "handle bind_receiver_resp correctly" in {
      val cmdIdInt = 0x80000001
      CommandId(cmdIdInt) mustEqual bind_receiver_resp
    }

    "handle bind_transceiver correctly" in {
      val cmdIdInt = 0x00000009
      CommandId(cmdIdInt) mustEqual bind_transceiver
    }

    "handle bind_transceiver_resp correctly" in {
      val cmdIdInt = 0x80000009
      CommandId(cmdIdInt) mustEqual bind_transceiver_resp
    }

    "throw an InvalidCommandIdException when given an unknown command id" in {
      val cmdIdInt = 0x11000000
      CommandId(cmdIdInt) must throwA[InvalidCommandIdException]
    }
  }

  "CommandId as a parser for CommandIds given a ChannelBuffer" should {

    "not consume any bytes in the given buffer" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x00000002")
      val availableBytesBeforeParse = buf.readableBytes()
      CommandId.getFrom(buf)
      buf.readableBytes() mustEqual availableBytesBeforeParse
    }

    "handle bind_transmitter correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x00000002")
      CommandId.getFrom(buf) mustEqual bind_transmitter
    }

    "handle bind_transmitter_resp correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x80000002")
      CommandId.getFrom(buf) mustEqual bind_transmitter_resp
    }

    "handle bind_receiver correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x00000001")
      CommandId.getFrom(buf) mustEqual bind_receiver
    }

    "handle bind_receiver_resp correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x80000001")
      CommandId.getFrom(buf) mustEqual bind_receiver_resp
    }

    "handle bind_transceiver correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x00000009")
      CommandId.getFrom(buf) mustEqual bind_transceiver
    }

    "handle bind_transceiver_resp correctly" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x80000009")
      CommandId.getFrom(buf) mustEqual bind_transceiver_resp
    }

    "throw an InvalidCommandIdException when given an unknown command id" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0x11000000")
      CommandId.getFrom(buf) must throwA[InvalidCommandIdException]
    }
  }
}
