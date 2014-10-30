package io.garuda.codec.pdu

import io.garuda.codec.InvalidCommandIdException
import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.Unpooled._
import org.specs2.mutable.Specification


/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 16:30
 * To change this template use File | Settings | File Templates.
 */
class HeaderSpec extends Specification {

  "Header as an encoder for Headers" should {

    "correctly encode a Header" in {
      val headerToEncode = Header(16, CommandId(0x00000001), 0, 1)
      val expectedEncoding = ChannelBufferHelpers.createBufferFromHexString("00000010" + "00000001" + "00000000" +
        "00000001")
      val actualEncoding = buffer(16)

      headerToEncode.encodeInto(actualEncoding)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "Header as a decoder for Headers" should {

    "return None if there are not enough bytes in the buffer (yet)" in {
      val incompleteBuffer = buffer(15)

      val decodedHeader = Header.getFrom(incompleteBuffer)

      decodedHeader must_== None
    }

    "throw an IllegalArgumentException if decoded command length is below minimum (16)" in {
      val encodedHeaderWithIllegalCommandLength = ChannelBufferHelpers.createBufferFromHexString("0000000F" +
        "00000002" +
        "00000008" +
        "000000A0")

      Header.getFrom(encodedHeaderWithIllegalCommandLength) must throwA[IllegalArgumentException]
    }

    "throw an InvalidCommandIdException if decoded command id is illegal" in {
      val encodedHeaderWithIllegalCommandId = ChannelBufferHelpers.createBufferFromHexString("000000A0" +
        "10000000" +
        "00000008" + "000000A0")

      Header.getFrom(encodedHeaderWithIllegalCommandId) must throwA[InvalidCommandIdException]
    }

    "correctly decode a legal Header" in {
      val expectedLegalHeader = Header(160, CommandId(0x02), 8, 160)
      val encodedLegalHeader = ChannelBufferHelpers.createBufferFromHexString("000000A0" +
        "00000002" +
        "00000008" + "000000A0")

      val decodedHeader = Header.getFrom(encodedLegalHeader).get

      decodedHeader mustEqual expectedLegalHeader
    }
  }
}
