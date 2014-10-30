package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf


/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 15.08.13
 * Time: 22:01
 * To change this template use File | Settings | File Templates.
 */
abstract class BaseBind[R <: BaseBindResponse](commandId: CommandId,
                                               sequenceNumber: Option[Int],
                                               val systemId: String,
                                               val password: String,
                                               val systemType: String,
                                               interfaceVersionByte: Byte,
                                               addressRangeTon: Byte,
                                               addressRangeNpi: Byte,
                                               addressRangeAddress: String,
                                               rawPdu: Option[Array[Byte]]) extends PduRequest[R](commandId,
  sequenceNumber,
  Vector.empty[Tlv],
  rawPdu) {

  def interfaceVersion: InterfaceVersion = InterfaceVersion(interfaceVersionByte)

  def addressRange: Address = Address(Ton(addressRangeTon), Npi(addressRangeNpi), addressRangeAddress)

  def createResponse(commandStatus: Int,
                     systemId: String,
                     interfaceVersion: InterfaceVersion = InterfaceVersion.SMPP5_0): R

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {
    encodeNullTerminatedStringInto(systemId, channelBuffer)
    encodeNullTerminatedStringInto(password, channelBuffer)
    encodeNullTerminatedStringInto(systemType, channelBuffer)
    channelBuffer.writeByte(interfaceVersionByte)
    channelBuffer.writeByte(addressRangeTon)
    channelBuffer.writeByte(addressRangeNpi)
    encodeNullTerminatedStringInto(addressRangeAddress, channelBuffer)
  }

  protected[this] def bodyLengthInBytes: Int = {
    systemId.length + 1 +
      password.length + 1 +
      systemType.length + 1 +
      1 + /* interfaceVersionByte */
      addressRange.lengthInBytes
  }
}

abstract class BaseBindDecoder[R <: BaseBindResponse, P <: BaseBind[R]] extends PduDecoder[P] {

  protected case class BaseBindBody(systemId: String,
                                    password: String,
                                    systemType: String,
                                    interfaceVersionByte: Byte,
                                    addressRangeTonByte: Byte,
                                    addressRangeNpiByte: Byte,
                                    addressRangeAddress: String) extends Body[P]

  type BODY = BaseBindBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): BaseBindBody = {
    val systemId = readSystemIdFrom(channelBuffer)
    val password = readPasswordFrom(channelBuffer)
    val systemType = readSystemTypeFrom(channelBuffer)
    val interfaceVersionByte = channelBuffer.readByte()
    val addressRangeTonByte = channelBuffer.readByte()
    val addressRangeNpiByte = channelBuffer.readByte()
    val addressRangeAddress = readAddressRangeFrom(channelBuffer)

    BaseBindBody(systemId, password, systemType, interfaceVersionByte, addressRangeTonByte, addressRangeNpiByte, addressRangeAddress)
  }
}
