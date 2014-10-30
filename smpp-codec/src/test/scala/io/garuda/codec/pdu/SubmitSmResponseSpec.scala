package io.garuda.codec.pdu

import io.garuda.codec.util.ChannelBufferHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 06.10.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
class SubmitSmResponseSpec extends Specification {

  "SubmitSmResponse (class) as an encoder for SubmitSmResponse" should {

    "correctly encode a SubmitSmResponse without TLVs" in {
      val objectUnderTest = SubmitSmResponse(0, Some(171192033), "94258431594")
      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("0000001c80000004000000000a342ee1393432353834333135393400")

      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "SubmitSmResponse (object) as a decoder for SubmitSmResponse" should {

    "correctly decode a SubmitSmResponse without TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("0000001c80000004000000000a342ee1393432353834333135393400")
      val header = Header.getFrom(buf)

      val decoded = SubmitSmResponse.decodeFrom(header.get, buf).get

      decoded.commandStatus mustEqual 0
      decoded.sequenceNumber mustEqual Some(171192033)
      decoded.messageId mustEqual "94258431594"
    }
  }
}
