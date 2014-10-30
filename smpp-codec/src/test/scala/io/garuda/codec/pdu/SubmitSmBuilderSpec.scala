package io.garuda.codec.pdu

import io.garuda.codec.util.HexHelpers
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 08.10.13
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */
class SubmitSmBuilderSpec extends Specification {

  "SubmitSmBuilder" should {

    "bail out if no seqNo was set" in {
      val objectUnderTest = SubmitSm.newBuilder()
      objectUnderTest
        .sourceAddress(Address(Ton.International, Npi.IsdnNumberingPlan, "40404"))
        .destinationAddress(Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"))
        .build() must throwAn[IllegalArgumentException]
    }

    "bail out if no destinationAddress was set" in {
      val objectUnderTest = SubmitSm.newBuilder()
      objectUnderTest
        .seqNo(2)
        .sourceAddress(Address(Ton.International, Npi.IsdnNumberingPlan, "40404"))
        .build() must throwAn[IllegalArgumentException]
    }

    "build a correct default SubmitSm PDU when given only required parameters" in {
      val objectUnderTest = SubmitSm.newBuilder()
      val product = objectUnderTest
        .seqNo(1)
        .sourceAddress(Address(Ton.International, Npi.IsdnNumberingPlan, "40404"))
        .destinationAddress(Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"))
        .build()

      product.esmClass mustEqual EsmClass(MessagingMode.Default, MessageType.Default, GsmFeature.None)
      product.serviceType mustEqual ServiceType.Default
      product.priorityFlag mustEqual PriorityFlag.Zero
      product.registeredDelivery mustEqual
        RegisteredDelivery(MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliverySuccessOrFailure,
          SMEOriginatedAcknowledgement.NoRecipientSMEAcknowledgementRequested,
          IntermediateNotification.NoIntermediateNotificationRequested)
      product.scheduleDeliveryTime mustEqual None
      product.validityPeriod mustEqual None
      product.dataCodingScheme mustEqual DataCoding.MCSpecificDefault
    }

    "build a correct SubmitSm PDU when given all parameters except fo TLVs" in {
      val expectedSeqNo = 5567
      val expectedSourceAddress = Address(Ton.International, Npi.IsdnNumberingPlan, "40404")
      val expectedDestinationAddress = Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920")
      val expectedEsmClass = EsmClass(MessagingMode.Forward, MessageType.ConversationAbort, GsmFeature.SetUdhIndicatorAndReplyPath)
      val expectedServiceType = ServiceType.GUTS
      val expectedPriorityFlag = PriorityFlag.Three
      val expectedRegisteredDelivery = RegisteredDelivery(
        MCDeliveryReceipt.MCDeliveryReceiptRequestedOnDeliveryFailure,
        SMEOriginatedAcknowledgement.SMEDeliveryAndUserAcknowledgementRequested,
        IntermediateNotification.IntermediateNotificationRequested)
      val expectedDataCodingScheme = DataCodingScheme(0x11)
      val expectedShortMessage = HexHelpers.hexStringToByteArray("1F2A33")

      val objectUnderTest = SubmitSm.newBuilder()
      val product = objectUnderTest
        .seqNo(expectedSeqNo)
        .sourceAddress(expectedSourceAddress)
        .destinationAddress(expectedDestinationAddress)
        .esmClass(expectedEsmClass)
        .serviceType(expectedServiceType)
        .priorityFlag(expectedPriorityFlag)
        .registeredDelivery(expectedRegisteredDelivery)
        .dataCodingScheme(expectedDataCodingScheme)
        .shortMessage(expectedShortMessage)
        .build()

      product.seqNo mustEqual Some(expectedSeqNo)
      product.sourceAddress mustEqual expectedSourceAddress
      product.destinationAddress mustEqual expectedDestinationAddress
      product.esmClass mustEqual expectedEsmClass
      product.serviceType mustEqual expectedServiceType
      product.priorityFlag mustEqual expectedPriorityFlag
      product.registeredDelivery mustEqual expectedRegisteredDelivery
      product.scheduleDeliveryTime mustEqual None
      product.validityPeriod mustEqual None
      product.dataCodingScheme mustEqual expectedDataCodingScheme
      product.shortMessage mustEqual expectedShortMessage
    }
  }
}
