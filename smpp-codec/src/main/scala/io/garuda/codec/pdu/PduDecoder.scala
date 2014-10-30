package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

import scala.collection.mutable


/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 19:22
 * To change this template use File | Settings | File Templates.
 */
trait PduDecoder[P <: Pdu] extends DecodingSupport {

  protected[this] abstract class Body[U <: Pdu]

  type BODY <: Body[P]

  def decodeFrom(header: Header, channelBuffer: ByteBuf): Option[P] = {
    if (channelBuffer.readableBytes() < header.commandLength) return None

    val startPduIdx = channelBuffer.readerIndex
    val finalPduIdx = startPduIdx + header.commandLength
    channelBuffer.skipBytes(Header.Length)

    val body: BODY = decodeBodyFrom(channelBuffer)

    val tlvParameters = new mutable.ListBuffer[Tlv]
    while (channelBuffer.readerIndex < finalPduIdx) {
      val tlv: Tlv = Tlv.decodeFrom(channelBuffer)
      tlvParameters += tlv
    }

    val rawPdu = new Array[Byte](header.commandLength)
    channelBuffer.getBytes(startPduIdx, rawPdu)

    Some(makePduFrom(header, body, tlvParameters.toVector, rawPdu))
  }

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): BODY

  protected[this] def makePduFrom(header: Header, body: BODY, tlvParameters: Vector[Tlv], rawPdu: Array[Byte]): P
}
