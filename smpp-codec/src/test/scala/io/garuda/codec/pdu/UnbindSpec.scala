package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 22:57
 * To change this template use File | Settings | File Templates.
 */
class UnbindSpec extends Specification {

  "Unbind (class) as an encoder for Unbind" should {

    "correctly encode an Unbind" in {
      val objectUnderTest = Unbind(1)

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("00000010000000060000000000000001")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "Unbind (object) as a decoder for Unbind" should {

    "correctly decode an enquire_link_resp" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("00000010000000060000000000000001")
      val header = Header.getFrom(buf)

      val decoded = Unbind.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(1)
    }
  }
}
