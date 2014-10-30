package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 08.10.13
 * Time: 20:34
 * To change this template use File | Settings | File Templates.
 */
case class SubmitSm(seqNo: Option[Int],
                    rawServiceType: String,
                    sourceAddressTon: Byte,
                    sourceAddressNpi: Byte,
                    sourceAddressAddress: String,
                    destinationAddressTon: Byte,
                    destinationAddressNpi: Byte,
                    destinationAddressAddress: String,
                    rawEsmClass: Byte,
                    protocolId: Byte,
                    rawPriorityFlag: Byte,
                    rawScheduleDeliveryTime: String,
                    rawValidityPeriod: String,
                    rawRegisteredDelivery: Byte,
                    rawReplaceIfPresentFlag: Byte,
                    rawDataCodingScheme: Byte,
                    smDefaultMsgId: Byte,
                    shortMessage: Array[Byte],
                    tlvParameters: Vector[Tlv],
                    rawPdu: Option[Array[Byte]]) extends PduRequest[SubmitSmResponse](CommandId.submit_sm,
  seqNo,
  tlvParameters,
  rawPdu) {

  def serviceType: ServiceType = ServiceType(rawServiceType)

  def sourceAddress: Address = Address(Ton(sourceAddressTon), Npi(sourceAddressNpi), sourceAddressAddress)

  def destinationAddress: Address = Address(Ton(destinationAddressTon), Npi(destinationAddressNpi),
    destinationAddressAddress)

  def esmClass: EsmClass = EsmClass(rawEsmClass)

  def priorityFlag: PriorityFlag = PriorityFlag(rawPriorityFlag)

  def scheduleDeliveryTime: Option[SmppTime] = SmppTime(rawScheduleDeliveryTime)

  def validityPeriod: Option[SmppTime] = SmppTime(rawValidityPeriod)

  def registeredDelivery: RegisteredDelivery = RegisteredDelivery(rawRegisteredDelivery)

  def replaceIfPresentFlag: ReplaceIfPresentFlag = ReplaceIfPresentFlag(rawReplaceIfPresentFlag)

  def dataCodingScheme: DataCodingScheme = DataCodingScheme(rawDataCodingScheme)

  def createResponse(commandStatus: Int,
                     messageId: String,
                     tlvParameters: Vector[Tlv] = Vector.empty[Tlv]): SubmitSmResponse = {
    SubmitSmResponse(commandStatus, seqNo, messageId, tlvParameters, None)
  }

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {
    encodeNullTerminatedStringInto(rawServiceType, channelBuffer)
    channelBuffer.writeByte(sourceAddressTon)
    channelBuffer.writeByte(sourceAddressNpi)
    encodeNullTerminatedStringInto(sourceAddressAddress, channelBuffer)
    channelBuffer.writeByte(destinationAddressTon)
    channelBuffer.writeByte(destinationAddressNpi)
    encodeNullTerminatedStringInto(destinationAddressAddress, channelBuffer)
    channelBuffer.writeByte(rawEsmClass)
    channelBuffer.writeByte(protocolId)
    channelBuffer.writeByte(rawPriorityFlag)
    encodeNullTerminatedStringInto(rawScheduleDeliveryTime, channelBuffer)
    encodeNullTerminatedStringInto(rawValidityPeriod, channelBuffer)
    channelBuffer.writeByte(rawRegisteredDelivery)
    channelBuffer.writeByte(rawReplaceIfPresentFlag)
    channelBuffer.writeByte(rawDataCodingScheme)
    channelBuffer.writeByte(smDefaultMsgId)
    channelBuffer.writeByte(shortMessage.size)
    channelBuffer.writeBytes(shortMessage)
  }

  protected[this] def bodyLengthInBytes: Int = {
    rawServiceType.length + 1 +
      sourceAddress.lengthInBytes +
      destinationAddress.lengthInBytes +
      1 + /* rawEsmClass */
      1 + /* protocolId */
      1 + /* rawPriorityFlag */
      rawScheduleDeliveryTime.length + 1 +
      rawValidityPeriod.length + 1 +
      1 + /* rawRegisteredDelivery */
      1 + /* rawReplaceIfPresentFlag */
      1 + /* rawDataCodingScheme */
      1 + /* smDefaultMsgId */
      1 + shortMessage.length
  }
}

