package io.garuda.codec.pdu

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 16.08.13
 * Time: 22:20
 * To change this template use File | Settings | File Templates.
 */
sealed abstract class Npi(npi: Byte) {

  def asByte = npi
}

object Npi {

  case object Unknown extends Npi(0)

  case object IsdnNumberingPlan extends Npi(1)

  case object DataNumberingPlan extends Npi(3)

  case object TelexNumberingPlan extends Npi(4)

  case object LandMobile extends Npi(6)

  case object NationalNumberingPlan extends Npi(8)

  case object PrivateNumberingPlan extends Npi(9)

  case object ErmesNumberingPlan extends Npi(10)

  case object Internet extends Npi(13)

  case object WapClientId extends Npi(18)

  def apply(npi: Byte): Npi = npi match {
    case 0 => Unknown
    case 1 => IsdnNumberingPlan
    case 3 => DataNumberingPlan
    case 4 => TelexNumberingPlan
    case 6 => LandMobile
    case 8 => NationalNumberingPlan
    case 9 => PrivateNumberingPlan
    case 10 => ErmesNumberingPlan
    case 13 => Internet
    case 18 => WapClientId
    // TODO: We should actually report either an invalid source address NPI or destination address NPI
    case _ => throw new IllegalArgumentException("Not a valid NPI (Numbering Plan Indicator): " + npi)
  }
}


