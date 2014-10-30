package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.ByteBuf
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 23:52
 * To change this template use File | Settings | File Templates.
 */
class BindReceiverSpec extends Specification {

  "BindReceiver (class) as an encoder for BindReceiver" should {

    "correctly encode a BindReceiver with unknown address range" in {
      val objectUnderTest = BindReceiver(235873, "twitter", "twitter", "", InterfaceVersion.SMPP3_4,
        Address.Unknown)
      val expectedEncoding: ByteBuf = ChannelBufferHelpers.
        createBufferFromHexString("00000025000000010000000000039961747769747465720074776974746572000034000000")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode a BindReceiver with known address range" in {
      val objectUnderTest = BindReceiver(235873, "twitter", "twitter", "", InterfaceVersion.SMPP3_4,
        Address(Ton.International, Npi.IsdnNumberingPlan, ""))
      val expectedEncoding: ByteBuf = ChannelBufferHelpers.
        createBufferFromHexString("00000025000000010000000000039961747769747465720074776974746572000034010100")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "BindReceiver (object) as a decoder for BindReceiver" should {

    "correctly decode a bind_transmitter with known address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("00000025000000010000000000039961747769747465720074776974746572000034010100")
      val header = Header.getFrom(buf)

      val decoded = BindReceiver.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235873)
      decoded.systemId mustEqual "twitter"
      decoded.password mustEqual "twitter"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address(Ton.International, Npi.IsdnNumberingPlan, "")
    }

    "correctly decode a bind_transmitter with unknown address range" in {
      val buf = ChannelBufferHelpers.
        createBufferFromHexString("00000025000000010000000000039961747769747465720074776974746572000034000000")
      val header = Header.getFrom(buf)

      val decoded = BindReceiver.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(235873)
      decoded.systemId mustEqual "twitter"
      decoded.password mustEqual "twitter"
      decoded.systemType mustEqual ""
      decoded.interfaceVersion mustEqual InterfaceVersion.SMPP3_4
      decoded.addressRange mustEqual Address.Unknown
    }
  }
}
