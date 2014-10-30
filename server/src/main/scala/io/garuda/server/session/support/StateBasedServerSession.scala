package io.garuda.server.session.support

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu._
import io.garuda.codec.{ErrorCode, SmppPduFrameDecoder}
import io.garuda.common.authentication._
import io.garuda.common.channel._
import io.garuda.common.logging.RemoteSystemIdContextKey
import io.garuda.common.logging.slf4j.ChannelRegisteringExecutionContextWrapper
import io.garuda.common.session.ping.PeriodicPing
import io.garuda.common.spi.session.Session.{BindType, State}
import io.garuda.common.spi.session.{Session, SessionChannelAdapter}
import io.garuda.server.session.ServerSessionConfig
import io.garuda.server.spi.authentication.{AuthenticatedClient, Credentials, InvalidCredentialsException}
import io.garuda.server.spi.session.InboundPduRequestDispatcher
import io.garuda.windowing.{Slot, SlotAcquisitionTimedOutException, Window}
import io.netty.channel._
import io.netty.util.concurrent.Future
import io.netty.util.{Timeout, Timer, TimerTask}
import org.slf4j.MDC

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Abstract base class for classes representing an SMPP session. Concrete subclasses represent SMPP sessions in a particular
 * state, UNBOUND or BOUND, depending on whether a session has already been authenticated (BOUND) or not (UNBOUND).
 *
 * User: obergner
 * Date: 22.11.13
 * Time: 21:30
 */