object SubmitSm extends PduDecoder[SubmitSm] {

  def apply(seqNo: Option[Int],
            rawServiceType: String,
            sourceAddress: Address,
            destinationAddress: Address,
            rawEsmClass: Byte,
            protocolId: Byte,
            rawPriorityFlag: Byte,
            rawScheduleDeliveryTime: String,
            rawValidityPeriod: String,
            rawRegisteredDelivery: Byte,
            rawReplaceIfPresentFlag: Byte,
            rawDataCodingScheme: Byte,
            smDefaultMsgId: Byte,
            shortMessage: Array[Byte],
            tlvParameters: Vector[Tlv] = Vector.empty[Tlv],
            rawPdu: Option[Array[Byte]] = None): SubmitSm = {
    SubmitSm(seqNo,
      rawServiceType,
      sourceAddress.ton.asByte,
      sourceAddress.npi.asByte,
      sourceAddress.address,
      destinationAddress.ton.asByte,
      destinationAddress.npi.asByte,
      destinationAddress.address,
      rawEsmClass,
      protocolId,
      rawPriorityFlag,
      rawScheduleDeliveryTime,
      rawValidityPeriod,
      rawRegisteredDelivery,
      rawReplaceIfPresentFlag,
      rawDataCodingScheme,
      smDefaultMsgId,
      shortMessage,
      tlvParameters,
      rawPdu)
  }

  protected case class SubmitSmBody(rawServiceType: String,
                                    sourceAddressTon: Byte,
                                    sourceAddressNpi: Byte,
                                    sourceAddressAdress: String,
                                    destinationAddressTon: Byte,
                                    destinationAddressNpi: Byte,
                                    destinationAddressAddress: String,
                                    rawEsmClass: Byte,
                                    protocolId: Byte,
                                    rawPriorityFlag: Byte,
                                    rawScheduleDeliveryTime: String,
                                    rawValidityPeriod: String,
                                    rawRegisteredDelivery: Byte,
                                    rawReplaceIfPresentFlag: Byte,
                                    rawDataCodingScheme: Byte,
                                    smDefaultMsgId: Byte,
                                    shortMessage: Array[Byte]) extends Body[SubmitSm]

  type BODY = SubmitSmBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): SubmitSm.SubmitSmBody = {
    val rawServiceType = readServiceTypeFrom(channelBuffer)
    val sourceAddressTon = channelBuffer.readByte()
    val sourceAddressNpi = channelBuffer.readByte()
    val sourceAddressAddress = readSourceAddressFrom(channelBuffer)
    val destinationAddressTon = channelBuffer.readByte()
    val destinationAddressNpi = channelBuffer.readByte()
    val destinationAddressAddress = readDestinationAddressFrom(channelBuffer)
    val rawEsmClass = channelBuffer.readByte()
    val protocolId = channelBuffer.readByte()
    val rawPriorityFlag = channelBuffer.readByte()
    val rawScheduleDeliveryTime = readScheduleDeliveryTimeFrom(channelBuffer)
    val rawValidityPeriod = readValidityPeriodFrom(channelBuffer)
    val rawRegisteredDelivery = channelBuffer.readByte()
    val rawReplaceIfPresentFlag = channelBuffer.readByte()
    val rawDataCodingScheme = channelBuffer.readByte()
    val smDefaultMsgId = channelBuffer.readByte()
    val shortMessageLength: Short = channelBuffer.readUnsignedByte()
    val shortMessage = new Array[Byte](shortMessageLength)
    channelBuffer.readBytes(shortMessage)

    SubmitSm.SubmitSmBody(rawServiceType,
      sourceAddressTon,
      sourceAddressNpi,
      sourceAddressAddress,
      destinationAddressTon,
      destinationAddressNpi,
      destinationAddressAddress,
      rawEsmClass,
      protocolId,
      rawPriorityFlag,
      rawScheduleDeliveryTime,
      rawValidityPeriod,
      rawRegisteredDelivery,
      rawReplaceIfPresentFlag,
      rawDataCodingScheme,
      smDefaultMsgId,
      shortMessage)
  }

  protected[this] def makePduFrom(header: Header, body: SubmitSm.SubmitSmBody, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): SubmitSm = {
    SubmitSm(header.sequenceNumber,
      body.rawServiceType,
      body.sourceAddressTon,
      body.sourceAddressNpi,
      body.sourceAddressAdress,
      body.destinationAddressTon,
      body.destinationAddressNpi,
      body.destinationAddressAddress,
      body.rawEsmClass,
      body.protocolId,
      body.rawPriorityFlag,
      body.rawScheduleDeliveryTime,
      body.rawValidityPeriod,
      body.rawRegisteredDelivery,
      body.rawReplaceIfPresentFlag,
      body.rawDataCodingScheme,
      body.smDefaultMsgId,
      body.shortMessage,
      tlvParameters,
      Some(rawPdu))
  }

  def newBuilder(): SubmitSmBuilder = new SubmitSmBuilder
}

