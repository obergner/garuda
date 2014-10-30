package io.garuda.codec.pdu.tlv

import io.garuda.codec.util.ChannelBufferHelpers
import io.netty.buffer.Unpooled._
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.08.13
 * Time: 16:22
 * To change this template use File | Settings | File Templates.
 */
class TagSpec extends Specification {

  "Tag (class) as an encoder for Tags" should {

    "correctly encode ScInterfaceVersion" in {
      val expectedEncoding = ChannelBufferHelpers.createBufferFromHexString("0x0210")

      val actualEncoding = buffer(2)
      Tag.ScInterfaceVersion.encodeInto(actualEncoding)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "Tag (object) as a decoder for Tags" should {

    "correctly decode OrigMscAddr" in {
      val encoded = ChannelBufferHelpers.createBufferFromHexString("0x8081")
      val decodedTag = Tag.decodeFrom(encoded)

      decodedTag mustEqual Tag.OrigMscAddr
    }

    "correctly decode CustomTag" in {
      val encoded = ChannelBufferHelpers.createBufferFromHexString("0x9999")
      val decodedTag = Tag.decodeFrom(encoded)

      decodedTag mustEqual Tag.Custom(0x9999.toShort)
    }
  }
}
