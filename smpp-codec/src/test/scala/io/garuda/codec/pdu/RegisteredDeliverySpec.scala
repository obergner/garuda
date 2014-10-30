package io.garuda.codec.pdu

import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 04.10.13
 * Time: 14:06
 * To change this template use File | Settings | File Templates.
 */
class RegisteredDeliverySpec extends Specification {

  "MCDeliveryReceipt as a factory for MCDeliveryReceipts given a Byte" should {

    "correctly recognize NoMCDeliveryReceiptRequested in an registered delivery with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.NoMCDeliveryReceiptRequested
    }

    "correctly recognize NoMCDeliveryReceiptRequested in an registered delivery with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000100", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.NoMCDeliveryReceiptRequested
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure in an registered delivery with no other" +
      " bits set" in {
      val esmClass: Byte = Integer.parseInt("00000001", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure in an registered delivery with some " +
      "other bits set" in {
      val esmClass: Byte = Integer.parseInt("10001001", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliveryFailure in an registered delivery with no other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("00000010", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliveryFailure
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliveryFailure in an registered delivery with some other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("01010010", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliveryFailure
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliverySuccess in an registered delivery with no other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("00000011", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccess
    }

    "correctly recognize MCDeliveryReceiptRequestedOnDeliverySuccess in an registered delivery with some other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("01000111", 2).toByte
      val decodedMCDeliveryReceipt = MCDeliveryReceipt(esmClass)

      decodedMCDeliveryReceipt mustEqual MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccess
    }
  }

  "MCDeliveryReceipt as an encoder for MCDeliveryReceipts" should {

    "correctly encode NoMCDeliveryReceiptRequested" in {
      val encodingTarget: Byte = Integer.parseInt("00000010", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00000000", 2).toByte

      val actualEncoding: Byte = MCDeliveryReceipt.NoMCDeliveryReceiptRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure" in {
      val encodingTarget: Byte = Integer.parseInt("10100001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("10100001", 2).toByte

      val actualEncoding: Byte =
        MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode MCDeliveryReceiptRequestedOnDeliveryFailure" in {
      val encodingTarget: Byte = Integer.parseInt("00101000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00101010", 2).toByte

      val actualEncoding: Byte =
        MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliveryFailure.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode MCDeliveryReceiptRequestedOnDeliverySuccess" in {
      val encodingTarget: Byte = Integer.parseInt("00010001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00010011", 2).toByte

      val actualEncoding: Byte =
        MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccess.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "SMEOriginatedAcknowledgement as a factory for SMEOriginatedAcknowledgements given a Byte" should {

    "correctly recognize NoRecipientSMEAcknowledgementRequested in an registered delivery with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested
    }

    "correctly recognize NoRecipientSMEAcknowledgementRequested in an registered delivery with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000001", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested
    }

    "correctly recognize SMEDeliveryAcknowledgementRequested in an registered delivery with no other" +
      " bits set" in {
      val esmClass: Byte = Integer.parseInt("00000100", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.SMEDeliveryAcknowledgementRequested
    }

    "correctly recognize SMEDeliveryAcknowledgementRequested in an registered delivery with some " +
      "other bits set" in {
      val esmClass: Byte = Integer.parseInt("10000101", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.SMEDeliveryAcknowledgementRequested
    }

    "correctly recognize SMEUserAcknowledgementRequested in an registered delivery with no other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("00001000", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.SMEUserAcknowledgementRequested
    }

    "correctly recognize SMEUserAcknowledgementRequested in an registered delivery with some other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("01001010", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement.SMEUserAcknowledgementRequested
    }

    "correctly recognize SMEDeliveryAndUserAcknowledgementRequested in an registered delivery with no other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("00001100", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement
        .SMEDeliveryAndUserAcknowledgementRequested
    }

    "correctly recognize SMEDeliveryAndUserAcknowledgementRequested in an registered delivery with some other bits " +
      "set" in {
      val esmClass: Byte = Integer.parseInt("01001111", 2).toByte
      val decodedSMEOriginatedAcknowledgement = SMEOriginatedAcknowledgement(esmClass)

      decodedSMEOriginatedAcknowledgement mustEqual SMEOriginatedAcknowledgement
        .SMEDeliveryAndUserAcknowledgementRequested
    }
  }

  "SMEOriginatedAcknowledgement as an encoder for SMEOriginatedAcknowledgements" should {

    "correctly encode NoRecipientSMEAcknowledgementRequested" in {
      val encodingTarget: Byte = Integer.parseInt("00000100", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00000000", 2).toByte

      val actualEncoding: Byte =
        SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SMEDeliveryAcknowledgementRequested" in {
      val encodingTarget: Byte = Integer.parseInt("10100001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("10100101", 2).toByte

      val actualEncoding: Byte =
        SMEOriginatedAcknowledgement.SMEDeliveryAcknowledgementRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SMEUserAcknowledgementRequested" in {
      val encodingTarget: Byte = Integer.parseInt("00100000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00101000", 2).toByte

      val actualEncoding: Byte =
        SMEOriginatedAcknowledgement.SMEUserAcknowledgementRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SMEDeliveryAndUserAcknowledgementRequested" in {
      val encodingTarget: Byte = Integer.parseInt("00010001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00011101", 2).toByte

      val actualEncoding: Byte =
        SMEOriginatedAcknowledgement.SMEDeliveryAndUserAcknowledgementRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "IntermediateNotification as a factory for IntermediateNotifications given a Byte" should {

    "correctly recognize NoIntermediateNotificationRequested in an registered delivery with no other bits set" in {
      val esmClass: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedIntermediateNotification = IntermediateNotification(esmClass)

      decodedIntermediateNotification mustEqual IntermediateNotification.NoIntermediateNotificationRequested
    }

    "correctly recognize NoIntermediateNotificationRequested in an registered delivery with some other bits set" in {
      val esmClass: Byte = Integer.parseInt("01000001", 2).toByte
      val decodedIntermediateNotification = IntermediateNotification(esmClass)

      decodedIntermediateNotification mustEqual IntermediateNotification.NoIntermediateNotificationRequested
    }

    "correctly recognize IntermediateNotificationRequested in an registered delivery with no other" +
      " bits set" in {
      val esmClass: Byte = Integer.parseInt("00010000", 2).toByte
      val decodedIntermediateNotification = IntermediateNotification(esmClass)

      decodedIntermediateNotification mustEqual IntermediateNotification.IntermediateNotificationRequested
    }

    "correctly recognize IntermediateNotificationRequested in an registered delivery with some " +
      "other bits set" in {
      val esmClass: Byte = Integer.parseInt("10010101", 2).toByte
      val decodedIntermediateNotification = IntermediateNotification(esmClass)

      decodedIntermediateNotification mustEqual IntermediateNotification.IntermediateNotificationRequested
    }
  }

  "IntermediateNotification as an encoder for IntermediateNotifications" should {

    "correctly encode NoIntermediateNotificationRequested" in {
      val encodingTarget: Byte = Integer.parseInt("00010100", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00000100", 2).toByte

      val actualEncoding: Byte =
        IntermediateNotification.NoIntermediateNotificationRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode IntermediateNotificationRequested" in {
      val encodingTarget: Byte = Integer.parseInt("10100001", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("10110001", 2).toByte

      val actualEncoding: Byte =
        IntermediateNotification.IntermediateNotificationRequested.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }
}