//private\[this\] var ([a-zA-Z]+): ([a-zA-Z\[\].]+)
sealed class SubmitSmBuilder protected[pdu]() {

  private[this] var seqNo: Option[Int] = None

  private[this] var serviceType: ServiceType = ServiceType.Default

  private[this] var sourceAddressTon: Ton = Ton.International

  private[this] var sourceAddressNpi: Npi = Npi.IsdnNumberingPlan

  private[this] var sourceAddressAddress: String = null

  private[this] var destinationAddressTon: Ton = Ton.International

  private[this] var destinationAddressNpi: Npi = Npi.IsdnNumberingPlan

  private[this] var destinationAddressAddress: String = null

  private[this] var esmClass: EsmClass = EsmClass(MessagingMode.Default, MessageType.Default, GsmFeature.None)

  private[this] var protocolId: Byte = 0x00

  private[this] var priorityFlag: PriorityFlag = PriorityFlag.Zero

  private[this] var scheduleDeliveryTime: Option[SmppTime] = None

  private[this] var validityPeriod: Option[SmppTime] = None

  private[this] var registeredDelivery: RegisteredDelivery = RegisteredDelivery.Default

  private[this] var replaceIfPresentFlag: ReplaceIfPresentFlag = ReplaceIfPresentFlag.DoNotReplace

  private[this] var dataCodingScheme: DataCodingScheme = DataCodingScheme.Default

  private[this] var smDefaultMsgId: Byte = 0x00

  private[this] var shortMessage: Array[Byte] = Array(0x00.toByte)

  private[this] val tlvParameters: scala.collection.mutable.ArrayBuffer[Tlv] = scala.collection.mutable.ArrayBuffer()

  def seqNo(seqNo: Int): SubmitSmBuilder = {
    this.seqNo = Some(seqNo)
    this
  }

  def serviceType(serviceType: ServiceType): SubmitSmBuilder = {
    require(serviceType != null, "Argument 'serviceType' must not be null")
    this.serviceType = serviceType
    this
  }

  def sourceAddress(sourceAddress: Address): SubmitSmBuilder = {
    require(sourceAddress != null, "Argument 'sourceAddress' must not be null")
    this.sourceAddressTon = sourceAddress.ton
    this.sourceAddressNpi = sourceAddress.npi
    this.sourceAddressAddress = sourceAddress.address
    this
  }

  def sourceAddressTon(sourceAddressTon: Ton): SubmitSmBuilder = {
    require(sourceAddressTon != null, "Argument 'sourceAddress' must not be null")
    this.sourceAddressTon = sourceAddressTon
    this
  }

  def sourceAddressNpi(sourceAddressNpi: Npi): SubmitSmBuilder = {
    require(sourceAddressNpi != null, "Argument 'sourceAddressNpi' must not be null")
    this.sourceAddressNpi = sourceAddressNpi
    this
  }

  def sourceAddressAddress(sourceAddressAddress: String): SubmitSmBuilder = {
    require(sourceAddressAddress != null, "Argument 'sourceAddressAddress' must not be null")
    this.sourceAddressAddress = sourceAddressAddress
    this
  }

  def destinationAddress(destinationAddress: Address): SubmitSmBuilder = {
    require(destinationAddress != null, "Argument 'destinationAddress' must not be null")
    this.destinationAddressTon = destinationAddress.ton
    this.destinationAddressNpi = destinationAddress.npi
    this.destinationAddressAddress = destinationAddress.address
    this
  }

