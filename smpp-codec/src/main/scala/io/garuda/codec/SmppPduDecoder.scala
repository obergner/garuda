package io.garuda.codec

import io.garuda.codec.pdu._
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 12.10.13
 * Time: 22:42
 * To change this template use File | Settings | File Templates.
 */
class SmppPduDecoder {

  def decodeFrom(channelBuffer: ByteBuf): Option[Pdu] = {
    val headerOpt = Header.getFrom(channelBuffer)
    if (headerOpt.isEmpty) return None
    val header = headerOpt.get
    val pduOpt: Option[Pdu] = header.commandId match {
      case CommandId.bind_receiver => BindReceiver.decodeFrom(header, channelBuffer)
      case CommandId.bind_receiver_resp => BindReceiverResponse.decodeFrom(header, channelBuffer)
      case CommandId.bind_transceiver => BindTransceiver.decodeFrom(header, channelBuffer)
      case CommandId.bind_transceiver_resp => BindTransceiverResponse.decodeFrom(header, channelBuffer)
      case CommandId.bind_transmitter => BindTransmitter.decodeFrom(header, channelBuffer)
      case CommandId.bind_transmitter_resp => BindTransmitterResponse.decodeFrom(header, channelBuffer)
      case CommandId.enquire_link => EnquireLink.decodeFrom(header, channelBuffer)
      case CommandId.enquire_link_resp => EnquireLinkResponse.decodeFrom(header, channelBuffer)
      case CommandId.generic_nack => GenericNack.decodeFrom(header, channelBuffer)
      case CommandId.submit_sm => SubmitSm.decodeFrom(header, channelBuffer)
      case CommandId.submit_sm_resp => SubmitSmResponse.decodeFrom(header, channelBuffer)
      case CommandId.unbind => Unbind.decodeFrom(header, channelBuffer)
      case CommandId.unbind_resp => UnbindResponse.decodeFrom(header, channelBuffer)
      // We should never come here since Header.getFrom(channelBuffer) should already throw an InvalidCommandIdException
    }
    pduOpt
  }
}
