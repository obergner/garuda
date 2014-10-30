package io.garuda.codec.pdu

import io.garuda.codec.{ErrorCode, TerminatingNullByteNotFoundException}
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 12.10.13
 * Time: 16:32
 * To change this template use File | Settings | File Templates.
 */
abstract class DecodingSupport {

  protected[this] def readSystemIdFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("systemId", ErrorCode.ESME_RINVSYSID, channelBuffer)
  }

  protected[this] def readSystemTypeFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("systemType", ErrorCode.ESME_RINVSYSTYP, channelBuffer)
  }

  protected[this] def readPasswordFrom(channelBuffer: ByteBuf): String = {
    // TODO: Is ErrorCode.ESME_RINVPASWD really appropriate?
    readNullTerminatedFieldFrom("password", ErrorCode.ESME_RINVPASWD, channelBuffer)
  }

  protected[this] def readDestinationAddressFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("destinationAddress", ErrorCode.ESME_RINVDSTADR, channelBuffer)
  }

  protected[this] def readAddressRangeFrom(channelBuffer: ByteBuf): String = {
    // TODO: Is ESME_RBINDFAIL really appropriate?
    readNullTerminatedFieldFrom("addressRange", ErrorCode.ESME_RBINDFAIL, channelBuffer)
  }

  protected[this] def readSourceAddressFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("sourceAddress", ErrorCode.ESME_RINVSRCADR, channelBuffer)
  }

  protected[this] def readServiceTypeFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("serviceType", ErrorCode.ESME_RINVSERTYP, channelBuffer)
  }

  protected[this] def readScheduleDeliveryTimeFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("scheduleDeliveryTime", ErrorCode.ESME_RINVSCHED, channelBuffer)
  }

  protected[this] def readValidityPeriodFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("validityPeriod", ErrorCode.ESME_RINVEXPIRY, channelBuffer)
  }

  protected[this] def readMessageIdFrom(channelBuffer: ByteBuf): String = {
    readNullTerminatedFieldFrom("messageId", ErrorCode.ESME_RINVMSGID, channelBuffer)
  }

  private[this] def readNullTerminatedFieldFrom(fieldName: String, errorCodeToReport: ErrorCode, channelBuffer: ByteBuf): String = {
    val field = readNullTerminatedStringFrom(channelBuffer)
    field match {
      case Some(fld) => fld
      case None => throw new TerminatingNullByteNotFoundException(errorCodeToReport, "Invalid " + fieldName)
    }
  }

  private[this] def readNullTerminatedStringFrom(channelBuffer: ByteBuf): Option[String] = {
    if (channelBuffer.readableBytes() < 1) None
    val readableBytes = channelBuffer.readableBytes()

    val offset = channelBuffer.readerIndex()
    val nullByteIndex = channelBuffer.indexOf(offset, offset + readableBytes, 0x00.toByte)
    if (nullByteIndex < 0) {
      return None
    }

    val stringBytes = new Array[Byte](nullByteIndex - offset)
    channelBuffer.readBytes(stringBytes)
    val resultString = new String(stringBytes, "ISO-8859-1")

    // Skip null byte
    channelBuffer.skipBytes(1)

    Some(resultString)
  }
}