  def destinationAddressTon(destinationAddressTon: Ton): SubmitSmBuilder = {
    require(destinationAddressTon != null, "Argument 'destinationAddress' must not be null")
    this.destinationAddressTon = destinationAddressTon
    this
  }

  def destinationAddressNpi(destinationAddressNpi: Npi): SubmitSmBuilder = {
    require(destinationAddressNpi != null, "Argument 'destinationAddressNpi' must not be null")
    this.destinationAddressNpi = destinationAddressNpi
    this
  }

  def destinationAddressAddress(destinationAddressAddress: String): SubmitSmBuilder = {
    require(destinationAddressAddress != null, "Argument 'destinationAddressAddress' must not be null")
    this.destinationAddressAddress = destinationAddressAddress
    this
  }

  def esmClass(esmClass: EsmClass): SubmitSmBuilder = {
    require(esmClass != null, "Argument 'esmClass' must not be null")
    this.esmClass = esmClass
    this
  }

  def protocolId(protocolId: Byte): SubmitSmBuilder = {
    this.protocolId = protocolId
    this
  }

  def priorityFlag(priorityFlag: PriorityFlag): SubmitSmBuilder = {
    require(priorityFlag != null, "Argument 'priorityFlag' must not be null")
    this.priorityFlag = priorityFlag
    this
  }

  def scheduleDeliveryTime(scheduleDeliveryTime: SmppTime): SubmitSmBuilder = {
    require(scheduleDeliveryTime != null, "Argument 'scheduleDeliveryTime' must not be null")
    this.scheduleDeliveryTime = Some(scheduleDeliveryTime)
    this
  }

  def validityPeriod(validityPeriod: SmppTime): SubmitSmBuilder = {
    require(validityPeriod != null, "Argument 'validityPeriod' must not be null")
    this.validityPeriod = Some(validityPeriod)
    this
  }

  def registeredDelivery(registeredDelivery: RegisteredDelivery): SubmitSmBuilder = {
    require(registeredDelivery != null, "Argument 'registeredDelivery' must not be null")
    this.registeredDelivery = registeredDelivery
    this
  }

  def replaceIfPresentFlag(replaceIfPresentFlag: ReplaceIfPresentFlag): SubmitSmBuilder = {
    require(replaceIfPresentFlag != null, "Argument 'replaceIfPresentFlag' must not be null")
    this.replaceIfPresentFlag = replaceIfPresentFlag
    this
  }

  def dataCodingScheme(dataCodingScheme: DataCodingScheme): SubmitSmBuilder = {
    require(dataCodingScheme != null, "Argument 'dataCodingScheme' must not be null")
    this.dataCodingScheme = dataCodingScheme
    this
  }

  def smDefaultMsgId(smDefaultMsgId: Byte): SubmitSmBuilder = {
    this.smDefaultMsgId = smDefaultMsgId
    this
  }

  def shortMessage(shortMessage: Array[Byte]): SubmitSmBuilder = {
    require(shortMessage != null, "Argument 'shortMessage' must not be null")
    this.shortMessage = shortMessage
    this
  }

  def addTlvParameter(tlv: Tlv): SubmitSmBuilder = {
    require(tlv != null, "Argument 'tlv' must not be null")
    this.tlvParameters += tlv
    this
  }

  def build(): SubmitSm = {
    require(this.seqNo.isDefined, "A SubmitSm PDU MUST have a sequence number (seqNo) assigned")
    require(this.destinationAddressAddress != null, "A SubmitSm PDU MUST have a destination address")
    SubmitSm(
      this.seqNo,
      this.serviceType.asString,
      this.sourceAddressTon.asByte,
      this.sourceAddressNpi.asByte,
      this.sourceAddressAddress,
      this.destinationAddressTon.asByte,
      this.destinationAddressNpi.asByte,
      this.destinationAddressAddress,
      this.esmClass.asByte,
      this.protocolId,
      this.priorityFlag.value,
      if (this.scheduleDeliveryTime.isDefined) this.scheduleDeliveryTime.get.toString else "",
      if (this.validityPeriod.isDefined) this.validityPeriod.get.toString else "",
      this.registeredDelivery.asByte,
      this.replaceIfPresentFlag.asByte,
      this.dataCodingScheme.asByte,
      this.smDefaultMsgId,
      this.shortMessage,
      this.tlvParameters.toVector,
      None
    )
  }
}
