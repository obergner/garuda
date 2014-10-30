package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 19.08.13
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
case class BindTransmitterResponse(commandStatus: Int,
                                   seqNo: Option[Int],
                                   override val systemId: String,
                                   tlvParameters: Vector[Tlv] = Vector.empty[Tlv],
                                   rawPdu: Option[Array[Byte]] = None) extends BaseBindResponse(CommandId
  .bind_transmitter_resp,
  commandStatus,
  seqNo,
  systemId,
  tlvParameters,
  rawPdu)

object BindTransmitterResponse extends BaseBindResponseDecoder[BindTransmitterResponse] {

  def apply(commandStatus: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindTransmitterResponse = {
    new BindTransmitterResponse(commandStatus, None, systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindTransmitterResponse = {
    new BindTransmitterResponse(commandStatus, Some(sequenceNumber), systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindTransmitterResponse = {
    new BindTransmitterResponse(commandStatus, None, systemId,
      Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
        interfaceVersion
          .asByte)))
  }


  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindTransmitterResponse = {
    new BindTransmitterResponse(commandStatus, Some(sequenceNumber), systemId,
      Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
        interfaceVersion.asByte)))
  }

  protected[this] def makePduFrom(header: Header,
                                  body: BindTransmitterResponse.BindResponseBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): BindTransmitterResponse = BindTransmitterResponse(header
    .commandStatus,
    header
      .sequenceNumber,
    body.systemId,
    tlvParameters,
    Some(rawPdu))
}
