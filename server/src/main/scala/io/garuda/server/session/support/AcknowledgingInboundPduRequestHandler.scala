package io.garuda.server.session.support

import java.util.UUID

import io.garuda.codec.ErrorCode
import io.garuda.codec.pdu.{PduRequest, PduResponse, SubmitSm}
import io.garuda.common.spi.session.Session
import io.garuda.server.spi.session.{InboundPduRequestHandler, InboundPduRequestHandlerFactory}

import scala.concurrent.Future

/**
 * An [[InboundPduRequestHandler]] that simply creates a positive [[PduResponse]] (ack) for each incoming [[PduRequest]]
 * and returns it. Useful in test scenarios.
 */
object AcknowledgingInboundPduRequestHandler extends InboundPduRequestHandler {

  override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
    pduRequest match {
      case submitSm: SubmitSm => Future.successful(submitSm.createResponse(ErrorCode.ESME_ROK.code,
        UUID.randomUUID().toString).asInstanceOf[Res])
      case _ => Future.failed(throw new IllegalArgumentException("Unsupported PduRequest: " + pduRequest))
    }
  }

}

object AcknowledgingInboundPduRequestHandlerFactory
  extends InboundPduRequestHandlerFactory[AcknowledgingInboundPduRequestHandler.type] {

  override def apply(smppServerSession: Session): AcknowledgingInboundPduRequestHandler.type =
    AcknowledgingInboundPduRequestHandler
}
