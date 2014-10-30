package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 14.08.13
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
abstract class PduResponse(commandId: CommandId,
                           commandStatus: Int,
                           seqNo: Option[Int],
                           tlvParameters: Vector[Tlv],
                           rawPdu: Option[Array[Byte]]) extends Pdu(commandId,
  commandStatus,
  seqNo,
  tlvParameters,
  rawPdu)
