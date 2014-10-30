package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
class PduSpec extends Specification {

  case class StringBodyPdu(commandId: CommandId, commandStatus: Int, seqNo: Int,
                           body: String) extends Pdu(commandId, commandStatus, Some(seqNo), Vector.empty[Tlv],
    None) {
    protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {
      encodeNullTerminatedStringInto(body, channelBuffer)
    }

    protected[this] def bodyLengthInBytes: Int = body.length + 1
  }

  "Pdu as an encoder" should {

    "correctly encode command length" in {
      val body: String = "correctly encode command length"
      val pduToEncode = StringBodyPdu(CommandId.bind_receiver, 0, 1, body)
      val encodedPduLength = Header.Length + body.getBytes("ISO-8859-1").size + 1

      val encodedPdu = pduToEncode.encode()

      encodedPdu.readInt() mustEqual encodedPduLength
    }

    "correctly encode CommandId" in {
      val body: String = "correctly encode CommandId"
      val pduToEncode = StringBodyPdu(CommandId.bind_receiver, 0, 1, body)
      val encodedPduLength = Header.Length + body.getBytes("ISO-8859-1").size + 1

      val encodedPdu = pduToEncode.encode()

      encodedPdu.skipBytes(4) // Skip length
      CommandId.getFrom(encodedPdu) mustEqual CommandId.bind_receiver
    }

    "correctly encode command status" in {
      val body: String = "correctly encode command status"
      val expectedCommandStatus = 14
      val pduToEncode = StringBodyPdu(CommandId.bind_receiver, expectedCommandStatus, 1, body)
      val encodedPduLength = Header.Length + body.getBytes("ISO-8859-1").size + 1

      val encodedPdu = pduToEncode.encode()

      encodedPdu.getInt(8) mustEqual expectedCommandStatus
    }

    "correctly encode sequenceNumber" in {
      val body: String = "correctly encode sequence number"
      val expectedSequenceNumber = 14
      val pduToEncode = StringBodyPdu(CommandId.bind_receiver, 0, expectedSequenceNumber, body)
      val encodedPduLength = Header.Length + body.getBytes("ISO-8859-1").size + 1

      val encodedPdu = pduToEncode.encode()

      encodedPdu.getInt(12) mustEqual expectedSequenceNumber
    }
  }
}
