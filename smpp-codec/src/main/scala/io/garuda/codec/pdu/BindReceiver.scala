package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 23:51
 * To change this template use File | Settings | File Templates.
 */
case class BindReceiver(seqNo: Option[Int],
                        override val systemId: String,
                        override val password: String,
                        override val systemType: String,
                        interfaceVersionByte: Byte,
                        addressRangeTon: Byte = 0x00,
                        addressRangeNpi: Byte = 0x00,
                        addressRangeAddress: String = "",
                        rawPdu: Option[Array[Byte]] = None)
  extends BaseBind[BindReceiverResponse](CommandId
    .bind_receiver,
    seqNo,
    systemId,
    password,
    systemType,
    interfaceVersionByte,
    addressRangeTon,
    addressRangeNpi,
    addressRangeAddress,
    rawPdu) {

  def createResponse(commandStatus: Int,
                     systemId: String,
                     interfaceVersion: InterfaceVersion = InterfaceVersion.SMPP5_0): BindReceiverResponse =
    BindReceiverResponse(commandStatus,
      sequenceNumber,
      systemId,
      Vector(Tlv(Tag.ScInterfaceVersion, Array[Byte](interfaceVersion.asByte))))
}

object BindReceiver extends BaseBindDecoder[BindReceiverResponse, BindReceiver] {

  def apply(seqNo: Option[Int],
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address,
            rawPdu: Option[Array[Byte]]): BindReceiver = {
    BindReceiver(seqNo, systemId, password, systemType, interfaceVersion.asByte, addressRange.ton.asByte,
      addressRange.npi.asByte, addressRange.address, rawPdu)
  }

  def apply(systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address): BindReceiver = {
    BindReceiver(None, systemId, password, systemType, interfaceVersion, addressRange, None)
  }

  def apply(seqNo: Int,
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address): BindReceiver = {
    BindReceiver(Some(seqNo), systemId, password, systemType, interfaceVersion, addressRange, None)
  }

  def apply(seqNo: Int,
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address,
            rawPdu: Array[Byte]): BindReceiver = {
    BindReceiver(Some(seqNo), systemId, password, systemType, interfaceVersion, addressRange, Some(rawPdu))
  }

  protected[this] def makePduFrom(header: Header, body: BindReceiver.BaseBindBody, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): BindReceiver = {
    require(tlvParameters.isEmpty, "BindReceiver does not support optional TLVs")
    BindReceiver(header.sequenceNumber, body.systemId, body.password, body.systemType, body.interfaceVersionByte,
      body.addressRangeTonByte, body.addressRangeNpiByte, body.addressRangeAddress, Some(rawPdu))
  }
}
