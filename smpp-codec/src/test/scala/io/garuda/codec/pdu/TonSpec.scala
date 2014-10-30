package io.garuda.codec.pdu

import io.garuda.codec.pdu.Ton._
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 16.08.13
 * Time: 21:41
 * To change this template use File | Settings | File Templates.
 */
class TonSpec extends Specification {

  "Ton as a factory for Tons given a byte" should {

    "handle Unknown correctly" in {
      val tonByte: Byte = 0
      Ton(tonByte) mustEqual Unknown
    }

    "handle International correctly" in {
      val tonByte: Byte = 1
      Ton(tonByte) mustEqual International
    }

    "handle National correctly" in {
      val tonByte: Byte = 2
      Ton(tonByte) mustEqual National
    }

    "handle NetworkSpecific correctly" in {
      val tonByte: Byte = 3
      Ton(tonByte) mustEqual NetworkSpecific
    }

    "handle SubscriberNumber correctly" in {
      val tonByte: Byte = 4
      Ton(tonByte) mustEqual SubscriberNumber
    }

    "handle Alphanumeric correctly" in {
      val tonByte: Byte = 5
      Ton(tonByte) mustEqual Alphanumeric
    }

    "handle Abbreviated correctly" in {
      val tonByte: Byte = 6
      Ton(tonByte) mustEqual Abbreviated
    }

    "throw an IllegalArgumentException when given an unknown ton" in {
      val tonByte: Byte = 10
      Ton(tonByte) must throwA[IllegalArgumentException]
    }
  }
}
