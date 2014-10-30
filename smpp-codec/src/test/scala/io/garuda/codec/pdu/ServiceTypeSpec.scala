package io.garuda.codec.pdu

import io.garuda.codec.pdu.ServiceType._
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.09.13
 * Time: 21:52
 * To change this template use File | Settings | File Templates.
 */
class ServiceTypeSpec extends Specification {

  "ServiceType as a factory for ServiceTypes given a string" should {

    "handle Default correctly" in {
      val serviceTypeString: String = ""
      ServiceType(serviceTypeString) mustEqual Default
    }

    "handle CMT correctly" in {
      val serviceTypeString: String = "CMT"
      ServiceType(serviceTypeString) mustEqual CMT
    }

    "handle CPT correctly" in {
      val serviceTypeString: String = "CPT"
      ServiceType(serviceTypeString) mustEqual CPT
    }

    "handle VMN correctly" in {
      val serviceTypeString: String = "VMN"
      ServiceType(serviceTypeString) mustEqual VMN
    }

    "handle VMA correctly" in {
      val serviceTypeString: String = "VMA"
      ServiceType(serviceTypeString) mustEqual VMA
    }

    "handle WAP correctly" in {
      val serviceTypeString: String = "WAP"
      ServiceType(serviceTypeString) mustEqual WAP
    }

    "handle USSD correctly" in {
      val serviceTypeString: String = "USSD"
      ServiceType(serviceTypeString) mustEqual USSD
    }

    "handle CBS correctly" in {
      val serviceTypeString: String = "CBS"
      ServiceType(serviceTypeString) mustEqual CBS
    }

    "handle GUTS correctly" in {
      val serviceTypeString: String = "GUTS"
      ServiceType(serviceTypeString) mustEqual GUTS
    }

    "handle a custom ServiceType correctly" in {
      val serviceTypeString: String = "customServiceType"
      ServiceType(serviceTypeString) mustEqual Custom(serviceTypeString)
    }
  }
}

