package io.garuda.server.session.support

import java.util.concurrent.{ExecutorService, RejectedExecutionException}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{PduRequest, PduResponse}
import io.garuda.common.concurrent.CallerThreadExecutionContext
import io.garuda.common.logging.slf4j.MdcPropagatingRunnableWrapper
import io.garuda.common.metrics.ManagedMetricsSupport
import io.garuda.common.session.support.SessionListenerAdapter
import io.garuda.common.spi.session.Session
import io.garuda.server.session.ServerSessionConfig
import io.garuda.server.spi.session.{InboundPduRequestDispatcher, InboundPduRequestDispatcherFactory, InboundPduRequestHandler}
import nl.grons.metrics.scala.Timer

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * An [[InboundPduRequestDispatcher]] that dispatches inbound [[PduRequest]]s onto an [[ExecutorService]].
 *
 * This implementation will also publish a [[Timer]] that measures execution time of all calls to
 * [[InstrumentedCallerThreadInboundPduRequestDispatcher.dispatch]].
 *
 * @param session The [[io.garuda.common.spi.session.Session]] this dispatcher is assigned to
 * @param handler The [[InboundPduRequestHandler]] used to actually handle inbound [[PduRequest]]s
 * @param metricRegistry The [[MetricRegistry]] used to publish a [[Timer]] that measures execution times
 * @param executor The [[ExecutorService]] managing the [[Thread]]s to dispatch inbound [[PduRequest]]s onto
 * @throws IllegalArgumentException If one of the arguments is `null`
 */
@throws[IllegalArgumentException]("If one of the arguments is null")
class InstrumentedExecutorServiceInboundPduRequestDispatcher(session: Session,
                                                             handler: InboundPduRequestHandler,
                                                             protected[this] val metricRegistry: MetricRegistry,
                                                             executor: ExecutorService)
  extends InboundPduRequestDispatcher
  with ManagedMetricsSupport
  with Logging {
  require(session != null, "Argument 'session' must not be null")
  require(handler != null, "Argument 'handler' must not be null")
  require(metricRegistry != null, "Argument 'metricRegistry' must not be null")
  require(executor != null, "Argument 'executor' must not be null")

  /**
   * Call completion handlers in our `responsePromise` (see method `dispatch`) from the same thread that completes
   * that promise.
   */
  implicit private[this] val executionContext: ExecutionContext = CallerThreadExecutionContext

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
   * Dispatch the incoming [[PduRequest]] `request` onto a [[Thread]] managed by our [[ExecutorService]]. Return
   * a [[Future]] of the [[PduResponse]] to that `request`.
   *
   * @param request The [[PduRequest]] we want to process
   * @tparam Res Type of [[PduResponse]]
   * @tparam Req Type of [[PduRequest]]
   * @return A [[Future]] of the [[PduResponse]] to `request`
   */
  override def dispatch[Res <: PduResponse, Req <: PduRequest[Res]](request: Req): Future[Res] = {
    require(request != null, "Argument 'request' must not be null")
    import scala.concurrent.promise
    logger.info(s"Dispatching ${request} ...")
    val responsePromise = promise[Res]
    val task = requestProcessorTask(request, responsePromise)
    try this.executor.submit(task) catch {
      case ree: RejectedExecutionException => responsePromise.failure(ree)
    }
    logger.info(s"Dispatched ${request}")
    responsePromise.future
  }

  private[this] def requestProcessorTask[Res <: PduResponse, Req <: PduRequest[Res]](request: Req,
                                                                                     responsePromise: Promise[Res]): Runnable = {
    new MdcPropagatingRunnableWrapper(new PduRequestProcessor[Res, Req](request, responsePromise))
  }

  private[this] class PduRequestProcessor[Res <: PduResponse, Req <: PduRequest[Res]](request: Req,
                                                                                      responsePromise: Promise[Res])
    extends Runnable {

    override def run(): Unit = {
      logger.info(s"Processing ${request} ...")
      val response: Future[Res] = requestProcessingTimer.time[Future[Res]] {
        handler[Res, Req](request)
      }
      logger.info(s"Finished processing ${request}: -> ${response}")
      response onSuccess {
        case resp => responsePromise.success(resp)
      }
      response onFailure {
        case t: Throwable => responsePromise.failure(t)
      }
    }
  }

}

/**
 * A factory for [[InstrumentedExecutorServiceInboundPduRequestDispatcher]]s, using an [[ExecutorService]] supplied at
 * construction time for all [[InstrumentedExecutorServiceInboundPduRequestDispatcher]]s created by it.
 *
 * @param executor The [[ExecutorService]] to use for all created [[InstrumentedExecutorServiceInboundPduRequestDispatcher]]s
 * @throws IllegalArgumentException If `executor` is `null`
 */
@throws[IllegalArgumentException]("If executor is null")
class InstrumentedExecutorServiceInboundPduRequestDispatcherFactory(executor: ExecutorService)
  extends InboundPduRequestDispatcherFactory[InstrumentedExecutorServiceInboundPduRequestDispatcher] {
  require(executor != null, "Argument 'executor' must not be null")

  /**
   * Create a new [[InstrumentedExecutorServiceInboundPduRequestDispatcher]], assigned to the provided [[Session]]
   * `session` and using the supplied [[InboundPduRequestHandler]] `handler` to actually process requests.
   *
   * @param session The [[Session]] the new dispatcher will be assigned to
   * @param handler The [[InboundPduRequestHandler]] the new dispatcher will use to process incoming requests
   * @param config The [[ServerSessionConfig]] the new dispatcher will ask for its [[MetricRegistry]]
   * @throws IllegalArgumentException If one of the arguments is `null`
   * @return A new [[InstrumentedExecutorServiceInboundPduRequestDispatcher]], assigned to the supplied `session` and using
   *         the supplied `handler` to process requests
   */
  @throws[IllegalArgumentException]("If one of the arguments is null")
  override def apply(session: Session,
                     handler: InboundPduRequestHandler,
                     config: ServerSessionConfig): InstrumentedExecutorServiceInboundPduRequestDispatcher = {
    require(config != null, "Argument 'config' must not be null")
    new
        InstrumentedExecutorServiceInboundPduRequestDispatcher(session, handler, config.builderContext.metricRegistry, executor)
  }
}
