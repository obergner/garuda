package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf


/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 15.08.13
 * Time: 21:54
 * To change this template use File | Settings | File Templates.
 */
abstract class BaseBindResponse(commandId: CommandId,
                                commandStatus: Int,
                                seqNo: Option[Int],
                                val systemId: String,
                                tlvParameters: Vector[Tlv],
                                rawPdu: Option[Array[Byte]]) extends PduResponse(commandId,
  commandStatus,
  seqNo,
  tlvParameters,
  rawPdu) {

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {
    encodeNullTerminatedStringInto(systemId, channelBuffer)
  }

  protected[this] def bodyLengthInBytes: Int = {
    systemId.length + 1
  }
}

abstract class BaseBindResponseDecoder[P <: BaseBindResponse] extends PduDecoder[P] {

  protected case class BindResponseBody(systemId: String) extends Body[P]

  type BODY = BindResponseBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): BindResponseBody = {
    val systemId = readSystemIdFrom(channelBuffer)
    BindResponseBody(systemId)
  }
}
