package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 19:46
 * To change this template use File | Settings | File Templates.
 */
case class BindReceiverResponse(commandStatus: Int,
                                seqNo: Option[Int],
                                override val systemId: String,
                                tlvParameters: Vector[Tlv] = Vector.empty[Tlv],
                                rawPdu: Option[Array[Byte]] = None) extends BaseBindResponse(CommandId
  .bind_receiver_resp,
  commandStatus,
  seqNo,
  systemId,
  tlvParameters,
  rawPdu)

object BindReceiverResponse extends BaseBindResponseDecoder[BindReceiverResponse] {

  def apply(commandStatus: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindReceiverResponse = {
    new BindReceiverResponse(commandStatus, None, systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindReceiverResponse = {
    new BindReceiverResponse(commandStatus, Some(sequenceNumber), systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindReceiverResponse = {
    new BindReceiverResponse(commandStatus, None, systemId, Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
      interfaceVersion.asByte)))
  }


  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindReceiverResponse = {
    new BindReceiverResponse(commandStatus, Some(sequenceNumber), systemId,
      Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
        interfaceVersion.asByte)))
  }

  protected[this] def makePduFrom(header: Header,
                                  body: BindReceiverResponse.BindResponseBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): BindReceiverResponse = BindReceiverResponse(header
    .commandStatus,
    header
      .sequenceNumber,
    body.systemId,
    tlvParameters,
    Some(rawPdu))
}
