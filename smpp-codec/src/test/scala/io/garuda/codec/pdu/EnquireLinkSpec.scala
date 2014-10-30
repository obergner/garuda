package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
class EnquireLinkSpec extends Specification {

  "EnquireLink (class) as an encoder for EnquireLink" should {

    "correctly encode an EnquireLink" in {
      val objectUnderTest = EnquireLink(171192039)

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("0000001000000015000000000a342ee7")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "EnquireLink (object) as a decoder for EnquireLink" should {

    "correctly decode an enquire_link_resp" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001000000015000000000a342ee7")
      val header = Header.getFrom(buf)

      val decoded = EnquireLink.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(171192039)
    }
  }
}
