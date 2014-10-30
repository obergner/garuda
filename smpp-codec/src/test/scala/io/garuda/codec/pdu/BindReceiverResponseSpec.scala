package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}
import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.ByteBuf
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 19:49
 * To change this template use File | Settings | File Templates.
 */
class BindReceiverResponseSpec extends Specification {

  "BindReceiverResponse (class) as an encoder for BindReceiverResponse" should {

    "correctly encode a BindReceiverResponse without TLVs" in {
      val objectUnderTest = BindReceiverResponse(0, 235874, "twitter", Vector.empty[Tlv])
      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual
        ChannelBufferHelpers.createBufferFromHexString("000000188000000100000000000399627477697474657200")
    }

    "correctly encode a BindReceiverResponse with TLVs" in {
      val expectedEncoding: ByteBuf =
        ChannelBufferHelpers.createBufferFromHexString("0000001d80000001000000000003996274776974746572000210000134")
      val objectUnderTest = BindReceiverResponse(0, 235874, "twitter", InterfaceVersion.SMPP3_4)
      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindReceiverResponse (object) as a decoder for BindReceiverResponse" should {

    "correctly decode a simple bind_receiver_resp without TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("000000188000000100000000000399627477697474657200")
      val header = Header.getFrom(buf)

      val decoded = BindReceiverResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235874)
      decoded.systemId mustEqual "twitter"
    }

    "correctly decode a complex bind_transmitter_resp with TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001d80000001000000000003996274776974746572000210000134")
      val header = Header.getFrom(buf)

      val decoded = BindReceiverResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235874)
      decoded.systemId mustEqual "twitter"
      decoded.tlvParameters.size mustEqual 1
      decoded.tlvParameters.exists(tlv => tlv.tag == Tag.ScInterfaceVersion) must_== true
    }
  }
}
