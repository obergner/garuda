package io.garuda.codec.pdu

import io.garuda.codec.pdu.Npi._
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 16.08.13
 * Time: 22:55
 * To change this template use File | Settings | File Templates.
 */
class NpiSpec extends Specification {

  "Npi as a factory for Npis given a byte" should {

    "handle Unknown correctly" in {
      val npiByte: Byte = 0
      Npi(npiByte) mustEqual Unknown
    }

    "handle IsdnNumberingPlan correctly" in {
      val npiByte: Byte = 1
      Npi(npiByte) mustEqual IsdnNumberingPlan
    }

    "handle DataNumberingPlan correctly" in {
      val npiByte: Byte = 3
      Npi(npiByte) mustEqual DataNumberingPlan
    }

    "handle TelexNumberingPlan correctly" in {
      val npiByte: Byte = 4
      Npi(npiByte) mustEqual TelexNumberingPlan
    }

    "handle LandMobile correctly" in {
      val npiByte: Byte = 6
      Npi(npiByte) mustEqual LandMobile
    }

    "handle NationalNumberingPlan correctly" in {
      val npiByte: Byte = 8
      Npi(npiByte) mustEqual NationalNumberingPlan
    }

    "handle PrivateNumberingPlan correctly" in {
      val npiByte: Byte = 9
      Npi(npiByte) mustEqual PrivateNumberingPlan
    }

    "handle ErmesNumberingPlan correctly" in {
      val npiByte: Byte = 10
      Npi(npiByte) mustEqual ErmesNumberingPlan
    }

    "handle Internet correctly" in {
      val npiByte: Byte = 13
      Npi(npiByte) mustEqual Internet
    }

    "handle WapClientId correctly" in {
      val npiByte: Byte = 18
      Npi(npiByte) mustEqual WapClientId
    }

    "throw an IllegalArgumentException when given an unknown Npi" in {
      val npiByte: Byte = 2
      Npi(npiByte) must throwA[IllegalArgumentException]
    }
  }
}
