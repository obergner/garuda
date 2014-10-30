package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}
import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.ByteBuf
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 21:14
 * To change this template use File | Settings | File Templates.
 */
class BindTransceiverResponseSpec extends Specification {

  "BindTransceiverResponse (class) as an encoder for BindTransceiverResponse" should {

    "correctly encode a BindTransceiverResponse without TLVs" in {
      val objectUnderTest = BindTransceiverResponse(0, 235857, "Smsc Simulator", Vector.empty[Tlv])

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual
        ChannelBufferHelpers.createBufferFromHexString("0000001f800000090000000000039951536d73632053696d756c61746f7200")
    }

    "correctly encode a BindTransceiverResponse with TLVs" in {
      val expectedEncoding: ByteBuf =
        ChannelBufferHelpers.createBufferFromHexString("0000001d800000090000000000039943536d7363204757000210000134")
      val objectUnderTest = BindTransceiverResponse(0, 235843, "Smsc GW", InterfaceVersion.SMPP3_4)

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindTransceiverResponse (object) as a decoder for BindTransceiverResponse" should {

    "correctly decode a simple bind_transceiver_resp without TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001f800000090000000000039951536d73632053696d756c61746f7200")
      val header = Header.getFrom(buf)

      val decoded = BindTransceiverResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235857)
      decoded.systemId mustEqual "Smsc Simulator"
    }

    "correctly decode a complex bind_transceiver_resp with TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001d800000090000000000039943536d7363204757000210000134")
      val header = Header.getFrom(buf)

      val decoded = BindTransceiverResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235843)
      decoded.systemId mustEqual "Smsc GW"
      decoded.tlvParameters.size mustEqual 1
      decoded.tlvParameters.exists(tlv => tlv.tag == Tag.ScInterfaceVersion) must_== true
    }
  }
}
