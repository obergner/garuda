package io.garuda.server.session.support

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{PduRequest, PduResponse}
import io.garuda.common.metrics.ManagedMetricsSupport
import io.garuda.common.session.support.SessionListenerAdapter
import io.garuda.common.spi.session.Session
import io.garuda.server.session.ServerSessionConfig
import io.garuda.server.spi.session.{InboundPduRequestDispatcher, InboundPduRequestDispatcherFactory, InboundPduRequestHandler}
import nl.grons.metrics.scala.Timer

import scala.concurrent.Future

/**
 * An [[InboundPduRequestDispatcher]] that dispatches inbound [[PduRequest]]s onto the calling thread. Typically, this
 * will be an I/O thread managed by Netty.
 *
 * This implementation will also publish a [[Timer]] that measures execution time of all calls to
 * [[InstrumentedCallerThreadInboundPduRequestDispatcher.dispatch]].
 *
 * @param session The [[io.garuda.common.spi.session.Session]] this dispatcher is assigned to, must not be `null`
 * @param handler The [[InboundPduRequestHandler]] used to actually handle inbound [[PduRequest]]s
 * @param metricRegistry The [[MetricRegistry]] used to publish a [[Timer]] that measures execution times
 * @throws IllegalArgumentException If one of the arguments is `null`
 */
@throws[IllegalArgumentException]("If one of the arguments is null")
class InstrumentedCallerThreadInboundPduRequestDispatcher(session: Session,
                                                          handler: InboundPduRequestHandler,
                                                          protected[this] val metricRegistry: MetricRegistry)
  extends InboundPduRequestDispatcher
  with ManagedMetricsSupport
  with Logging {
  require(session != null, "Argument 'session' must not be null")
  require(handler != null, "Argument 'handler' must not be null")
  require(metricRegistry != null, "Argument 'metricRegistry' must not be null")

  private[this] val requestProcessingTimer: Timer = metrics.timer("request-processing-time", session.id)

  session.addListener(new SessionListenerAdapter {
    override def sessionClosed(session: Session): Unit = unregisterAllMetrics()
  })

  /**
   * The [[Session]] this dispatcher is assigned to.
   *
   * @return The [[Session]] this dispatcher is assigned to
   */
  override protected[this] def serverSession: Session = session

  /**
   * Dispatch the incoming [[PduRequest]] `request` onto the calling [[Thread]], typically an I/O thread managed by Netty.
   * Return a [[Future]] of the [[PduResponse]] to that `request`.
   *
   * @param request The [[PduRequest]] we want to process
   * @tparam Res Type of [[PduResponse]]
   * @tparam Req Type of [[PduRequest]]
   * @return A [[Future]] of the [[PduResponse]] to `request`
   */
  override def dispatch[Res <: PduResponse, Req <: PduRequest[Res]](request: Req): Future[Res] = {
    logger.info(s"Processing ${request} ...")
    val response: Future[Res] = requestProcessingTimer.time[Future[Res]] {
      handler[Res, Req](request)
    }
    logger.info(s"Finished processing ${request}: -> ${response}")
    response
  }
}

/**
 * A factory for [[InstrumentedCallerThreadInboundPduRequestDispatcher]]s.
 */
object InstrumentedCallerThreadInboundPduRequestDispatcherFactory
  extends InboundPduRequestDispatcherFactory[InstrumentedCallerThreadInboundPduRequestDispatcher] {

  /**
   * Create a new [[InstrumentedCallerThreadInboundPduRequestDispatcher]], assigned to the provided [[Session]]
   * `session` and using the supplied [[InboundPduRequestHandler]] `handler` to actually process requests.
   *
   * @param session The [[Session]] the new dispatcher will be assigned to
   * @param handler The [[InboundPduRequestHandler]] the new dispatcher will use to process incoming requests
   * @param config The [[ServerSessionConfig]] the new dispatcher will ask for its [[MetricRegistry]]
   * @throws IllegalArgumentException If one of the arguments is `null`
   * @return A new [[InstrumentedCallerThreadInboundPduRequestDispatcher]], assigned to the supplied `session` and using
   *         the supplied `handler` to process requests
   */
  @throws[IllegalArgumentException]("If one of the arguments is null")
  override def apply(session: Session,
                     handler: InboundPduRequestHandler,
                     config: ServerSessionConfig): InstrumentedCallerThreadInboundPduRequestDispatcher = {
    require(config != null, "Argument 'config' must not be null")
    new InstrumentedCallerThreadInboundPduRequestDispatcher(session, handler, config.builderContext.metricRegistry)
  }
}
