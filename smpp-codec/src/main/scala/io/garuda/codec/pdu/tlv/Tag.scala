package io.garuda.codec.pdu.tlv

import io.garuda.codec.pdu.{Decoder, Encodeable}
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.08.13
 * Time: 15:49
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class Tag(tag: Short) extends Encodeable {

  def lengthInBytes: Int = 2

  def encodeInto(channelBuffer: ByteBuf): Unit = {
    channelBuffer.writeShort(tag)
  }
}

object Tag extends Decoder[Tag] {

  case object PayloadType extends Tag(0x0019)

  case object PrivacyIndicator extends Tag(0x0201)

  case object UserMessageReference extends Tag(0x0204)

  case object UserResponseCode extends Tag(0x0205)

  case object SourcePort extends Tag(0x020A)

  case object DestinationPort extends Tag(0x020B)

  case object SarMsgRefNum extends Tag(0x020C)

  case object LanguageIndicator extends Tag(0x020D)

  case object SarTotalSegments extends Tag(0x020E)

  case object SarSegmentSeqnum extends Tag(0x020F)

  case object SourceSubaddress extends Tag(0x0202)

  case object DestSubaddress extends Tag(0x0203)

  case object CallbackNum extends Tag(0x0381)

  case object MessagePayload extends Tag(0x0424)

  // SC Interface Version
  case object ScInterfaceVersion extends Tag(0x0210)

  // Display Time
  case object DisplayTime extends Tag(0x1201)

  // Validity Information
  case object MsValidity extends Tag(0x1204)

  // DPF Result
  case object DpfResult extends Tag(0x0420)

  // Set DPF
  case object SetDpf extends Tag(0x0421)

  // MS Availability Status
  case object MsAvailStatus extends Tag(0x0422)

  // Network Error Code
  case object NetworkErrorCode extends Tag(0x0423)

  // Delivery Failure Reason
  case object DeliveryFailureReason extends Tag(0x0425)

  // More Messages to Follow
  case object MoreMsgsToFollow extends Tag(0x0426)

  // Message State
  case object MsgState extends Tag(0x0427)

  // Callback Number Presentation  Indicator
  case object CallbackNumPresInd extends Tag(0x0302)

  // Callback Number Alphanumeric Tag
  case object CallbackNumAtag extends Tag(0x0303)

  // Number of messages in Mailbox
  case object NumMsgs extends Tag(0x0304)

  // SMS Received Alert
  case object SmsSignal extends Tag(0x1203)

  // Message Delivery Alert
  case object AlertOnMsgDelivery extends Tag(0x130C)

  // ITS Reply Type
  case object ItsReplyType extends Tag(0x1380)

  // ITS Session Info
  case object ItsSessionInfo extends Tag(0x1383)

  // USSD Service Op
  case object UssdServiceOp extends Tag(0x0501)

  // Originating MSC Address
  case object OrigMscAddr extends Tag(0x8081.toShort)

  // Destination MSC Address
  case object DestMscAddr extends Tag(0x8082.toShort)

  // Destination Address Subunit
  case object DestAddrSubunit extends Tag(0x0005)

  // Destination Network Type
  case object DestNetworkType extends Tag(0x0006)

  // Destination Bearer Type
  case object DestBearerType extends Tag(0x0007)

  // Destination Telematics ID
  case object DestTeleId extends Tag(0x0008)

  // Source Address Subunit
  case object SourceAddrSubunit extends Tag(0x000D)

  // Source Network Type
  case object SourceNetworkType extends Tag(0x000E)

  // Source Bearer Type
  case object SourceBearerType extends Tag(0x000F)

  // Source Telematics ID
  case object SourceTeleId extends Tag(0x0010)

  // QOS Time to Live
  case object QosTimeToLive extends Tag(0x0017)

  // Additional Status Info Text
  case object AddStatusInfo extends Tag(0x001D)

  // Receipted Message ID
  case object ReceiptedMsgId extends Tag(0x001E)

  case class Custom(tag: Short) extends Tag(tag)

  def apply(tag: Int): Tag = tag match {
    case 0x0019 => PayloadType
    case 0x0201 => PrivacyIndicator
    case 0x0204 => UserMessageReference
    case 0x0205 => UserResponseCode
    case 0x020A => SourcePort
    case 0x020B => DestinationPort
    case 0x020C => SarMsgRefNum
    case 0x020D => LanguageIndicator
    case 0x020E => SarTotalSegments
    case 0x020F => SarSegmentSeqnum
    case 0x0202 => SourceSubaddress
    case 0x0203 => DestSubaddress
    case 0x0381 => CallbackNum
    case 0x0424 => MessagePayload
    // SC Interface Version
    case 0x0210 => ScInterfaceVersion
    // Display Time
    case 0x1201 => DisplayTime
    // Validity Information
    case 0x1204 => MsValidity
    // DPF Result
    case 0x0420 => DpfResult
    // Set DPF
    case 0x0421 => SetDpf
    // MS Availability Status
    case 0x0422 => MsAvailStatus
    // Network Error Code
    case 0x0423 => NetworkErrorCode
    // Delivery Failure Reason
    case 0x0425 => DeliveryFailureReason
    // More Messages to Follow
    case 0x0426 => MoreMsgsToFollow
    // Message State
    case 0x0427 => MsgState
    // Callback Number Presentation  Indicator
    case 0x0302 => CallbackNumPresInd
    // Callback Number Alphanumeric Tag
    case 0x0303 => CallbackNumAtag
    // Number of messages in Mailbox
    case 0x0304 => NumMsgs
    // SMS Received Alert
    case 0x1203 => SmsSignal
    // Message Delivery Alert
    case 0x130C => AlertOnMsgDelivery
    // ITS Reply Type
    case 0x1380 => ItsReplyType
    // ITS Session Info
    case 0x1383 => ItsSessionInfo
    // USSD Service Op
    case 0x0501 => UssdServiceOp
    case 0x8081 => OrigMscAddr
    case 0x8082 => DestMscAddr
    // Destination Address Subunit
    case 0x0005 => DestAddrSubunit
    // Destination Network Type
    case 0x0006 => DestNetworkType
    // Destination Bearer Type
    case 0x0007 => DestBearerType
    // Destination Telematics ID
    case 0x0008 => DestTeleId
    // Source Address Subunit
    case 0x000D => SourceAddrSubunit
    // Source Network Type
    case 0x000E => SourceNetworkType
    // Source Bearer Type
    case 0x000F => SourceBearerType
    // Source Telematics ID
    case 0x0010 => SourceTeleId
    // QOS Time to Live
    case 0x0017 => QosTimeToLive
    // Additional Status Info Text
    case 0x001D => AddStatusInfo
    // Receipted Message ID
    case 0x001E => ReceiptedMsgId
    case _ => Custom(tag.toShort)
  }

  def decodeFrom(channelBuffer: ByteBuf): Tag = {
    require(channelBuffer.readableBytes() >= 2, "Not enough readable bytes (" + channelBuffer.readableBytes() + ") to" +
      " decode Tag in supplied ChannelBuffer")
    val tag = channelBuffer.readUnsignedShort()
    Tag(tag)
  }
}
