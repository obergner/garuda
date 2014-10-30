package io.garuda.codec.pdu

import io.garuda.codec.{ErrorCode, InvalidPduFieldValueException}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 28.09.13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
case class EsmClass(value: Byte) {

  def asByte: Byte = value

  def messagingMode: MessagingMode = MessagingMode(value)

  def messageType: MessageType = MessageType(value)

  def gsmFeature: GsmFeature = GsmFeature(value)
}

object EsmClass {

  def apply(messagingMode: MessagingMode, messageType: MessageType, gsmFeature: GsmFeature): EsmClass = {
    var esmClass: Byte = 0x00
    esmClass = messagingMode.encodeInto(esmClass)
    esmClass = messageType.encodeInto(esmClass)
    esmClass = gsmFeature.encodeInto(esmClass)
    EsmClass(esmClass)
  }
}

sealed abstract class MessagingMode(bitmask: Byte) {

  def encodeInto(esmClass: Byte): Byte = {
    var result: Byte = esmClass
    for (bitPos <- 0 to 1) {
      val bitAtPos: Byte = (1 << bitPos).toByte
      val setBitAtPos: Boolean = (bitmask & bitAtPos) == bitAtPos
      if (setBitAtPos) {
        result = (result | bitAtPos).toByte
      } else {
        result = (result & ~bitAtPos).toByte
      }
    }
    result
  }
}

object MessagingMode {

  case object Default extends MessagingMode(0x00)

  case object Datagram extends MessagingMode(0x01)

  case object Forward extends MessagingMode(0x02)

  case object StoreAndForward extends MessagingMode(0x03)

  def apply(esmClass: Byte): MessagingMode = {
    if ((esmClass & 0x03) == 0x00) {
      Default
    } else if ((esmClass & 0x03) == 0x01) {
      Datagram
    } else if ((esmClass & 0x03) == 0x02) {
      Forward
    } else {
      StoreAndForward
    }
  }
}

sealed abstract class MessageType(bitmask: Byte) {

  def encodeInto(esmClass: Byte): Byte = {
    var result: Byte = esmClass
    for (bitPos <- 2 to 5) {
      val bitAtPos: Byte = (1 << bitPos).toByte
      val setBitAtPos: Boolean = (bitmask & bitAtPos) == bitAtPos
      if (setBitAtPos) {
        result = (result | bitAtPos).toByte
      } else {
        result = (result & ~bitAtPos).toByte
      }
    }
    result
  }
}

object MessageType {

  case object Default extends MessageType(0x00)

  case object MCDeliveryReceipt extends MessageType(0x04)

  case object IntermediateDeliveryNotification extends MessageType(0x20)

  case object DeliveryAcknowledgement extends MessageType(0x08)

  case object UserAcknowledgement extends MessageType(0x10)

  case object ConversationAbort extends MessageType(0x18)

  def apply(esmClass: Byte): MessageType = {
    if ((esmClass & 0x3c) == 0x00) {
      Default
    } else if ((esmClass & 0x3c) == 0x04) {
      MCDeliveryReceipt
    } else if ((esmClass & 0x3c) == 0x20) {
      IntermediateDeliveryNotification
    } else if ((esmClass & 0x3c) == 0x08) {
      DeliveryAcknowledgement
    } else if ((esmClass & 0x3c) == 0x10) {
      UserAcknowledgement
    } else if ((esmClass & 0x3c) == 0x18) {
      ConversationAbort
    } else {
      throw new
          InvalidPduFieldValueException(ErrorCode.ESME_RINVESMCLASS, "Supplied ESM class does not contain a valid MessageType: " + esmClass)
    }
  }
}

sealed abstract class GsmFeature(bitmask: Byte) {

  def encodeInto(esmClass: Byte): Byte = {
    var result: Byte = esmClass
    for (bitPos <- 6 to 7) {
      val bitAtPos: Byte = (1 << bitPos).toByte
      val setBitAtPos: Boolean = (bitmask & bitAtPos) == bitAtPos
      if (setBitAtPos) {
        result = (result | bitAtPos).toByte
      } else {
        result = (result & ~bitAtPos).toByte
      }
    }
    result
  }
}

object GsmFeature {

  case object None extends GsmFeature(0x00)

  case object UdhIndicator extends GsmFeature(0x40)

  case object SetReplyPath extends GsmFeature(0x80.toByte)

  case object SetUdhIndicatorAndReplyPath extends GsmFeature(0xc0.toByte)

  def apply(esmClass: Byte): GsmFeature = {
    if ((esmClass & 0xc0) == 0x00) {
      None
    } else if ((esmClass & 0xc0) == 0x40) {
      UdhIndicator
    } else if ((esmClass & 0xc0) == 0x80) {
      SetReplyPath
    } else {
      SetUdhIndicatorAndReplyPath
    }
  }
}
