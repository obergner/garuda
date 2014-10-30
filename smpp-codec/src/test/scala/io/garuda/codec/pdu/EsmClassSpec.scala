package io.garuda.codec.pdu

import io.garuda.codec.InvalidPduFieldValueException
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 29.09.13
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
class EsmClassSpec extends Specification {

  "MessagingMode as a factory for messaging modes given a Byte" should {

    "correctly recognize Default in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Default
    }

    "correctly recognize Default in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000100", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Default
    }

    "correctly recognize Datagram in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000001", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Datagram
    }

    "correctly recognize Datagram in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("10001001", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Datagram
    }

    "correctly recognize Forward in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000010", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Forward
    }

    "correctly recognize Forward in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000110", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.Forward
    }

    "correctly recognize StoreAndForward in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000011", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.StoreAndForward
    }

    "correctly recognize StoreAndForward in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01010011", 2).toByte
      val decodedMessagingMode = MessagingMode(esmClass)

      decodedMessagingMode mustEqual MessagingMode.StoreAndForward
    }
  }

  "MessagingMode as an encoder for MessagingModes" should {

    "correctly encode Default" in {
      val encodingTarget: Byte = Integer.parseInt("00000010", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00000000", 2).toByte

      val actualEncoding: Byte = MessagingMode.Default.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode Datagram" in {
      val encodingTarget: Byte = Integer.parseInt("10100001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("10100001", 2).toByte

      val actualEncoding: Byte = MessagingMode.Datagram.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode Forward" in {
      val encodingTarget: Byte = Integer.parseInt("00101000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00101010", 2).toByte

      val actualEncoding: Byte = MessagingMode.Forward.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode StoreAndForward" in {
      val encodingTarget: Byte = Integer.parseInt("00010001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00010011", 2).toByte

      val actualEncoding: Byte = MessagingMode.StoreAndForward.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "MessageType as a factory for message types given a Byte" should {

    "correctly recognize Default in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.Default
    }

    "correctly recognize Default in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("11000010", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.Default
    }

    "correctly recognize MCDeliveryReceipt in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000100", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.MCDeliveryReceipt
    }

    "correctly recognize MCDeliveryReceipt in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("11000110", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.MCDeliveryReceipt
    }

    "correctly recognize IntermediateDeliveryNotification in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00100000", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.IntermediateDeliveryNotification
    }

    "correctly recognize IntermediateDeliveryNotification in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("10100011", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.IntermediateDeliveryNotification
    }

    "correctly recognize DeliveryAcknowledgement in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00001000", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.DeliveryAcknowledgement
    }

    "correctly recognize DeliveryAcknowledgement in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01001010", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.DeliveryAcknowledgement
    }

    "correctly recognize UserAcknowledgement in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00010000", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.UserAcknowledgement
    }

    "correctly recognize UserAcknowledgement in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("11010010", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.UserAcknowledgement
    }

    "correctly recognize ConversationAbort in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00011000", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.ConversationAbort
    }

    "correctly recognize ConversationAbort in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("11011001", 2).toByte
      val decodedMessageType = MessageType(esmClass)

      decodedMessageType mustEqual MessageType.ConversationAbort
    }

    "correctly recognize an illegal MessageType" in {
      val esmClass: Byte = Integer.parseInt("11001101", 2).toByte
      MessageType(esmClass) must throwA[InvalidPduFieldValueException]
    }
  }

  "MessageType as an encoder for MessageTypes" should {

    "correctly encode Default" in {
      val encodingTarget: Byte = Integer.parseInt("00001000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00000000", 2).toByte

      val actualEncoding: Byte = MessageType.Default.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode MCDeliveryReceipt" in {
      val encodingTarget: Byte = Integer.parseInt("11010011", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11000111", 2).toByte

      val actualEncoding: Byte = MessageType.MCDeliveryReceipt.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode IntermediateDeliveryNotification" in {
      val encodingTarget: Byte = Integer.parseInt("11001110", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11100010", 2).toByte

      val actualEncoding: Byte = MessageType.IntermediateDeliveryNotification.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode DeliveryAcknowledgement" in {
      val encodingTarget: Byte = Integer.parseInt("11000000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11001000", 2).toByte

      val actualEncoding: Byte = MessageType.DeliveryAcknowledgement.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode UserAcknowledgement" in {
      val encodingTarget: Byte = Integer.parseInt("00001011", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00010011", 2).toByte

      val actualEncoding: Byte = MessageType.UserAcknowledgement.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode ConversationAbort" in {
      val encodingTarget: Byte = Integer.parseInt("11000110", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11011010", 2).toByte

      val actualEncoding: Byte = MessageType.ConversationAbort.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "GsmFeature as a factory for GSM specific features given a Byte" should {

    "correctly recognize None in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.None
    }

    "correctly recognize None in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("00100100", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.None
    }

    "correctly recognize UdhIndicator in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000000", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.UdhIndicator
    }

    "correctly recognize UdhIndicator in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01001001", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.UdhIndicator
    }

    "correctly recognize SetReplyPath in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("10000000", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.SetReplyPath
    }

    "correctly recognize SetReplyPath in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("10010110", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.SetReplyPath
    }

    "correctly recognize SetUdhIndicatorAndReplyPath in an esm class with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("11000000", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.SetUdhIndicatorAndReplyPath
    }

    "correctly recognize SetUdhIndicatorAndReplyPath in an esm class with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("11010011", 2).toByte
      val decodedGsmFeature = GsmFeature(esmClass)

      decodedGsmFeature mustEqual GsmFeature.SetUdhIndicatorAndReplyPath
    }
  }

  "GsmFeature as an encoder for GsmFeatures" should {

    "correctly encode None" in {
      val encodingTarget: Byte = Integer.parseInt("10111100", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00111100", 2).toByte

      val actualEncoding: Byte = GsmFeature.None.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode UdhIndicator" in {
      val encodingTarget: Byte = Integer.parseInt("01101101", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("01101101", 2).toByte

      val actualEncoding: Byte = GsmFeature.UdhIndicator.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SetReplyPath" in {
      val encodingTarget: Byte = Integer.parseInt("01111100", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("10111100", 2).toByte

      val actualEncoding: Byte = GsmFeature.SetReplyPath.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SetUdhIndicatorAndReplyPath" in {
      val encodingTarget: Byte = Integer.parseInt("00111110", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11111110", 2).toByte

      val actualEncoding: Byte = GsmFeature.SetUdhIndicatorAndReplyPath.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "EsmClass (class) as an encoder for EsmClasses" should {

    "correctly encode a simple EsmClass" in {
      val objectUnderTest = EsmClass(MessagingMode.Default, MessageType.Default, GsmFeature.None)

      val expectedEncoding = 0x00.toByte
      val actualEncoding = objectUnderTest.asByte

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode a complex EsmClass" in {
      val objectUnderTest = EsmClass(MessagingMode.StoreAndForward, MessageType.ConversationAbort,
        GsmFeature.SetUdhIndicatorAndReplyPath)

      val expectedEncoding = 0xdb.toByte
      val actualEncoding = objectUnderTest.asByte

      actualEncoding mustEqual expectedEncoding
    }
  }

  "EsmClass (object) as a decoder for EsmClasses" should {

    "correctly decode a simple EsmClass" in {
      val decoded = EsmClass(0x00.toByte)

      decoded.messagingMode mustEqual MessagingMode.Default
      decoded.messageType mustEqual MessageType.Default
      decoded.gsmFeature mustEqual GsmFeature.None
    }

    "correctly decode a complex EsmClass" in {
      val decoded = EsmClass(0xdb.toByte)

      decoded.messagingMode mustEqual MessagingMode.StoreAndForward
      decoded.messageType mustEqual MessageType.ConversationAbort
      decoded.gsmFeature mustEqual GsmFeature.SetUdhIndicatorAndReplyPath
    }
  }
}
