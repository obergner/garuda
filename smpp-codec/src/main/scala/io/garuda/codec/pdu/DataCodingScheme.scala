package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 04.10.13
 * Time: 15:43
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class DataCodingScheme(val codingGroup: DataCodingScheme.CodingGroup[_ <: DataCodingScheme]) {

  def asByte: Byte
}

object DataCodingScheme {

  val Default: DataCodingScheme = DataCodingScheme(0x00)

  sealed abstract class CodingGroup[A <: DataCodingScheme] {

    val RelevantBits: Byte

    def encodeInto(dataCodingScheme: Byte): Byte

    def apply(dataCodingScheme: Byte): A
  }

  object CodingGroup {

    def apply(dataCodingScheme: Byte): DataCodingScheme.CodingGroup[_ <: DataCodingScheme] = (dataCodingScheme &
      0xF0.toByte).toByte match {
      case DataCoding.AlphabetIndication.RelevantBits => DataCoding.AlphabetIndication
      case MessageWaiting.DiscardMessage.RelevantBits => MessageWaiting.DiscardMessage
      case MessageWaiting.StoreMessage.RelevantBits => MessageWaiting.StoreMessage
      case MessageCodingAndClass.MessageClassCodingGroup.RelevantBits => MessageCodingAndClass.MessageClassCodingGroup
      case dcs => new Reserved.ReservedCodingGroup(dcs)
    }
  }

  def apply(dataCodingScheme: Byte): DataCodingScheme = {
    val codingGroup = DataCodingScheme.CodingGroup(dataCodingScheme)
    codingGroup(dataCodingScheme)
  }
}

/*
 * Data Coding
 */

sealed abstract class DataCoding(bitmask: Byte) extends DataCodingScheme(DataCoding.AlphabetIndication) {

