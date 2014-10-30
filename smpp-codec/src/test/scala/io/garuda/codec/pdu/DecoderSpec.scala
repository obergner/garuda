package io.garuda.codec.pdu

import io.garuda.codec.TerminatingNullByteNotFoundException
import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.ByteBuf
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 17.08.13
 * Time: 21:40
 * To change this template use File | Settings | File Templates.
 */
class DecoderSpec extends Specification {

  class StringDecoder extends Decoder[String] {
    def decodeFrom(channelBuffer: ByteBuf): String = readMessageIdFrom(channelBuffer)
  }

  val objectUnderTest: StringDecoder = new StringDecoder

  "StringDecoder as a decoder of null-terminated strings" should {

    "throw an TerminatingNullByteNotFoundException if the supplied ChannelBuffer does not contain a null byte" in {
      val buf = ChannelBufferHelpers.createBufferFromHexString("0602034578")
      objectUnderTest.decodeFrom(buf) must throwA[TerminatingNullByteNotFoundException]
    }

    "correctly decode a proper (non-zero-length) null-terminated string in a ChannelBuffer WITH NO skipped bytes" in {
      val expectedString = "Hello, World!"
      val helloWorldHex = "48656c6c6f2c20576f726c6421"

      val buf = ChannelBufferHelpers.createBufferFromHexString(helloWorldHex + "0002a0")
      val decodedString = objectUnderTest.decodeFrom(buf)

      decodedString mustEqual expectedString
    }

    "skip the null byte after successfully decoding a null-terminated string" in {
      val expectedStringHex =
        "506c656173652c20646f202425264020736b697020746865207465726d696e6174696e67206e756c6c2062797465"

      val buf = ChannelBufferHelpers.createBufferFromHexString(expectedStringHex + "00a34578")
      objectUnderTest.decodeFrom(buf)
      val firstByteAfterDecoding = buf.readByte()

      firstByteAfterDecoding mustEqual 0xa3.toByte
    }

    "correctly decode a zero-length null-terminated string in a ChannelBuffer WITH NO skipped bytes" in {
      val expectedString = ""
      val zeroLengthHex = "00"

      val buf = ChannelBufferHelpers.createBufferFromHexString(zeroLengthHex + "0002a0")
      val decodedString = objectUnderTest.decodeFrom(buf)

      decodedString mustEqual expectedString
    }

    "correctly decode a proper (non-zero-length) null-terminated string in a ChannelBuffer WITH skipped bytes" in {
      val expectedString = "This String is preceded by 5 skipped bytes in its ChannelBuffer"
      val expectedStringHex =
        "5468697320537472696e67206973207072656365646564206279203520736b697070656420627974657320696e20697473204368616e6e656c427566666572"

      val buf = ChannelBufferHelpers.createBufferFromHexString("0102030405" + expectedStringHex + "0002a0")
      buf.skipBytes(5)
      val decodedString = objectUnderTest.decodeFrom(buf)

      decodedString mustEqual expectedString
    }

    "correctly decode a zero-length null-terminated string in a ChannelBuffer WITH skipped bytes" in {
      val expectedString = ""
      val zeroLengthHex = "00"

      val buf = ChannelBufferHelpers.createBufferFromHexString("01020304050607" + zeroLengthHex + "0002a0")
      buf.skipBytes(7)
      val decodedString = objectUnderTest.decodeFrom(buf)

      decodedString mustEqual expectedString
    }

    "correctly decode a zero-length null-terminated string in a ChannelBuffer WITH skipped bytes" in {
      val expectedString = ""
      val zeroLengthHex = "00"

      val buf = ChannelBufferHelpers.createBufferFromHexString("0102" + zeroLengthHex + "00a4c5")
      buf.skipBytes(2)
      val decodedString = objectUnderTest.decodeFrom(buf)

      decodedString mustEqual expectedString
    }
  }
}
