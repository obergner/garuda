package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.09.13
 * Time: 20:38
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class ServiceType(value: String) {

  def asString: String = value
}

object ServiceType {

  case object Default extends ServiceType("")

  case object CMT extends ServiceType("CMT")

  case object CPT extends ServiceType("CPT")

  case object VMN extends ServiceType("VMN")

  case object VMA extends ServiceType("VMA")

  case object WAP extends ServiceType("WAP")

  case object USSD extends ServiceType("USSD")

  case object CBS extends ServiceType("CBS")

  case object GUTS extends ServiceType("GUTS")

  case class Custom(value: String) extends ServiceType(value)

  def apply(value: String): ServiceType = value match {
    case "" => Default
    case "CMT" => CMT
    case "CPT" => CPT
    case "VMN" => VMN
    case "VMA" => VMA
    case "WAP" => WAP
    case "USSD" => USSD
    case "CBS" => CBS
    case "GUTS" => GUTS
    case other => Custom(other)
  }
}