  def asByte: Byte = {
    var result: Byte = 0x00
    result = codingGroup.encodeInto(result)
    for (bitPos <- 0 to 3) {
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

object DataCoding {

  case object AlphabetIndication extends DataCodingScheme.CodingGroup[DataCoding] {

    val RelevantBits: Byte = 0x00

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      for (bitPos <- 4 to 7) {
        val bitAtPos: Byte = (1 << bitPos).toByte
        val setBitAtPos: Boolean = (RelevantBits & bitAtPos) == bitAtPos
        if (setBitAtPos) {
          result = (result | bitAtPos).toByte
        } else {
          result = (result & ~bitAtPos).toByte
        }
      }
      result
    }

    def apply(dataCodingScheme: Byte): DataCoding = (dataCodingScheme & 0x0F).toByte match {
      case 0x00 => MCSpecificDefault
      case 0x01 => IA5
      case 0x02 => EightBitBinaryTDMAAndCDMA
      case 0x03 => Latin1
      case 0x04 => EightBitBinaryAll
      case 0x05 => JIS
      case 0x06 => Cyrillic
      case 0x07 => LatinHebrew
      case 0x08 => UCS2
      case 0x09 => PictogramEncoding
      case 0x0A => MusicCodes
      case 0x0B => Reserved1
      case 0x0C => Reserved2
      case 0x0D => ExtendedKanjiJIS
      case 0x0E => KSC5601
      case 0x0F => Reserved3
    }

  }

  case object MCSpecificDefault extends DataCoding(0x00)

  case object IA5 extends DataCoding(0x01)

  case object EightBitBinaryTDMAAndCDMA extends DataCoding(0x02)

  case object Latin1 extends DataCoding(0x03)

  case object EightBitBinaryAll extends DataCoding(0x04)

  case object JIS extends DataCoding(0x05)

  case object Cyrillic extends DataCoding(0x06)

  case object LatinHebrew extends DataCoding(0x07)

  case object UCS2 extends DataCoding(0x08)

  case object PictogramEncoding extends DataCoding(0x09)

  case object MusicCodes extends DataCoding(0x0A)

  case object Reserved1 extends DataCoding(0x0B)

  case object Reserved2 extends DataCoding(0x0C)

  case object ExtendedKanjiJIS extends DataCoding(0x0D)

  case object KSC5601 extends DataCoding(0x0E)

  case object Reserved3 extends DataCoding(0x0F)

}

/*
 * Message Waiting
 */

case class MessageWaiting(override val codingGroup: MessageWaiting.MessageWaitingIndicationGroup,
                          indicationSense: MessageWaiting.IndicationSense,
                          indicationType: MessageWaiting.IndicationType) extends DataCodingScheme(codingGroup) {

  def asByte: Byte = {
    var result: Byte = 0x00
    result = codingGroup.encodeInto(result)
    result = indicationSense.encodeInto(result)
    result = indicationType.encodeInto(result)
    // Bit 2 is always set to 0
    (result & ~(1 << 2)).toByte
  }
}

object MessageWaiting {

  sealed abstract class MessageWaitingIndicationGroup(bitmask: Byte)
    extends DataCodingScheme.CodingGroup[MessageWaiting] {

    val RelevantBits: Byte = bitmask

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      for (bitPos <- 4 to 7) {
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

    def apply(dataCodingScheme: Byte): MessageWaiting = {
      require(((dataCodingScheme & 0xF0) == 0xC0) || ((dataCodingScheme & 0xF0) == 0xD0),
        "This does not look like a MessageWaitingIndicationGroup: " + dataCodingScheme)
      val indicationSense = IndicationSense(dataCodingScheme)
      val indicationType = IndicationType(dataCodingScheme)
      MessageWaiting(this, indicationSense, indicationType)
    }

  }

  case object DiscardMessage extends MessageWaitingIndicationGroup(0xC0.toByte)

  case object StoreMessage extends MessageWaitingIndicationGroup(0xD0.toByte)

  sealed abstract class IndicationSense(bitmask: Byte) {

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      val bitPos: Int = 3
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

  object IndicationSense {

    case object SetIndicationInactive extends IndicationSense(0x00)

    case object SetIndicationActive extends IndicationSense(0x08)

    def apply(dataCodingScheme: Byte): IndicationSense = {
      if ((dataCodingScheme & 0x08) == 0x00) {
        SetIndicationInactive
      } else {
        SetIndicationActive
      }
    }
  }

  sealed abstract class IndicationType(bitmask: Byte) {

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
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

  object IndicationType {

    case object VoicemailMessageWaiting extends IndicationType(0x00)

    case object FaxMessageWaiting extends IndicationType(0x01)

    case object ElectronicMailMessageWaiting extends IndicationType(0x02)

    case object OtherMessageWaiting extends IndicationType(0x03)

    def apply(dataCodingScheme: Byte): IndicationType = {
      if ((dataCodingScheme & 0x03) == 0x00) {
        VoicemailMessageWaiting
      } else if ((dataCodingScheme & 0x03) == 0x01) {
        FaxMessageWaiting
      } else if ((dataCodingScheme & 0x03) == 0x02) {
        ElectronicMailMessageWaiting
      } else {
        OtherMessageWaiting
      }
    }
  }

}

/*
 * Message Coding and Message Class
 */

case class MessageCodingAndClass(messageCoding: MessageCodingAndClass.MessageCoding,
                                 messageClass: MessageCodingAndClass.MessageClass)
  extends DataCodingScheme(MessageCodingAndClass.MessageClassCodingGroup) {

  def asByte: Byte = {
    var result: Byte = 0x00
    codingGroup.encodeInto(result)
    result = messageCoding.encodeInto(result)
    result = messageClass.encodeInto(result)
    // Bit 3 is always set to 0
    (result & ~(1 << 3)).toByte
  }
}

object MessageCodingAndClass {

  object MessageClassCodingGroup extends DataCodingScheme.CodingGroup[MessageCodingAndClass] {

    val RelevantBits: Byte = 0xF0.toByte

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      for (bitPos <- 4 to 7) {
        val bitAtPos: Byte = (1 << bitPos).toByte
        val setBitAtPos: Boolean = (RelevantBits & bitAtPos) == bitAtPos
        if (setBitAtPos) {
          result = (result | bitAtPos).toByte
        } else {
          result = (result & ~bitAtPos).toByte
        }
      }
      result
    }

    def apply(dataCodingScheme: Byte): MessageCodingAndClass = {
      require((dataCodingScheme & 0xF0) == 0xF0, "This does not look like a MessageClassCodingGroup: " +
        dataCodingScheme)
      val messageCoding = MessageCoding(dataCodingScheme)
      val messageClass = MessageClass(dataCodingScheme)
      MessageCodingAndClass(messageCoding, messageClass)
    }

  }

  sealed abstract class MessageCoding(bitmask: Byte) {

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      val bitPos: Int = 2
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

  object MessageCoding {

    case object DefaultAlphabet extends MessageCoding(0x00)

    case object EightBitData extends MessageCoding(0x01)

    def apply(dataCodingScheme: Byte): MessageCoding = {
      if ((dataCodingScheme & 0x04) == 0x00) {
        DefaultAlphabet
      } else {
        EightBitData
      }
    }
  }

  sealed abstract class MessageClass(bitmask: Byte) {

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
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

  object MessageClass {

    case object Class0 extends MessageClass(0x00)

    case object Class1 extends MessageClass(0x01)

    case object Class2 extends MessageClass(0x02)

    case object Class3 extends MessageClass(0x03)

    def apply(dataCodingScheme: Byte): MessageClass = {
      if ((dataCodingScheme & 0x03) == 0x00) {
        Class0
      } else if ((dataCodingScheme & 0x03) == 0x01) {
        Class1
      } else if ((dataCodingScheme & 0x03) == 0x02) {
        Class2
      } else {
        Class3
      }
    }
  }

}

/*
 * Reserved coding groups
 */

case class Reserved(override val codingGroup: Reserved.ReservedCodingGroup, value: Byte)
  extends DataCodingScheme(codingGroup) {

  def asByte: Byte = value
}

object Reserved {

  case class ReservedCodingGroup(bitmask: Byte) extends DataCodingScheme.CodingGroup[Reserved] {

    val RelevantBits: Byte = bitmask

    def encodeInto(dataCodingScheme: Byte): Byte = {
      var result: Byte = dataCodingScheme
      for (bitPos <- 4 to 7) {
        val bitAtPos: Byte = (1 << bitPos).toByte
        val setBitAtPos: Boolean = (RelevantBits & bitAtPos) == bitAtPos
        if (setBitAtPos) {
          result = (result | bitAtPos).toByte
        } else {
          result = (result & ~bitAtPos).toByte
        }
      }
      result
    }

    def apply(dataCodingScheme: Byte): Reserved = new Reserved(this, dataCodingScheme)
  }

}


// ---------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------

//sealed abstract class DataCodingScheme[A <: DataCodingScheme[A, B], B <: DataCodingScheme.CodingGroup[A]](val
// codingGroup: B) {
//
//  def asByte(dataCodingScheme: Byte): Byte
//}
//
//object DataCodingScheme {
//
//  sealed abstract class CodingGroup[A <: DataCodingScheme[A, _]] {
//
//    def asByte(dataCodingScheme: Byte): Byte
//
//    def apply(dataCodingScheme: Byte): A
//  }
//
//  object CodingGroup {
//
//    def apply(dataCodingScheme: Byte): DataCodingScheme.CodingGroup[_ <: DataCodingScheme[_,_]] = (dataCodingScheme &
//      0xF0).toByte match {
//      case 0x00 => DataCoding.AlphabetIndication
//      //case 0xC0 => MessageWaiting.DiscardMessage
//      //case 0xD0 => MessageWaiting.StoreMessage
//      case 0xF0 => MessageCodingAndClass.MessageClassCodingGroup
//      case dcs => new Reserved.ReservedCodingGroup(dcs)
//    }
//  }
//
//  def apply(dataCodingScheme: Byte): DataCodingScheme[_,_] = {
//    val codingGroup = DataCodingScheme.CodingGroup(dataCodingScheme)
//    codingGroup(dataCodingScheme)
//  }
//}
//
//sealed abstract class DataCoding(bitmask: Byte) extends DataCodingScheme[DataCoding,
// DataCoding.AlphabetIndication.type](DataCoding.AlphabetIndication) {
//
//  def asByte(dataCodingScheme: Byte): Byte = {
//    var result: Byte = dataCodingScheme
//    result = codingGroup.asByte(result)
//    for (bitPos <- 0 to 3) {
//      val bitAtPos: Byte = (1 << bitPos).toByte
//      val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//      if (setBitAtPos) {
//        result = (result | bitAtPos).toByte
//      } else {
//        result = (result & ~bitAtPos).toByte
//      }
//    }
//    result
//  }
//}
//
//object DataCoding {
//
//  case object AlphabetIndication extends DataCodingScheme.CodingGroup[DataCoding] {
//
//    private[this] val bitmask: Byte = 0x00
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 4 to 7) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//
//    def apply(dataCodingScheme: Byte): DataCoding = (dataCodingScheme & 0x0F).toByte match {
//      case 0x00 => MCSpecificDefault
//      case 0x01 => IA5
//      case 0x02 => EightBitBinaryTDMAAndCDMA
//      case 0x03 => Latin1
//      case 0x04 => EightBitBinaryAll
//      case 0x05 => JIS
//      case 0x06 => Cyrillic
//      case 0x07 => LatinHebrew
//      case 0x08 => UCS2
//      case 0x09 => PictogramEncoding
//      case 0x0A => MusicCodes
//      case 0x0B => Reserved1
//      case 0x0C => Reserved2
//      case 0x0D => ExtendedKanjiJIS
//      case 0x0E => KSC5601
//      case 0x0F => Reserved3
//    }
//  }
//
//  case object MCSpecificDefault extends DataCoding(0x00)
//
//  case object IA5 extends DataCoding(0x01)
//
//  case object EightBitBinaryTDMAAndCDMA extends DataCoding(0x02)
//
//  case object Latin1 extends DataCoding(0x03)
//
//  case object EightBitBinaryAll extends DataCoding(0x04)
//
//  case object JIS extends DataCoding(0x05)
//
//  case object Cyrillic extends DataCoding(0x06)
//
//  case object LatinHebrew extends DataCoding(0x07)
//
//  case object UCS2 extends DataCoding(0x08)
//
//  case object PictogramEncoding extends DataCoding(0x09)
//
//  case object MusicCodes extends DataCoding(0x0A)
//
//  case object Reserved1 extends DataCoding(0x0B)
//
//  case object Reserved2 extends DataCoding(0x0C)
//
//  case object ExtendedKanjiJIS extends DataCoding(0x0D)
//
//  case object KSC5601 extends DataCoding(0x0E)
//
//  case object Reserved3 extends DataCoding(0x0F)
//
//}
//
//
//case class MessageWaiting(override val codingGroup: DataCodingScheme.CodingGroup[MessageWaiting],
//                                                                             indicationSense:
//                                                                             MessageWaiting.IndicationSense,
//                                                                             indicationType:
//                                                                             MessageWaiting.IndicationType)
//  extends DataCodingScheme[MessageWaiting, DataCodingScheme.CodingGroup[MessageWaiting]](codingGroup) {
//
//  def asByte(dataCodingScheme: Byte): Byte = {
//    var result: Byte = dataCodingScheme
//    result = codingGroup.asByte(result)
//    result = indicationSense.asByte(result)
//    result = indicationType.asByte(result)
//    // Bit 2 is always set to 0
//    (result & ~(1 << 2)).toByte
//  }
//}
//
//object MessageWaiting {
//
//  sealed abstract class MessageWaitingIndicationGroup(bitmask: Byte)
//    extends DataCodingScheme.CodingGroup[MessageWaiting] {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 4 to 7) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//
//    def apply(dataCodingScheme: Byte): MessageWaiting = {
//      require(((dataCodingScheme & 0xF0) == 0xC0) || ((dataCodingScheme & 0xF0) == 0xD0),
//              "This does not look like a MessageWaitingIndicationGroup: " + dataCodingScheme)
//      val indicationSense = IndicationSense(dataCodingScheme)
//      val indicationType = IndicationType(dataCodingScheme)
//      MessageWaiting(this, indicationSense, indicationType)
//    }
//  }
//
//  case object DiscardMessage extends MessageWaitingIndicationGroup(0xC0.toByte)
//
//  case object StoreMessage extends MessageWaitingIndicationGroup(0xD0.toByte)
//
//  sealed abstract class IndicationSense(bitmask: Byte) {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      val bitPos: Int = 3
//      val bitAtPos: Byte = (1 << bitPos).toByte
//      val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//      if (setBitAtPos) {
//        result = (result | bitAtPos).toByte
//      } else {
//        result = (result & ~bitAtPos).toByte
//      }
//      result
//    }
//  }
//
//  object IndicationSense {
//
//    case object SetIndicationInactive extends IndicationSense(0x00)
//
//    case object SetIndicationActive extends IndicationSense(0x08)
//
//    def apply(dataCodingScheme: Byte): IndicationSense = {
//      if ((dataCodingScheme & 0x08) == 0x00) {
//        SetIndicationInactive
//      } else {
//        SetIndicationActive
//      }
//    }
//  }
//
//  sealed abstract class IndicationType(bitmask: Byte) {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 0 to 1) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//  }
//
//  object IndicationType {
//
//    case object VoicemailMessageWaiting extends IndicationType(0x00)
//
//    case object FaxMessageWaiting extends IndicationType(0x01)
//
//    case object ElectronicMailMessageWaiting extends IndicationType(0x02)
//
//    case object OtherMessageWaiting extends IndicationType(0x03)
//
//    def apply(dataCodingScheme: Byte): IndicationType = {
//      if ((dataCodingScheme & 0x03) == 0x00) {
//        VoicemailMessageWaiting
//      } else if ((dataCodingScheme & 0x03) == 0x01) {
//        FaxMessageWaiting
//      } else if ((dataCodingScheme & 0x03) == 0x02) {
//        ElectronicMailMessageWaiting
//      } else {
//        OtherMessageWaiting
//      }
//    }
//  }
//
//}
//
//case class MessageCodingAndClass(messageCoding: MessageCodingAndClass.MessageCoding,
//                                 messageClass: MessageCodingAndClass.MessageClass)
//  extends DataCodingScheme[MessageCodingAndClass, MessageCodingAndClass.MessageClassCodingGroup.type]
// (MessageCodingAndClass.MessageClassCodingGroup) {
//
//  def asByte(dataCodingScheme: Byte): Byte = {
//    var result: Byte = dataCodingScheme
//    codingGroup.asByte(dataCodingScheme)
//    result = messageCoding.asByte(result)
//    result = messageClass.asByte(result)
//    // Bit 3 is always set to 0
//    (result & ~(1 << 3)).toByte
//  }
//}
//
//object MessageCodingAndClass {
//
//  object MessageClassCodingGroup extends DataCodingScheme.CodingGroup[MessageCodingAndClass] {
//
//    private[this] val bitmask: Byte = 0xF0.toByte
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 4 to 7) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//
//    def apply(dataCodingScheme: Byte): MessageCodingAndClass = {
//      require((dataCodingScheme & 0xF0) == 0xF0, "This does not look like a MessageClassCodingGroup: " +
//        dataCodingScheme)
//      val messageCoding = MessageCoding(dataCodingScheme)
//      val messageClass = MessageClass(dataCodingScheme)
//      MessageCodingAndClass(messageCoding, messageClass)
//    }
//  }
//
//  sealed abstract class MessageCoding(bitmask: Byte) {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      val bitPos: Int = 2
//      val bitAtPos: Byte = (1 << bitPos).toByte
//      val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//      if (setBitAtPos) {
//        result = (result | bitAtPos).toByte
//      } else {
//        result = (result & ~bitAtPos).toByte
//      }
//      result
//    }
//  }
//
//  object MessageCoding {
//
//    case object DefaultAlphabet extends MessageCoding(0x00)
//
//    case object EightBitData extends MessageCoding(0x01)
//
//    def apply(dataCodingScheme: Byte): MessageCoding = {
//      if ((dataCodingScheme & 0x04) == 0x00) {
//        DefaultAlphabet
//      } else {
//        EightBitData
//      }
//    }
//  }
//
//  sealed abstract class MessageClass(bitmask: Byte) {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 0 to 1) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//  }
//
//  object MessageClass {
//
//    case object Class0 extends MessageClass(0x00)
//
//    case object Class1 extends MessageClass(0x01)
//
//    case object Class2 extends MessageClass(0x02)
//
//    case object Class3 extends MessageClass(0x03)
//
//    def apply(dataCodingScheme: Byte): MessageClass = {
//      if ((dataCodingScheme & 0x03) == 0x00) {
//        Class0
//      } else if ((dataCodingScheme & 0x03) == 0x01) {
//        Class1
//      } else if ((dataCodingScheme & 0x03) == 0x02) {
//        Class2
//      } else {
//        Class3
//      }
//    }
//  }
//
//}
//
//case class Reserved(override val codingGroup: Reserved.ReservedCodingGroup, value: Byte)
//  extends DataCodingScheme[Reserved, Reserved.ReservedCodingGroup](codingGroup) {
//
//  def asByte(dataCodingScheme: Byte): Byte = ???
//}
//
//object Reserved {
//
//  case class ReservedCodingGroup(bitmask: Byte) extends DataCodingScheme.CodingGroup[Reserved] {
//
//    def asByte(dataCodingScheme: Byte): Byte = {
//      var result: Byte = dataCodingScheme
//      for (bitPos <- 4 to 7) {
//        val bitAtPos: Byte = (1 << bitPos).toByte
//        val setBitAtPos: Boolean = ((bitmask & bitAtPos) == bitAtPos)
//        if (setBitAtPos) {
//          result = (result | bitAtPos).toByte
//        } else {
//          result = (result & ~bitAtPos).toByte
//        }
//      }
//      result
//    }
//
//    def apply(dataCodingScheme: Byte): Reserved = new Reserved(this, dataCodingScheme)
//  }
//
//}
