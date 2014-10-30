package io.garuda.codec

import com.typesafe.scalalogging.slf4j.Logging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.slf4j.MDC

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 12.10.13
 * Time: 23:08
 * To change this template use File | Settings | File Templates.
 */
class SmppPduFrameDecoder(smppPduDecoder: SmppPduDecoder) extends ByteToMessageDecoder with Logging {

  @throws(classOf[Exception])
  def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[Object]): Unit = {
    logger.debug(s"DECODE:  ${in} ...")
    // First, let's record where we currently are. If there are not enough bytes in the buffer yet to decode a
    // complete PDU we will need to reset the reader index.
    in.markReaderIndex()
    val decodedPduOpt = smppPduDecoder.decodeFrom(in)
    if (decodedPduOpt.isDefined) {
      val decodedPdu = decodedPduOpt.get
      // Store decoded PDU's ID in MDC for logging purposes
      MDC.put(PduIdContextKey, decodedPdu.id.toString)
      logger.info(s"DECODED: ${in} -> ${decodedPdu}")
      out.add(decodedPduOpt.get)
    } else {
      logger.debug(s"DECODE:  Need more packets to decode SMPP PDU")
      in.resetReaderIndex()
    }
  }
}

object SmppPduFrameDecoder {

  val Name = "smpp-frame-decoder"

  def apply(smppPduDecoder: SmppPduDecoder): SmppPduFrameDecoder = new SmppPduFrameDecoder(smppPduDecoder)
}
