package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 23:02
 * To change this template use File | Settings | File Templates.
 */
class GenericNackSpec extends Specification {

  "GenericNack (class) as an encoder for GenericNack" should {

    "correctly encode a GenericNack" in {
      val objectUnderTest = GenericNack(0x00000001, 535159)

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("00000010800000000000000100082a77")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "GenericNack (object) as a decoder for GenericNack" should {

    "correctly decode a generic_nack" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("00000010800000000000000100082a77")
      val header = Header.getFrom(buf)

      val decoded = GenericNack.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0x00000001
      decoded.sequenceNumber mustEqual Some(535159)
    }
  }
}
