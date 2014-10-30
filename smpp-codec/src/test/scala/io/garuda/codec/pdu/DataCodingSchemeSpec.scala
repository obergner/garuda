package io.garuda.codec.pdu

import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 05.10.13
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
class DataCodingSchemeSpec extends Specification {

  "IndicationSense as a parser for IndicationSenses given a Byte" should {

    "correctly recognize SetIndicationInactive in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedIndicationSense = MessageWaiting.IndicationSense(dataCodingScheme)

      decodedIndicationSense mustEqual MessageWaiting.IndicationSense.SetIndicationInactive
    }

    "correctly recognize SetIndicationInactive in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000011", 2).toByte
      val decodedIndicationSense = MessageWaiting.IndicationSense(dataCodingScheme)

      decodedIndicationSense mustEqual MessageWaiting.IndicationSense.SetIndicationInactive
    }

    "correctly recognize SetIndicationActive in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00001000", 2).toByte
      val decodedIndicationSense = MessageWaiting.IndicationSense(dataCodingScheme)

      decodedIndicationSense mustEqual MessageWaiting.IndicationSense.SetIndicationActive
    }

    "correctly recognize SetIndicationActive in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("11001011", 2).toByte
      val decodedIndicationSense = MessageWaiting.IndicationSense(dataCodingScheme)

      decodedIndicationSense mustEqual MessageWaiting.IndicationSense.SetIndicationActive
    }
  }


  "IndicationSense as a byte encoder for IndicationSenses" should {

    "correctly encode SetIndicationInactive" in {
      val encodingTarget: Byte = Integer.parseInt("11101010", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11100010", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationSense.SetIndicationInactive.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode SetIndicationActive" in {
      val encodingTarget: Byte = Integer.parseInt("00100010", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00101010", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationSense.SetIndicationActive.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "IndicationType as a parser for IndicationTypes given a Byte" should {

    "correctly recognize VoicemailMessageWaiting in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000000", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.VoicemailMessageWaiting
    }

    "correctly recognize VoicemailMessageWaiting in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("01000000", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.VoicemailMessageWaiting
    }

    "correctly recognize FaxMessageWaiting in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000001", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.FaxMessageWaiting
    }

    "correctly recognize FaxMessageWaiting in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("11001001", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.FaxMessageWaiting
    }

    "correctly recognize ElectronicMailMessageWaiting in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000010", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.ElectronicMailMessageWaiting
    }

    "correctly recognize ElectronicMailMessageWaiting in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("11001010", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.ElectronicMailMessageWaiting
    }

    "correctly recognize OtherMessageWaiting in an data coding scheme with no other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000011", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.OtherMessageWaiting
    }

    "correctly recognize OtherMessageWaiting in an data coding scheme with some other bits set" in {
      val dataCodingScheme: Byte = Integer.parseInt("11001011", 2).toByte
      val decodedIndicationType = MessageWaiting.IndicationType(dataCodingScheme)

      decodedIndicationType mustEqual MessageWaiting.IndicationType.OtherMessageWaiting
    }
  }

  "IndicationType as a byte encoder for IndicationTypes" should {

    "correctly encode VoicemailMessageWaiting" in {
      val encodingTarget: Byte = Integer.parseInt("11100010", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("11100000", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationType.VoicemailMessageWaiting.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode FaxMessageWaiting" in {
      val encodingTarget: Byte = Integer.parseInt("00100000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00100001", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationType.FaxMessageWaiting.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode ElectronicMailMessageWaiting" in {
      val encodingTarget: Byte = Integer.parseInt("00100000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00100010", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationType.ElectronicMailMessageWaiting.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode OtherMessageWaiting" in {
      val encodingTarget: Byte = Integer.parseInt("00100000", 2).toByte
      val expectedEncoding: Byte = Integer.parseInt("00100011", 2).toByte

      val actualEncoding: Byte =
        MessageWaiting.IndicationType.OtherMessageWaiting.encodeInto(encodingTarget)

      actualEncoding mustEqual expectedEncoding
    }
  }

  "MessageWaiting as a byte encoder for MessageWaitings" should {

    "correctly encode DiscardMessage with SetIndicationInactive and FaxMessageWaiting" in {
      val toEncode: MessageWaiting =
        MessageWaiting(MessageWaiting.DiscardMessage,
          MessageWaiting.IndicationSense.SetIndicationInactive,
          MessageWaiting.IndicationType.FaxMessageWaiting)

      val expectedEncoding: Byte = Integer.parseInt("11000001", 2).toByte

      val actualEncoding: Byte = toEncode.asByte

      actualEncoding mustEqual expectedEncoding
    }

    "correctly encode StoreMessage with SetIndicationActive and OtherMessageWaiting" in {
      val toEncode: MessageWaiting =
        MessageWaiting(MessageWaiting.StoreMessage,
          MessageWaiting.IndicationSense.SetIndicationActive,
          MessageWaiting.IndicationType.OtherMessageWaiting)

      val expectedEncoding: Byte = Integer.parseInt("11011011", 2).toByte

      val actualEncoding: Byte = toEncode.asByte

      actualEncoding mustEqual expectedEncoding
    }
  }

  "DataCodingScheme (object) as a factory for DataCodingSchemes" should {

    "correctly decode DataCoding.JIS data coding" in {
      val dataCodingScheme: Byte = Integer.parseInt("00000101", 2).toByte
      val decodedDataCodingScheme = DataCodingScheme(dataCodingScheme)

      decodedDataCodingScheme mustEqual DataCoding.JIS
    }

    "correctly decode MessageWaiting.DiscardMessage with SetIndicationInactive and FaxMessageWaiting" in {
      val dataCodingScheme: Byte = Integer.parseInt("11000001", 2).toByte
      val decodedDataCodingScheme = DataCodingScheme(dataCodingScheme)

      decodedDataCodingScheme mustEqual MessageWaiting(MessageWaiting.DiscardMessage,
        MessageWaiting.IndicationSense.SetIndicationInactive,
        MessageWaiting.IndicationType.FaxMessageWaiting)
    }

    "correctly decode MessageWaiting.DiscardMessage with SetIndicationActive and ElectronicMailMessageWaiting" in {
      val dataCodingScheme: Byte = Integer.parseInt("11001010", 2).toByte
      val decodedDataCodingScheme = DataCodingScheme(dataCodingScheme)

      decodedDataCodingScheme mustEqual MessageWaiting(MessageWaiting.DiscardMessage,
        MessageWaiting.IndicationSense.SetIndicationActive,
        MessageWaiting.IndicationType.ElectronicMailMessageWaiting)
    }

    "correctly decode MessageWaiting.StoreMessage with SetIndicationActive and VoiceMailMessageWaiting" in {
      val dataCodingScheme: Byte = Integer.parseInt("11011000", 2).toByte
      val decodedDataCodingScheme = DataCodingScheme(dataCodingScheme)

      decodedDataCodingScheme mustEqual MessageWaiting(MessageWaiting.StoreMessage,
        MessageWaiting.IndicationSense.SetIndicationActive,
        MessageWaiting.IndicationType.VoicemailMessageWaiting)
    }

    "correctly decode MessageWaiting.StoreMessage with SetIndicationActive and OtherMessageWaiting" in {
      val dataCodingScheme: Byte = Integer.parseInt("11011011", 2).toByte
      val decodedDataCodingScheme = DataCodingScheme(dataCodingScheme)

      decodedDataCodingScheme mustEqual MessageWaiting(MessageWaiting.StoreMessage,
        MessageWaiting.IndicationSense.SetIndicationActive,
        MessageWaiting.IndicationType.OtherMessageWaiting)
    }
  }
}
