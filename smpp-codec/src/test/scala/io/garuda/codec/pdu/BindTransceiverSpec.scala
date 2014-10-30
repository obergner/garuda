package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.09.13
 * Time: 21:22
 * To change this template use File | Settings | File Templates.
 */
class BindTransceiverSpec extends Specification {

  "BindTransceiver (class) as an encoder for BindTransceiver" should {

    "correctly encode a BindTransceiver with unknown address range" in {
      val objectUnderTest = BindTransceiver(235857, "ALL_TW", "ALL_TW", "", InterfaceVersion.SMPP3_4,
        Address.Unknown)
      val expectedEncoding = ChannelBufferHelpers.
        createBufferFromHexString("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034000000")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode a BindTransceiver with address range" in {
      val objectUnderTest = BindTransceiver(235857, "ALL_TW", "ALL_TW", "", InterfaceVersion.SMPP3_4,
        Address(Ton.International, Npi.IsdnNumberingPlan, ""))
      val expectedEncoding = ChannelBufferHelpers.
        createBufferFromHexString("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034010100")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindTransceiver (object) as a decoder for BindTransceiver" should {

    "correctly decode a bind_transmitter with known address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034010100")
      val header = Header.getFrom(buf)

      val decoded = BindTransceiver.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235857)
      decoded.systemId mustEqual "ALL_TW"
      decoded.password mustEqual "ALL_TW"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address(Ton.International, Npi.IsdnNumberingPlan, "")
    }

    "correctly decode a bind_transmitter with unknown address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034000000")
      val header = Header.getFrom(buf)

      val decoded = BindTransceiver.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235857)
      decoded.systemId mustEqual "ALL_TW"
      decoded.password mustEqual "ALL_TW"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address.Unknown
    }
  }
}
