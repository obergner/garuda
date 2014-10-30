package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 22:50
 * To change this template use File | Settings | File Templates.
 */
class BindTransmitterSpec extends Specification {

  "BindTransmitter (class) as an encoder for BindTransmitter" should {

    "correctly encode a BindTransmitter with unknown address range" in {
      val objectUnderTest = BindTransmitter(235871, "twitter", "twitter", "", InterfaceVersion.SMPP3_4,
        Address.Unknown)
      val expectedEncoding = ChannelBufferHelpers.
        createBufferFromHexString("0000002500000002000000000003995f747769747465720074776974746572000034000000")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode a BindTransmitter with address range" in {
      val objectUnderTest = BindTransmitter(235857, "ALL_TW", "ALL_TW", "", InterfaceVersion.SMPP3_4,
        Address(Ton.International, Npi.IsdnNumberingPlan, ""))
      val expectedEncoding = ChannelBufferHelpers.
        createBufferFromHexString("00000023000000020000000000039951414c4c5f545700414c4c5f5457000034010100")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindTransmitter (object) as a decoder for BindTransmitter" should {

    "correctly decode a bind_transmitter with known address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("0000002500000002000000000003995f747769747465720074776974746572000034010100")
      val header = Header.getFrom(buf)

      val decoded = BindTransmitter.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235871)
      decoded.systemId mustEqual "twitter"
      decoded.password mustEqual "twitter"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address(Ton.International, Npi.IsdnNumberingPlan, "")
    }

    "correctly decode a bind_transmitter with unknown address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("0000002500000002000000000003995f747769747465720074776974746572000034000000")
      val header = Header.getFrom(buf)

      val decoded = BindTransmitter.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235871)
      decoded.systemId mustEqual "twitter"
      decoded.password mustEqual "twitter"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address.Unknown
    }
  }
}
