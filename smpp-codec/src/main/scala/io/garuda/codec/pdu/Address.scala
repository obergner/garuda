package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 17.08.13
 * Time: 12:43
 * To change this template use File | Settings | File Templates.
 */
case class Address(ton: Ton, npi: Npi, address: String) {

  protected[pdu] def lengthInBytes: Int = {
    2 + address.length + 1
  }
}

object Address {

  val Unknown = Address(Ton.Unknown, Npi.Unknown, "")
}
