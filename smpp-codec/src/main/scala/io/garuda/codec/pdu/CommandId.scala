package io.garuda.codec.pdu

import io.garuda.codec.InvalidCommandIdException
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 11.08.13
 * Time: 20:41
 * To change this template use File | Settings | File Templates.
 */
private[codec] object CommandIdConstants {

  val BindTransmitter: Int = 0x00000002

  val BindTransmitterResp: Int = 0x80000002

  val BindReceiver: Int = 0x00000001

  val BindReceiverResp: Int = 0x80000001

  val BindTransceiver: Int = 0x00000009

  val BindTransceiverResp: Int = 0x80000009

  val EnquireLink: Int = 0x00000015

  val EnquireLinkResp: Int = 0x80000015

  val Unbind: Int = 0x00000006

  val UnbindResp: Int = 0x80000006

  val GenericNack: Int = 0x80000000

  val SubmitSm: Int = 0x00000004

  val SubmitSmResp: Int = 0x80000004
}

sealed abstract class CommandId(id: Int) {

  def asInt: Int = id
}

/**
 * A factory/parser for CommandIds.
 */
object CommandId {

  // Session management requests/responses
  case object bind_transmitter extends CommandId(CommandIdConstants.BindTransmitter)

  case object bind_transmitter_resp extends CommandId(CommandIdConstants.BindTransmitterResp)

  case object bind_receiver extends CommandId(CommandIdConstants.BindReceiver)

  case object bind_receiver_resp extends CommandId(CommandIdConstants.BindReceiverResp)

  case object bind_transceiver extends CommandId(CommandIdConstants.BindTransceiver)

  case object bind_transceiver_resp extends CommandId(CommandIdConstants.BindTransceiverResp)

  case object enquire_link extends CommandId(CommandIdConstants.EnquireLink)

  case object enquire_link_resp extends CommandId(CommandIdConstants.EnquireLinkResp)

  case object unbind extends CommandId(CommandIdConstants.Unbind)

  case object unbind_resp extends CommandId(CommandIdConstants.UnbindResp)

  case object generic_nack extends CommandId(CommandIdConstants.GenericNack)

  case object submit_sm extends CommandId(CommandIdConstants.SubmitSm)

  case object submit_sm_resp extends CommandId(CommandIdConstants.SubmitSmResp)

  def apply(id: Int): CommandId = id match {
    case CommandIdConstants.BindTransmitter => bind_transmitter
    case CommandIdConstants.BindTransmitterResp => bind_transmitter_resp
    case CommandIdConstants.BindReceiver => bind_receiver
    case CommandIdConstants.BindReceiverResp => bind_receiver_resp
    case CommandIdConstants.BindTransceiver => bind_transceiver
    case CommandIdConstants.BindTransceiverResp => bind_transceiver_resp
    case CommandIdConstants.EnquireLink => enquire_link
    case CommandIdConstants.EnquireLinkResp => enquire_link_resp
    case CommandIdConstants.Unbind => unbind
    case CommandIdConstants.UnbindResp => unbind_resp
    case CommandIdConstants.GenericNack => generic_nack
    case CommandIdConstants.SubmitSm => submit_sm
    case CommandIdConstants.SubmitSmResp => submit_sm_resp
    case invalidCommandId: Int => throw new InvalidCommandIdException(invalidCommandId, "Not a valid command id: " + id)
  }

  def getFrom(wireFormat: ByteBuf): CommandId = {
    val commandIdAsInt = wireFormat.getInt(wireFormat.readerIndex())
    CommandId(commandIdAsInt)
  }
}
