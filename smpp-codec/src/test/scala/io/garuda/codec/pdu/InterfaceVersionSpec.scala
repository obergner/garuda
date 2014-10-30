package io.garuda.codec.pdu

import io.garuda.codec.InvalidPduFieldValueException
import io.garuda.codec.pdu.InterfaceVersion.{SMPP3_3, SMPP3_4, SMPP5_0}
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 12:17
 * To change this template use File | Settings | File Templates.
 */
class InterfaceVersionSpec extends Specification {

  "InterfaceVersion as a factory for InterfaceVersions given a byte" should {

    "handle SMPP3_3 correctly" in {
      val interfaceVersionByte: Byte = 0x33
      InterfaceVersion(interfaceVersionByte) mustEqual SMPP3_3
    }

    "handle SMPP3_4 correctly" in {
      val interfaceVersionByte: Byte = 0x34
      InterfaceVersion(interfaceVersionByte) mustEqual SMPP3_4
    }

    "handle SMPP5_0 correctly" in {
      val interfaceVersionByte: Byte = 0x50
      InterfaceVersion(interfaceVersionByte) mustEqual SMPP5_0
    }

    "throw an InvalidPduFieldValueException when given an unknown InterfaceVersion" in {
      val interfaceVersionByte: Byte = 0x60
      InterfaceVersion(interfaceVersionByte) must throwA[InvalidPduFieldValueException]
    }
  }
}
