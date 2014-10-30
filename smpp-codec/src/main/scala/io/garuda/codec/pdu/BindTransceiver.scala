package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.09.13
 * Time: 21:17
 * To change this template use File | Settings | File Templates.
 */
case class BindTransceiver(seqNo: Option[Int],
                           override val systemId: String,
                           override val password: String,
                           override val systemType: String,
                           interfaceVersionByte: Byte,
                           addressRangeTon: Byte = 0x00,
                           addressRangeNpi: Byte = 0x00,
                           addressRangeAddress: String = "",
                           rawPdu: Option[Array[Byte]] = None) extends BaseBind[BindTransceiverResponse](CommandId
  .bind_transceiver,
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
                     interfaceVersion: InterfaceVersion = InterfaceVersion.SMPP5_0): BindTransceiverResponse =
    BindTransceiverResponse(commandStatus,
      sequenceNumber,
      systemId,
      Vector(Tlv(Tag.ScInterfaceVersion, Array[Byte](interfaceVersion.asByte))))
}

object BindTransceiver extends BaseBindDecoder[BindTransceiverResponse, BindTransceiver] {

  def apply(seqNo: Option[Int],
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address,
            rawPdu: Option[Array[Byte]]): BindTransceiver = {
    BindTransceiver(seqNo, systemId, password, systemType, interfaceVersion.asByte, addressRange.ton.asByte,
      addressRange.npi.asByte, addressRange.address, rawPdu)
  }

  def apply(systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address): BindTransceiver = {
    BindTransceiver(None, systemId, password, systemType, interfaceVersion, addressRange, None)
  }

  def apply(seqNo: Int,
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address): BindTransceiver = {
    BindTransceiver(Some(seqNo), systemId, password, systemType, interfaceVersion, addressRange, None)
  }

  def apply(seqNo: Int,
            systemId: String,
            password: String,
            systemType: String,
            interfaceVersion: InterfaceVersion,
            addressRange: Address,
            rawPdu: Array[Byte]): BindTransceiver = {
    BindTransceiver(Some(seqNo), systemId, password, systemType, interfaceVersion, addressRange, Some(rawPdu))
  }

  protected[this] def makePduFrom(header: Header, body: BindTransceiver.BaseBindBody, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): BindTransceiver = {
    require(tlvParameters.isEmpty, "BindTransceiver does not support optional TLVs")
    BindTransceiver(header.sequenceNumber, body.systemId, body.password, body.systemType, body.interfaceVersionByte,
      body.addressRangeTonByte, body.addressRangeNpiByte, body.addressRangeAddress, Some(rawPdu))
  }
}
