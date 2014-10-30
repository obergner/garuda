package io.garuda.server.spi.session

import io.garuda.codec.pdu.{PduRequest, PduResponse}
import io.garuda.common.spi.session.Session

import scala.concurrent.Future

/**
 * Abstraction for how to `dispatch` inbound [[PduRequest]]s onto some arbitrary kind of `execution context`. Here, an
 * `execution context` could be any of
 *
 * - [[java.util.concurrent.ExecutorService]], typically a `thread pool`,
 * - [[scala.concurrent.ExecutionContext]], typically a `fork-join-pool`,
 * - ActorSystem,
 * - the same [[Thread]] that calls [[InboundPduRequestDispatcher.dispatch]], a Netty I/O thread
 * - ...
 *
 * Note that each [[InboundPduRequestDispatcher]] instance is associated with one [[Session]] and one
 * [[Session]] only, i.e. [[InboundPduRequestDispatcher]]s are never shared between different [[Session]].
 * Note also that this obviously does not keep you from having different [[InboundPduRequestDispatcher]] instances
 * sharing e.g. the same [[scala.concurrent.ExecutionContext]].
 *
 * @see [[Session]]
 */
trait InboundPduRequestDispatcher {

  /**
   * The [[Session]] this dispatcher is assigned to.
   *
   * @return The [[Session]] this dispatcher is assigned to
   */
  protected[this] def serverSession: Session

  /**
   * Dispatch the incoming [[PduRequest]] `request` onto some kind of `execution context`, typically a [[Thread]]. Return
   * a [[Future]] of the [[PduResponse]] to that `request`.
   *
   * @param request The [[PduRequest]] we want to process
   * @tparam Res Type of [[PduResponse]]
   * @tparam Req Type of [[PduRequest]]
   * @return A [[Future]] of the [[PduResponse]] to `request`
   */
  def dispatch[Res <: PduResponse, Req <: PduRequest[Res]](request: Req): Future[Res]
}
