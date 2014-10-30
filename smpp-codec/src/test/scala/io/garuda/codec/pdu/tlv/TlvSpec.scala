package io.garuda.codec.pdu.tlv

import io.garuda.codec.util.{ChannelBufferHelpers, HexHelpers}
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled._
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.08.13
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
class TlvSpec extends Specification {

  "Tlv (class) as an encoder for Tlvs" should {

    "correctly encode a Tlv with null value" in {
      val nullValueTlv = Tlv(Tag.AlertOnMsgDelivery)
      val actualEncoding = buffer(4)

      nullValueTlv.encodeInto(actualEncoding)

      actualEncoding mustEqual ChannelBufferHelpers.createBufferFromHexString("130C0000")
    }

    "correctly encode a Tlv with a proper value" in {
      val value = "Reason"
      val valueAsBytes = value.getBytes("ISO-8859-1")
      val length = valueAsBytes.size
      val expectedEncoding: ByteBuf = ChannelBufferHelpers.createBufferFromHexString("04250006" + HexHelpers
        .byteArrayToHexString(valueAsBytes).substring(2))

      val properValueTlv = Tlv(Tag.DeliveryFailureReason, valueAsBytes)
      val actualEncoding = buffer(4 + length)

      properValueTlv.encodeInto(actualEncoding)


      actualEncoding mustEqual expectedEncoding
    }
  }

  "Tlv (object) as a decoder for Tlvs" should {

    "correctly decode a single byte Tlv" in {
      val expectedTlv = Tlv(Tag.ScInterfaceVersion, HexHelpers.hexStringToByteArray("34"))
      val encoded = ChannelBufferHelpers.createBufferFromHexString("0210000134")

      val actualTlv = Tlv.decodeFrom(encoded)

      actualTlv mustEqual expectedTlv
    }

    "correctly decode a C-string Tlv" in {
      val expectedTlv = Tlv(Tag.Custom(5122), HexHelpers.hexStringToByteArray("6331657400"))
      val encoded = ChannelBufferHelpers.createBufferFromHexString("140200056331657400")

      val actualTlv = Tlv.decodeFrom(encoded)

      actualTlv mustEqual expectedTlv
    }

    "correctly decode a two bytes Tlv" in {
      val expectedTlv = Tlv(Tag.UserMessageReference, HexHelpers.hexStringToByteArray("ce34"))
      val encoded = ChannelBufferHelpers.createBufferFromHexString("02040002ce34")

      val actualTlv = Tlv.decodeFrom(encoded)

      actualTlv mustEqual expectedTlv
    }

    "correctly decode a Tlv carrying a proper message" in {
      val expectedTlv = Tlv(Tag.MessagePayload, HexHelpers.hexStringToByteArray
        ("4f4d4720492077616e7420746f207365652022546865204372617a69657322206c6f6f6b73207369636b21203d5d20"))
      val encoded = ChannelBufferHelpers.
        createBufferFromHexString("0424002f4f4d4720492077616e7420746f2073656520225468652" +
        "04372617a69657322206c6f6f6b73207369636b21203d5d20")

      val actualTlv = Tlv.decodeFrom(encoded)

      actualTlv mustEqual expectedTlv
    }
  }
}
