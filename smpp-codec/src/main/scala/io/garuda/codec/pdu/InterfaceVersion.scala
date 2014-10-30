package io.garuda.codec.pdu

import io.garuda.codec.{ErrorCode, InvalidPduFieldValueException}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class InterfaceVersion(interfaceVersion: Byte) {

  def asByte = interfaceVersion
}

object InterfaceVersion {

  case object SMPP3_3 extends InterfaceVersion(0x33.toByte)

  case object SMPP3_4 extends InterfaceVersion(0x34.toByte)

  case object SMPP5_0 extends InterfaceVersion(0x50.toByte)

  def apply(interfaceVersion: Byte): InterfaceVersion = interfaceVersion match {
    case 0x33 => SMPP3_3
    case 0x34 => SMPP3_4
    case 0x50 => SMPP5_0
    // TODO: Is this really a bind failure?
    case _ => throw new
        InvalidPduFieldValueException(ErrorCode.ESME_RBINDFAIL, "Illegal InterfaceVersion: " + interfaceVersion)
  }
}