class StateBasedServerSession(val channel: Channel, config: ServerSessionConfig)
  extends Session
  with SessionChannelAdapter
  with Logging {

  /* -------------------------------------------------------------------------------------------------------------------
     Instance state
     -------------------------------------------------------------------------------------------------------------------
   */
  protected[support] implicit val userExecutionContext: ExecutionContext =
    new ChannelRegisteringExecutionContextWrapper(channel, config.userExecutionContext)

  protected[support] val currentStateDelegateRef: AtomicReference[StateDelegate] =
    new AtomicReference[StateDelegate](new InitialStateDelegate(config))

  protected[support] def currentStateDelegate: StateDelegate = currentStateDelegateRef.get

  /* -------------------------------------------------------------------------------------------------------------------
     io.garuda.server.spi.session.Session
     -------------------------------------------------------------------------------------------------------------------
   */

  def id: String = UUID.randomUUID().toString // TODO: Find a better ID

  def currentState: State = currentStateDelegate.state

  def authenticatedSystem: Option[AuthenticatedClient] = {
    val system: RemoteSystem = channel.attr(RemoteSystemAttributeKey).get()
    if (system != null) Some(system.asInstanceOf[AuthenticatedClient]) else None
  }

  def close(): Unit = currentStateDelegate.close()

  def channelAdapter: SessionChannelAdapter = this

  /* -------------------------------------------------------------------------------------------------------------------
     io.garuda.common.spi.session.SessionChannelAdapter
     -------------------------------------------------------------------------------------------------------------------
   */

  def channelActivated(ctx: ChannelHandlerContext): Unit = currentStateDelegate.sessionActivated(ctx)

  def bindRequestReceived(ctx: ChannelHandlerContext, bindRequest: BaseBind[_ <: BaseBindResponse]): Unit =
    currentStateDelegate.bindRequestReceived(ctx, bindRequest)

  def unbindRequestReceived(ctx: ChannelHandlerContext, unbindRequest: Unbind): Unit =
    currentStateDelegate.unbindRequestReceived(ctx, unbindRequest)

  def enquireLinkRequestReceived(ctx: ChannelHandlerContext, enquireLinkRequest: EnquireLink): Unit =
    currentStateDelegate.enquireLinkRequestReceived(ctx, enquireLinkRequest)

  def pduRequestReceived[P <: PduResponse, R <: PduRequest[P]](ctx: ChannelHandlerContext, pduRequest: R): Unit =
    currentStateDelegate.pduRequestReceived[P, R](ctx, pduRequest)

  def channelClosed(ctx: ChannelHandlerContext): Unit =
    currentStateDelegate.close()

  /* -------------------------------------------------------------------------------------------------------------------
     toString() etc.
     -------------------------------------------------------------------------------------------------------------------
   */

  override def toString: String = s"${getClass.getSimpleName}(${currentState}/${channel})"

  /* -------------------------------------------------------------------------------------------------------------------
     State pattern
     -------------------------------------------------------------------------------------------------------------------
   */

  /**
   * An SMPP session's current state, UNBOUND, BINDING, BOUND, CLOSING, CLOSED. This trait is called a Delegate simply to avoid
   * confusion with [[Session.State]].
   *
   * A [[StateBasedServerSession]] will delegate handling of incoming and outgoing messages to its current state.
   * @param state
   * @param config
   */
  protected[this] sealed abstract class StateDelegate(protected[support] val state: Session.State,
                                                      protected[support] val config: ServerSessionConfig) {

    /**
     * Callback invoked when this state is entered.
     */
    protected[support] def onEnter(): Unit = {}

    /**
     * Callback invoked once when an SMPP session is established.
     *
     * @param ctx
     */
    protected[support] def sessionActivated(ctx: ChannelHandlerContext): Unit =
      assert(false, "Illegal attempt to activate session in state [" + state + "]")

    /**
     * Callback invoked whenever a [[BaseBind]] request is received.
     *
     * @param ctx
     * @param bindRequest
     */
    protected[support] def bindRequestReceived(ctx: ChannelHandlerContext, bindRequest: BaseBind[_ <: BaseBindResponse]): Unit = {
      logger.warn(s"RECEIVED: bind request ${bindRequest} in session ${session} state ${state} - will send nack")
      ctx.channel().write(bindRequest.createResponse(ErrorCode.ESME_RALYBND.code, config.systemId, InterfaceVersion.SMPP5_0))
    }

    protected[support] def unbindRequestReceived(context: ChannelHandlerContext, unbind: Unbind): Unit = {
      logger.warn(s"RECEIVED: unbind request ${unbind} in session ${session} state ${state} - IGNORE")
    }

    /**
     * Callback invoked whenever an [[EnquireLink]] request is received. This base implementation will simply log a
     * warning and otherwise discard the received [[EnquireLink]] request, without sending a response.
     *
     * @param ctx
     * @param enquireLinkRequest
     */
    protected[support] def enquireLinkRequestReceived(ctx: ChannelHandlerContext, enquireLinkRequest: EnquireLink): Unit = {
      logger.warn(s"RECEIVED: enquire link request (ping) $enquireLinkRequest in session ${session} state ${state} - IGNORE")
    }

    /**
     * Callback invoked when the underlying [[Channel]] has received a "regular" [[PduRequest]], i.e.
     * a [[PduRequest]] that is not a session management operation.
     *
     * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
     * @param pduRequest The received [[PduRequest]]
     * @tparam P The type of [[PduResponse]] expected in response to `pduRequest`
     * @tparam R the type of [[PduRequest]]
     */
    def pduRequestReceived[P <: PduResponse, R <: PduRequest[P]](ctx: ChannelHandlerContext, pduRequest: R): Unit = {
      logger.warn(s"RECEIVED: ${pduRequest} in state ${state} - will send generic nack")
      ctx.channel().write(GenericNack(ErrorCode.ESME_RINVBNDSTS.code))
    }

    /**
     * Closes the [[StateBasedServerSession]] associated with this state. Once this method has been called a session cannot
     * be reused. Clients should release any references to it.
     */
    protected[support] def close(): Unit =
      assert(false, s"Illegal attempt to close session ${session} in state ${state}")

    /**
     * Callback invoked when this state is left.
     */
    protected[support] def onExit(): Unit = {}

    /* -------------------------------------------------------------------------------------------------------------------
       Internal
       -------------------------------------------------------------------------------------------------------------------
     */

    final protected[this] def session: StateBasedServerSession = StateBasedServerSession.this

    protected[this] def transitionTo(nextState: StateDelegate): Boolean = {
      logger.debug(s"TRANSITION:   ${this} -> ${nextState} ...")
      val succeeded = currentStateDelegateRef.compareAndSet(this, nextState)
      if (!succeeded) {
        logger.warn(s"Failed to transition from ${this} to ${nextState}: current state is in fact ${currentStateDelegate}")
      } else {
        onExit()
        nextState.onEnter()
        logger.debug(s"TRANSITIONED: ${this} -> ${nextState}")
      }
      succeeded
    }

    protected[this] def requireTransitionTo(nextState: StateDelegate): Unit = {
      assert(transitionTo(nextState), s"Expected session to be in state ${this}, but it is in state ${currentStateDelegate}")
    }

    /* -------------------------------------------------------------------------------------------------------------------
       toString() etc.
       -------------------------------------------------------------------------------------------------------------------
     */

    override def toString(): String = s"${state}(${session.getClass.getSimpleName}/${session.channel}})"
  }

  /* -------------------------------------------------------------------------------------------------------------------
     State delegates
     -------------------------------------------------------------------------------------------------------------------
   */

  /**
   * This session has just been created and is about to be opened.
   */
  protected[this] final class InitialStateDelegate(config: ServerSessionConfig)
    extends StateDelegate(Session.State.Initial, config) {

    lazy val bindTimer: Timer = config.bindConfig.bindTimer()

    protected[support] override def sessionActivated(ctx: ChannelHandlerContext): Unit = {
      assert(channel.isOpen, s"Expected channel ${ctx.channel} to be in state OPEN")
      closeOnChannelClose()
      requireTransitionTo(new OpenStateDelegate(config, bindTimeout()))
    }

    private[this] def closeOnChannelClose() {
      channel.closeFuture().addListener((_: Future[Void]) => {
        // Since this listener is executed asynchronously, we need to call stop() an the then current state,
        // which is not necessarily this state
        currentStateDelegate.close()
      })
    }

    private[this] def bindTimeout(): Timeout = {
      val closeOnBindTimeout = new TimerTask {
        override def run(timeout: Timeout): Unit = {
          logger.warn(s"Did not receive bind request after bind timeout of [${config.bindConfig.bindTimeoutMillis}] ms - SESSION ${session} WILL BE CLOSED")
          currentStateDelegate.close()
        }
      }
      bindTimer.newTimeout(closeOnBindTimeout, config.bindConfig.bindTimeoutMillis, TimeUnit.MILLISECONDS)
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * The session is waiting for a [[BaseBind]] request to initiate authentication.
   */
  protected[this] final class OpenStateDelegate(config: ServerSessionConfig, bindTimeout: Timeout)
    extends StateDelegate(Session.State.Open, config) {

    protected[support] override def onEnter(): Unit = {
      fireSessionOpened()
    }

    protected[support] override def bindRequestReceived(ctx: ChannelHandlerContext,
                                                        bindRequest: BaseBind[_ <: BaseBindResponse]): Unit = {
      assert(bindTimeout.cancel(), "Could not cancel bind timeout")
      val nextStateDelegate = new BindingStateDelegate(bindRequest, config)
      requireTransitionTo(nextStateDelegate)
    }

    protected[support] override def close(): Unit = {
      val nextStateDelegate = new ClosingStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT CLOSE: Attempt to close ${session} saw different thread changing its state to ${currentState} ")
      }
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This session has received a [[BaseBind]] request and is about to authenticate the remote system.
   *
   * @param bindRequest
   * @param config
   */
  protected[this] final class BindingStateDelegate(bindRequest: BaseBind[_ <: BaseBindResponse],
                                                   config: ServerSessionConfig)
    extends StateDelegate(Session.State.Binding(BindType.of(bindRequest)), config) {

    protected[support] override def onEnter(): Unit = {
      logger.info(s"RECEIVED: ${bindRequest} - attempting to authenticate remote ESME ...")
      // We do not want to receive any more messages until/unless we have successfully authenticated this session/ch
      channel.config().setAutoRead(false)
      attemptAuthentication(bindRequest)
    }

    private[this] def attemptAuthentication(bindRequest: BaseBind[_ <: BaseBindResponse]) {
      try {
        val authenticatedSystem = config.bindConfig.authenticator.authenticate(Credentials(bindRequest.systemId,
          bindRequest.password, channel.remoteAddress()))
        authenticatedSystem onSuccess {
          case authSystem: AuthenticatedClient => bindRequestSucceeded(bindRequest, authSystem)
        }
        authenticatedSystem onFailure {
          case t: Throwable => bindRequestFailed(bindRequest, t)
        }
      }
      catch {
        case e: Throwable => bindRequestFailed(bindRequest, e)
      }
    }

    private[this] def bindRequestSucceeded(successfulBindRequest: BaseBind[_ <: BaseBindResponse],
                                           authenticatedSystem: AuthenticatedClient) = {
      val ack = successfulBindRequest.createResponse(ErrorCode.ESME_ROK.code, config.systemId, InterfaceVersion.SMPP5_0)
      logger.info(s"AUTH SUCCEEDED: Successfully authenticated ${authenticatedSystem} using ${successfulBindRequest} - will send ${ack}")
      assert(channel.isActive, s"Expected channel ${channel} to be ACTIVE")
      channel.writeAndFlush(ack).addListener((future: Future[Void]) => {
        if (future.cause != null) {
          logger.error(s"Failed to send ack in response to successful authentication attempt ${successfulBindRequest}: ${future.cause.getMessage}", future.cause)
          // Since this listener is executed asynchronously, we need to call stop() an the then current state,
          // which is not necessarily this state
          currentStateDelegate.close()
        } else {
          logger.info(s"SENT: ${ack}")
          bind(BindType.of(successfulBindRequest), authenticatedSystem)

        }
      })
    }

    private[this] def bind(bindType: BindType, authenticatedSystem: AuthenticatedClient) = {
      val nextStateDelegate = new BoundStateDelegate(bindType, authenticatedSystem, config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT BIND: Attempt to bind ${session} saw different thread changing its state to ${currentState} ")
      }
    }

    private[this] def bindRequestFailed(bindRequest: BaseBind[_ <: BaseBindResponse], error: Throwable) = {
      val nack: PduResponse = error match {
        case t: InvalidCredentialsException => bindRequest.createResponse(ErrorCode.ESME_RBINDFAIL.code,
          config.systemId, InterfaceVersion.SMPP5_0)
        case _ => GenericNack(ErrorCode.ESME_RSYSERR.code, bindRequest.sequenceNumber.get)
      }
      logger.error(s"AUTH ERROR: Trying to authenticate remote ESME using ${bindRequest} failed: ${error.getMessage} - will send ${nack} (session will be closed)", error)
      channel.writeAndFlush(nack).addListener((future: Future[Void]) => {
        if (future.cause != null) {
          logger.error(s"Failed to send ${nack} in response to failed bind request ${bindRequest} (IGNORE): ${future.cause.getMessage}", future.cause)
        }
        // Since this listener is executed asynchronously, we need to call stop() on the then current state,
        // which is not necessarily this state
        currentStateDelegate.close()
      })
      fireSessionBindFailed()
    }

    protected[support] override def close(): Unit = {
      val nextStateDelegate = new ClosingStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT CLOSE: Attempt to close ${session} saw different thread changing its state to ${currentState} ")
      }
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This SMPP session has been successfully bound/authenticated and will accept and process incoming and outgoing SMPP
   * PDUs. This is arguably the most important and long-lived state.
   *
   * @param bindType
   * @param config
   */
  protected[this] final class BoundStateDelegate(bindType: BindType,
                                                 authenticatedSystem: AuthenticatedClient,
                                                 config: ServerSessionConfig)
    extends StateDelegate(Session.State.Bound(bindType), config) {

    /* -----------------------------------------------------------------------------------------------------------------
      Instance state
      ------------------------------------------------------------------------------------------------------------------
    */

    private[this] val inboundWindow: Window = config.inboundWindowConfig.window(id)

    private[this] val inboundPduRequestDispatcher: InboundPduRequestDispatcher = {
      val handler = config.inboundPduRequestHandlerFactory(session)
      config.inboundPduRequestDispatcherFactory(session, handler, config)
    }

    /* -------------------------------------------------------------------------------------------------------------------
      onEnter()
      -------------------------------------------------------------------------------------------------------------------
    */

    protected[support] override def onEnter(): Unit = {
      logger.debug(s"COMPLETE: bind of ${this} as ${bindType}...")
      assert(channel.attr(RemoteSystemAttributeKey).compareAndSet(null, authenticatedSystem),
        s"Expected no AuthenticatedClient to be already bound to this session/channel")
      // Register authenticated client's ID in SLF4J's MDC for logging purposes
      MDC.put(RemoteSystemIdContextKey, authenticatedSystem.id)
      // Make sure we close our inbound window when this session is closed
      channel.closeFuture().addListener((future: Future[Void]) => inboundWindow.close())
      inboundWindow.open()
      startPeriodicPing()
      reenableChannelRead()
      fireSessionBound(bindType, authenticatedSystem)
      logger.info(s"SESSION READY: ${this} successfully bound as ${bindType}")
    }

    private[this] def startPeriodicPing() {
      val periodicPing = PeriodicPing(config.periodicPingConfig)
      channel.pipeline().addLast(PeriodicPing.Name, periodicPing)
    }

    private[this] def reenableChannelRead() {
      channel.pipeline().get(classOf[SmppPduFrameDecoder]).setSingleDecode(false)
      channel.config().setAutoRead(true)
      logger.debug(s"RE-ENABLED: reading from channel ${channel} after completing bind")
    }

    /* -------------------------------------------------------------------------------------------------------------------
     Callbacks
     -------------------------------------------------------------------------------------------------------------------
   */

    /**
     * Callback invoked whenever an [[EnquireLink]] request is received. This implementation will send an [[io.garuda.codec.pdu.EnquireLinkResponse]].
     * Should sending it fail, this implementation will stop this session.
     *
     * @param ctx
     * @param enquireLinkRequest
     */
    protected[support] override def enquireLinkRequestReceived(ctx: ChannelHandlerContext, enquireLinkRequest: EnquireLink): Unit = {
      logger.debug(s"Processing ${enquireLinkRequest} ...")
      val enquireLinkResponse = enquireLinkRequest.createResponse(0)
      ctx.writeAndFlush(enquireLinkResponse).addListener((future: Future[Void]) => {
        if (future.isSuccess) {
          logger.debug(s"SENT: ${enquireLinkResponse} (pong) in response to ${enquireLinkRequest}")
        } else {
          logger.error(s"Failed to send ${enquireLinkResponse} (pong) in response to ${enquireLinkRequest} - SESSION ${session} WILL BE CLOSED: ${future.cause.getMessage}", future.cause)
          currentStateDelegate.close()
        }
      })
      logger.debug(s"Done ${enquireLinkRequest}")
    }

    override def pduRequestReceived[Res <: PduResponse, Req <: PduRequest[Res]](ctx: ChannelHandlerContext, pduRequest: Req): Unit = {
      logger.debug(s"Processing ${pduRequest} ...")
      bindType match {
        case BindType.Receiver => pduRequestReceivedInBindTypeReceiver[Res, Req](ctx, pduRequest)
        case _ => pduRequestReceivedInLegalBindType[Res, Req](ctx, pduRequest)
      }
      logger.debug(s"Done processing ${pduRequest}")
    }

    private[this] def pduRequestReceivedInBindTypeReceiver[Res <: PduResponse, Req <: PduRequest[Res]](ctx: ChannelHandlerContext, pduRequest: Req) {
      logger.warn(s"RCVD: ${pduRequest} in BindState RECEIVER - will send GenericNack, Unbind and CLOSE THIS SESSION")
      ctx.write(GenericNack(ErrorCode.ESME_RINVBNDSTS.code, pduRequest.sequenceNumber.get)).addListener((future: Future[Void]) => {
        if (!future.isSuccess) logger.warn(s"Sending GenericNack failed - IGNORE AND PROCEED WITH UNBIND AND CLOSE")
      })
      ctx.writeAndFlush(Unbind(pduRequest.sequenceNumber.get + 1)).addListener((future: Future[Void]) => {
        if (!future.isSuccess) logger.warn(s"Sending Unbind failed - IGNORE AND CLOSE")
        currentStateDelegate.close()
      })
    }

    private[this] def pduRequestReceivedInLegalBindType[Res <: PduResponse, Req <: PduRequest[Res]](ctx: ChannelHandlerContext, pduRequest: Req) {
      inboundWindow.tryAcquireSlot[Res, Req](pduRequest) match {
        case Success(slot: Slot[Res, Req]) => processPduRequest[Res, Req](ctx, pduRequest, slot)
        case Failure(satoe: SlotAcquisitionTimedOutException) => ctx.writeAndFlush(GenericNack(ErrorCode.ESME_RTHROTTLED.code, pduRequest.sequenceNumber.get, "Failed to acquire slot within timeout"))
        case Failure(t: Throwable) => ctx.writeAndFlush(GenericNack(ErrorCode.ESME_RSYSERR.code, pduRequest.sequenceNumber.get, s"Server error: ${t.getMessage}"))
      }
    }

    private[this] def processPduRequest[Res <: PduResponse, Req <: PduRequest[Res]](ctx: ChannelHandlerContext, pduRequest: Req, slot: Slot[Res, Req]) {
      slot.releaseFuture.onFailure {
        case t: Throwable => ctx.writeAndFlush(GenericNack(ErrorCode.ESME_RSYSERR.code, pduRequest.sequenceNumber.get, s"Server error: ${t.getMessage}"))
      }
      val responseFuture = inboundPduRequestDispatcher.dispatch[Res, Req](pduRequest)
      responseFuture.onSuccess {
        case response =>
          ctx.writeAndFlush(response).addListener((future: Future[Void]) => {
            if (future.isSuccess) {
              slot.release(response)
              logger.info(s"SENT: ${response} in response to ${pduRequest}")
            } else {
              logger.error(s"Failed to send ${response} in response to ${pduRequest} - SESSION ${session} WILL BE CLOSED: ${future.cause.getMessage}", future.cause)
              currentStateDelegate.close()
            }
          })
      }
      responseFuture.onFailure {
        case t =>
          val nack: GenericNack = GenericNack(ErrorCode.ESME_RSYSERR.code, pduRequest.sequenceNumber.get, s"Server error: ${t.getMessage}")
          logger.error(s"Processing ${pduRequest} failed - WILL SEND ${nack}: ${t.getMessage}", t)
          ctx.writeAndFlush(nack).addListener((future: Future[Void]) => {
            if (future.isSuccess) {
              slot.release(t)
              logger.info(s"SENT: ${nack} in response to ${pduRequest}")
            } else {
              logger.error(s"Failed to send ${nack} in response to ${pduRequest} - SESSION ${session} WILL BE CLOSED: ${future.cause.getMessage}", future.cause)
              currentStateDelegate.close()
            }
          })
      }
    }

    override protected[support] def unbindRequestReceived(ctx: ChannelHandlerContext, unbind: Unbind): Unit = {
      logger.debug(s"Processing ${unbind} ...")
      val nextStateDelegate = new UnbindingStateDelegate(unbind, config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT UNBIND: Attempt to unbind ${session} saw different thread changing its state to ${currentState}")
      }
    }

    protected[support] override def close(): Unit = {
      val nextStateDelegate = new ClosingStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT CLOSE: Attempt to close ${session} saw different thread changing its state to ${currentState}")
      }
    }

    /* -------------------------------------------------------------------------------------------------------------------
      onEnter()
     -------------------------------------------------------------------------------------------------------------------
    */

    /**
     * Callback invoked when this state is left.
     */
    override protected[support] def onExit(): Unit = {
      inboundWindow.close()
      super.onExit()
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This SMPP session has received an [[Unbind]] request and is about to send an [[UnbindResponse]].
   *
   * @param config
   */
  protected[this] final class UnbindingStateDelegate(unbind: Unbind, config: ServerSessionConfig)
    extends StateDelegate(Session.State.Unbinding, config) {

    protected[support] override def onEnter(): Unit = {
      logger.debug(s"Processing ${unbind} ...")
      // We are not interested in any more messages
      channel.pipeline().get(classOf[SmppPduFrameDecoder]).setSingleDecode(true)
      channel.config().setAutoRead(false)

      val unbindResponse = unbind.createResponse(ErrorCode.ESME_ROK.code)
      channel.writeAndFlush(unbindResponse).addListener((future: Future[Void]) => {
        if (future.isSuccess) {
          logger.debug(s"SENT: ${unbindResponse} in response to ${unbind}")
          unbindResponseSent()
        } else {
          logger.error(s"Failed to send ${unbindResponse} (pong) in response to ${unbind} - SESSION WILL BE CLOSED: ${future.cause.getMessage}", future.cause)
          currentStateDelegate.close()
        }
      })
      logger.debug(s"Done ${unbind}")
    }

    private[this] def unbindResponseSent(): Unit = {
      val nextStateDelegate = new UnboundStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT UNBIND: Attempt to unbind ${session} saw different thread changing its state to ${currentState} ")
      }
    }

    protected[support] override def close(): Unit = {
      val nextStateDelegate = new ClosingStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT CLOSE: Attempt to close ${session} saw different thread changing its state to ${currentState} ")
      }
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This SMPP session has been unbound and will be closed immediately.
   *
   * @param config
   */
  protected[this] final class UnboundStateDelegate(config: ServerSessionConfig)
    extends StateDelegate(Session.State.Unbound, config) {

    protected[support] override def onEnter(): Unit = {
      logger.info(s"${session} has been unbound and will be closed")
      fireSessionUnbound()
      currentStateDelegate.close()
    }

    protected[support] override def close(): Unit = {
      val nextStateDelegate = new ClosingStateDelegate(config)
      if (!transitionTo(nextStateDelegate)) {
        logger.warn(s"ABORT CLOSE: Attempt to stop ${session} saw different thread changing its state to ${currentState} ")
      }
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This session has been asked to close.
   *
   * @param config
   */
  protected[this] final class ClosingStateDelegate(config: ServerSessionConfig)
    extends StateDelegate(Session.State.Closing, config) {

    private[this] val alreadyClosing: AtomicBoolean = new AtomicBoolean(false)

    protected[support] override def onEnter(): Unit = {
      close()
    }

    protected[support] override def close(): Unit = {
      if (!alreadyClosing.compareAndSet(false, true)) {
        // We may be called again when the CloseFuture of our underlying channel fires
        return
      }
      logger.debug(s"CLOSING: ${session} ...")

      channel.pipeline().get(classOf[SmppPduFrameDecoder]).setSingleDecode(true)
      channel.config().setAutoRead(false)

      closeUnderlyingChannel()

      requireTransitionTo(new ClosedStateDelegate(config))

      logger.info(s"CLOSED: ${session} - THIS SESSION MUST NOT BE USED ANYMORE AND WILL BE DISCARDED")
    }

    private[this] def closeUnderlyingChannel() {
      if (!channel.isOpen) {
        // Prevent endless loop if called via ChannelListener on ch.stop()
        return
      }
      try {
        logger.debug(s"About to synchronously stop this session's underlying channel ${channel} ...")
        val start = System.currentTimeMillis
        channel.close().sync()
        val dur: Long = System.currentTimeMillis - start
        logger.debug(s"Closed this session's underlying channel ${channel} in [${dur}] ms")
      }
      catch {
        case e: Throwable => logger.error(s"Failed to close channel ${channel} - IGNORE: ${e.getMessage}", e)
      }
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * This session is closed. It cannot be reused.
   *
   * @param config
   */
  protected[this] final class ClosedStateDelegate(config: ServerSessionConfig)
    extends StateDelegate(Session.State.Closed, config) {

    protected[support] override def onEnter(): Unit = {
      channel.attr[Session](Session.AttachedSmppServerSession).set(null)
      fireSessionClosed()
    }

    protected[support] override def bindRequestReceived(ctx: ChannelHandlerContext, bindRequest: BaseBind[_ <: BaseBindResponse]): Unit = {
      assert(false, "Received bind request ${bindRequest} in closed state")
    }

    protected[support] override def close(): Unit = {
      logger.warn(s"Ignoring attempt to stop already closed session ${session}")
    }

    override protected[support] def onExit(): Unit = {
      assert(false, s"Illegal attempt to exit terminal state ${this}")
    }
  }

}
