package io.garuda.server.spi

import io.garuda.common.spi.session.Session
import io.garuda.server.session.ServerSessionConfig
import io.netty.channel.Channel

/**
 *
 */
package object session {

  type InboundPduRequestHandlerFactory[T <: InboundPduRequestHandler] = Session => T

  type InboundPduRequestDispatcherFactory[T <: InboundPduRequestDispatcher] = (Session, InboundPduRequestHandler, ServerSessionConfig) => T

  type SmppServerSessionFactory[T <: Session] = (Channel, ServerSessionConfig) => T
}
