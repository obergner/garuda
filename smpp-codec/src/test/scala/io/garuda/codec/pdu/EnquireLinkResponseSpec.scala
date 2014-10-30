package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.09.13
 * Time: 21:46
 * To change this template use File | Settings | File Templates.
 */
class EnquireLinkResponseSpec extends Specification {

  "EnquireLinkResponse (class) as an encoder for EnquireLinkResponse" should {

    "correctly encode an EnquireLinkResponse" in {
      val objectUnderTest = EnquireLinkResponse(0, 171192045)

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("0000001080000015000000000a342eed")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "EnquireLinkResponse (object) as a decoder for EnquireLinkResponse" should {

    "correctly decode an enquire_link_resp" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001080000015000000000a342eed")
      val header = Header.getFrom(buf)

      val decoded = EnquireLinkResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(171192045)
    }
  }
}
