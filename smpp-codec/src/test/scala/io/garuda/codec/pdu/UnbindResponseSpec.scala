package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 22:49
 * To change this template use File | Settings | File Templates.
 */
class UnbindResponseSpec extends Specification {

  "UnbindResponse (class) as an encoder for UnbindResponse" should {

    "correctly encode an UnbindResponse" in {
      val objectUnderTest = UnbindResponse(0, 1)

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("00000010800000060000000000000001")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "UnbindResponse (object) as a decoder for UnbindResponse" should {

    "correctly decode an unbind_resp" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("00000010800000060000000000000001")
      val header = Header.getFrom(buf)

      val decoded = UnbindResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(1)
    }
  }
}
