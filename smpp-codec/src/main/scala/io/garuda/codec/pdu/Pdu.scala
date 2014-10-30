package io.garuda.codec.pdu

import java.util.UUID

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled._

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 14.08.13
 * Time: 21:26
 * To change this template use File | Settings | File Templates.
 */
abstract class Pdu(commandId: CommandId,
                   commandStatus: Int,
                   private var _sequenceNumber: Option[Int],
                   tlvParameters: Vector[Tlv],
                   rawPdu: Option[Array[Byte]]) extends EncodingSupport {

  val id: UUID = UUID.randomUUID()

  def sequenceNumber: Option[Int] = _sequenceNumber

  def sequenceNumber_=(sequenceNumber: Option[Int]) {
    _sequenceNumber = sequenceNumber
  }

  def assignSequenceNumber(seqNo: Int): this.type = {
    if (sequenceNumber.isDefined)
      throw new IllegalStateException("Illegal attempt to re-assign a sequence number to a PDU that already has a " +
        "sequence number assigned")
    sequenceNumber = Some(seqNo)
    this
  }

  def header: Header = Header(0, commandId, commandStatus, sequenceNumber)

  def encode(): ByteBuf = {
    val channelBuffer = buffer(lengthInBytes)
    encodeInto(channelBuffer)
    channelBuffer
  }

  protected[this] def lengthInBytes: Int = {
    val headerLengthInBytes = header.lengthInBytes
    val tlvParamsLengthInBytes: Int = tlvParameters.foldLeft[Int](0)(_ + _.lengthInBytes)

    headerLengthInBytes + bodyLengthInBytes + tlvParamsLengthInBytes
  }

  protected[this] def bodyLengthInBytes: Int

  def encodeInto(channelBuffer: ByteBuf): Unit = {
    if (sequenceNumber.isEmpty)
      throw new IllegalStateException("Illegal attempt to encode a PDU without assigned sequence number")
    val offset = channelBuffer.writerIndex()
    val headerWithZeroLength = Header(0, commandId, commandStatus, sequenceNumber)
    headerWithZeroLength.encodeInto(channelBuffer)
    encodeBodyInto(channelBuffer)
    encodeTlvParametersInto(channelBuffer)
    val calculatedPduLength = channelBuffer.writerIndex() - offset
    channelBuffer.setInt(offset, calculatedPduLength)
  }

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit

  private[this] def encodeTlvParametersInto(channelBuffer: ByteBuf) = {
    tlvParameters.foreach(tlv => tlv.encodeInto(channelBuffer))
  }
}
