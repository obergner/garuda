package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 03.10.13
 * Time: 21:25
 * To change this template use File | Settings | File Templates.
 */
case class RegisteredDelivery(value: Byte) {

  def asByte: Byte = value

  def mcDeliveryReceipt: MCDeliveryReceipt = MCDeliveryReceipt(value)

  def smeOriginatedAcknowledgement: SMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(value)

  def intermediateNotification: IntermediateNotification = IntermediateNotification(value)
}

object RegisteredDelivery {

  val Default: RegisteredDelivery = RegisteredDelivery(MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure,
    SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested,
    IntermediateNotification.NoIntermediateNotificationRequested)

  def apply(mcDeliveryReceipt: MCDeliveryReceipt, smeOriginatedAcknowledgement: SMEOriginatedAcknowledgement,
            intermediateNotification: IntermediateNotification): RegisteredDelivery = {
    var value: Byte = 0x00
    value = mcDeliveryReceipt.encodeInto(value)
    value = smeOriginatedAcknowledgement.encodeInto(value)
    value = intermediateNotification.encodeInto(value)
    RegisteredDelivery(value)
  }
}

sealed abstract class MCDeliveryReceipt(bitmask: Byte) {

  def encodeInto(registeredDelivery: Byte): Byte = {
    var result: Byte = registeredDelivery
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

object MCDeliveryReceipt {

  case object NoMCDeliveryReceiptRequested extends MCDeliveryReceipt(0x00)

  case object MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure extends MCDeliveryReceipt(0x01)

  case object MCDeliveryReceiptRequestedOnDeliveryFailure extends MCDeliveryReceipt(0x02)

  case object MCDeliveryReceiptRequestedOnDeliverySuccess extends MCDeliveryReceipt(0x03)

  def apply(registeredDelivery: Byte): MCDeliveryReceipt = {
    if ((registeredDelivery & 0x03) == 0x00) {
      NoMCDeliveryReceiptRequested
    } else if ((registeredDelivery & 0x03) == 0x01) {
      MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure
    } else if ((registeredDelivery & 0x03) == 0x02) {
      MCDeliveryReceiptRequestedOnDeliveryFailure
    } else {
      MCDeliveryReceiptRequestedOnDeliverySuccess
    }
  }
}

sealed abstract class SMEOriginatedAcknowledgement(bitmask: Byte) {

  def encodeInto(registeredDelivery: Byte): Byte = {
    var result: Byte = registeredDelivery
    for (bitPos <- 2 to 3) {
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

object SMEOriginatedAcknowledgement {

  case object NoRecipientSMEAcknowledgementRequested extends SMEOriginatedAcknowledgement(0x00)

  case object SMEDeliveryAcknowledgementRequested extends SMEOriginatedAcknowledgement(0x04)

  case object SMEUserAcknowledgementRequested extends SMEOriginatedAcknowledgement(0x08)

  case object SMEDeliveryAndUserAcknowledgementRequested extends SMEOriginatedAcknowledgement(0x0c)

  def apply(registeredDelivery: Byte): SMEOriginatedAcknowledgement = {
    if ((registeredDelivery & 0x0c) == 0x00) {
      NoRecipientSMEAcknowledgementRequested
    } else if ((registeredDelivery & 0x0c) == 0x04) {
      SMEDeliveryAcknowledgementRequested
    } else if ((registeredDelivery & 0x0c) == 0x08) {
      SMEUserAcknowledgementRequested
    } else {
      SMEDeliveryAndUserAcknowledgementRequested
    }
  }
}

sealed abstract class IntermediateNotification(bitmask: Byte) {

  def encodeInto(registeredDelivery: Byte): Byte = {
    var result: Byte = registeredDelivery
    val bitPos = 4
    val bitAtPos: Byte = (1 << bitPos).toByte
    val setBitAtPos: Boolean = (bitmask & bitAtPos) == bitAtPos
    if (setBitAtPos) {
      result = (result | bitAtPos).toByte
    } else {
      result = (result & ~bitAtPos).toByte
    }
    result
  }
}

object IntermediateNotification {

  case object NoIntermediateNotificationRequested extends IntermediateNotification(0x00)

  case object IntermediateNotificationRequested extends IntermediateNotification(0x10)

  def apply(registeredDelivery: Byte): IntermediateNotification = {
    if ((registeredDelivery & 0x10) == 0x00) {
      NoIntermediateNotificationRequested
    } else {
      IntermediateNotificationRequested
    }
  }
}
