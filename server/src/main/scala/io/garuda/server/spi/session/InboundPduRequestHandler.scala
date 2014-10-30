package io.garuda.server.spi.session

import io.garuda.codec.pdu.{PduRequest, PduResponse}

import scala.concurrent.Future

/**
 * Created by obergner on 11.05.14.
 */
trait InboundPduRequestHandler {

  def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res]

}
