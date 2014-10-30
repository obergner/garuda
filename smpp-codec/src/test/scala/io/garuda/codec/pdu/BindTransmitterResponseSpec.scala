package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}
import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.08.13
 * Time: 21:02
 * To change this template use File | Settings | File Templates.
 */
class BindTransmitterResponseSpec extends Specification {

  "BindTransmitterResponse (class) as an encoder for BindTransmitterResponse" should {

    "correctly encode a BindTransmitterResponse without TLVs" in {
      val objectUnderTest = BindTransmitterResponse(0, 235871, "TWITTER", Vector.empty[Tlv])

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual
        ChannelBufferHelpers.createBufferFromHexString("0000001880000002000000000003995f5457495454455200")
    }

    "correctly encode a BindTransmitterResponse with TLVs" in {
      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("0000001d80000002000000000003995f54574954544552000210000134")
      val objectUnderTest = BindTransmitterResponse(0, 235871, "TWITTER", InterfaceVersion.SMPP3_4)

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindTransmitterResponse (object) as a decoder for BindTransmitterResponse" should {

    "correctly decode a simple bind_transmitter_resp without TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001f800000020000000000039951536d73632053696d756c61746f7200")
      val header = Header.getFrom(buf)

      val decoded = BindTransmitterResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235857)
      decoded.systemId mustEqual "Smsc Simulator"
    }

    "correctly decode a complex bind_transmitter_resp with TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001d80000002000000000003995f54574954544552000210000134")
      val header = Header.getFrom(buf)

      val decoded = BindTransmitterResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(235871)
      decoded.systemId mustEqual "TWITTER"
      decoded.tlvParameters.size mustEqual 1
      decoded.tlvParameters.exists(tlv => tlv.tag == Tag.ScInterfaceVersion) must_== true
    }
  }
}
