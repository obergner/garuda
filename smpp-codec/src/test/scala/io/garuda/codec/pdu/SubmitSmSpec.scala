package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.garuda.codec.util.{ChannelBufferHelpers, HexHelpers}
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 08.10.13
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */
class SubmitSmSpec extends Specification {

  "SubmitSm (class) as an encoder for SubmitSm" should {

    "correctly encode a SubmitSm PDU without TLVs" in {
      val objectUnderTest = SubmitSm(Some(20456),
        "",
        Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
        Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
        0x00,
        0x00,
        0x00,
        "",
        "",
        0x01,
        0x00,
        0x00,
        0x00,
        HexHelpers.hexStringToByteArray("4024232125262f3a"))

      val expectedEncoding =
        ChannelBufferHelpers.createBufferFromHexString("00000039000000040000000000004FE8000101343034303400010134" +
          "3439353133363139323000000000000001000000084024232125262F3A")
      val actualEncoding = objectUnderTest.encode()

      actualEncoding mustEqual expectedEncoding
    }
  }

  "SubmitSm (class) providing strongly typed accessor methods for most properties" should {

    "correctly return default values" in {
      val objectUnderTest = SubmitSm(Some(20456),
        "",
        Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
        Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
        0x00,
        0x00,
        0x00,
        "",
        "",
        0x01,
        0x00,
        0x00,
        0x00,
        HexHelpers.hexStringToByteArray("4024232125262f3a"))

      objectUnderTest.esmClass mustEqual EsmClass(MessagingMode.Default, MessageType.Default, GsmFeature.None)
      objectUnderTest.serviceType mustEqual ServiceType.Default
      objectUnderTest.priorityFlag mustEqual PriorityFlag.Zero
      objectUnderTest.registeredDelivery mustEqual
        RegisteredDelivery(MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure,
          SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested,
          IntermediateNotification.NoIntermediateNotificationRequested)
      objectUnderTest.scheduleDeliveryTime mustEqual None
      objectUnderTest.validityPeriod mustEqual None
      objectUnderTest.dataCodingScheme mustEqual DataCoding.MCSpecificDefault
    }
  }

  "SubmitSm (object) as a decoder for SubmitSm" should {

    "correctly decode a SubmitSm without TLVs" in {
      val buf =
        ChannelBufferHelpers.createBufferFromHexString("00000039000000040000000000004FE800010134303430340001013434393" +
          "53133363139323000000000000001000000084024232125262F3A")
      val header = Header.getFrom(buf)

      val decoded = SubmitSm.decodeFrom(header.get, buf).get

      decoded.sequenceNumber mustEqual Some(20456)
      decoded.rawServiceType mustEqual ""
      decoded.sourceAddress mustEqual Address(Ton.International, Npi.IsdnNumberingPlan, "40404")
      decoded.destinationAddress mustEqual Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920")
      decoded.rawEsmClass mustEqual 0x00
      decoded.rawDataCodingScheme mustEqual 0x00
      decoded.rawPriorityFlag mustEqual 0x00
      decoded.rawRegisteredDelivery mustEqual 0x01
      decoded.rawReplaceIfPresentFlag mustEqual 0x00
      decoded.rawScheduleDeliveryTime mustEqual ""
      decoded.rawValidityPeriod mustEqual ""
      decoded.smDefaultMsgId mustEqual 0x00
      decoded.shortMessage mustEqual HexHelpers.hexStringToByteArray("4024232125262f3a")
      decoded.tlvParameters mustEqual Vector.empty[Tlv]
    }
  }
}
