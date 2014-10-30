package io.garuda.codec

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.Pdu
import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 13.10.13
 * Time: 00:29
 * To change this template use File | Settings | File Templates.
 */
class SmppPduOneToOneEncoder(smppPduEncoder: SmppPduEncoder) extends MessageToByteEncoder[Pdu] with Logging {

  @throws(classOf[Exception])
  def encode(ctx: ChannelHandlerContext, msg: Pdu, out: ByteBuf): Unit = {
    require(msg.isInstanceOf[Pdu], "Not an SMPP PDU: " + msg)
    logger.debug(s"ENCODE:  ${msg} ...")
    val smppPdu = msg.asInstanceOf[Pdu]
    smppPduEncoder.encodeInto(smppPdu, out)
    logger.info(s"ENCODED: ${smppPdu} -> [hex: ${ByteBufUtil.hexDump(out)}]")
  }
}

object SmppPduOneToOneEncoder {

  val Name = "smpp-one-to-one-encoder"

  def apply(smppPduEncoder: SmppPduEncoder): SmppPduOneToOneEncoder = new SmppPduOneToOneEncoder(smppPduEncoder)
}
