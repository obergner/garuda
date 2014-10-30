package io.garuda.common.spi.session

import io.garuda.codec.pdu._
import io.garuda.common.authentication.RemoteSystem
import io.garuda.common.spi.session.Session.Listener
import io.netty.channel.Channel
import io.netty.util.AttributeKey

import scala.collection.JavaConversions._

/**
 * Created by obergner on 28.09.14.
 */
object Session {

  val AttachedSmppServerSession: AttributeKey[Session] = AttributeKey.valueOf[Session]("smppServerSession")

  sealed abstract class BindType

  object BindType {

    case object Transmitter extends BindType

    case object Transceiver extends BindType

    case object Receiver extends BindType

    def of(bindRequest: BaseBind[_ <: BaseBindResponse]): BindType = bindRequest match {
      case _: BindTransmitter => Transmitter
      case _: BindTransceiver => Transceiver
      case _: BindReceiver => Receiver
    }
  }

  sealed abstract class State(private val state: Int) extends Ordered[State] {

    def compare(that: State): Int = this.state.compareTo(that.state)
  }

  object State {

    /**
     * This [[Session]] has been start, but is not yet connected.
     */
    case object Initial extends State(0)

    /**
     * The ESME has established a network connection to the SMSC/MC, represented by this [[Session]].
     * This [[Session]] has not yet received a [[io.garuda.codec.pdu.BaseBind]].
     */
    case object Open extends State(1)

    /**
     * This [[Session]] has received a [[io.garuda.codec.pdu.BaseBind]] by the remote ESME.
     * It has not yet responded with a [[io.garuda.codec.pdu.BaseBindResponse]].
     */
    case class Binding(as: BindType) extends State(2)

    /**
     * This [[Session]] has responded with a [[io.garuda.codec.pdu.BaseBindResponse]]
     * that indicates a successful authentication of the remote ESME. This session is ready to serve requests. If bound as a
     * transceiver it is also ready to send out requests to the remote ESME.
     */
    case class Bound(as: BindType) extends State(3)

    /**
     * This [[Session]] is in the process of being unbound:
     *
     * Either, the remote ESME has send an [[io.garuda.codec.pdu.Unbind]] request and we haven't yet responded with
     * an [[io.garuda.codec.pdu.UnbindResponse]].
     *
     * Or we have sent an [[io.garuda.codec.pdu.Unbind]] request but haven't yet received an [[io.garuda.codec.pdu.UnbindResponse]]
     * from the remote ESME.
     */
    case object Unbinding extends State(4)

    /**
     * This [[Session]] is unbound:
     *
     * Either, we have sent out an [[io.garuda.codec.pdu.UnbindResponse]] to the remote ESME, answering its previously
     * sent [[io.garuda.codec.pdu.Unbind]] request.
     *
     * Or we have received an [[io.garuda.codec.pdu.UnbindResponse]] from the remote ESME to an [[io.garuda.codec.pdu.Unbind]]
     * request we have previously sent.
     *
     * The network connection to the remote ESME is still established.
     */
    case object Unbound extends State(5)

    /**
     * We are about to stop the underlying [[io.netty.channel.Channel]], i.e. this session's network connection to the remote ESME.
     */
    case object Closing extends State(6)

    /**
     * The network connection to the remote ESME has been closed. This [[Session]]
     * should be destroyed. It must not be accessed anymore.
     */
    case object Closed extends State(7)

  }

  /**
   * A listener for [[Session]] lifecycle events.
   */
  trait Listener {

    /**
     * The [[Session]] `session` has been opened, but is not yet bound.
     *
     * @param session The [[Session]] that has been opened
     */
    def sessionOpened(session: Session): Unit

    /**
     * The [[Session]] `session` has been successfully bound, with [[Session.BindType]] `bindType`.
     *
     * @param session The [[Session]] that has been bound
     * @param bindType The [[Session.BindType]] this `session` has been bound as
     * @param system The [[RemoteSystem]] representing the remote user
     */
    def sessionBound(session: Session, bindType: Session.BindType, system: RemoteSystem): Unit

    /**
     * An attempt to bind/authenticate [[Session]] `session` has failed.
     *
     * @param session The [[Session]] that failed authentication
     */
    def sessionBindFailed(session: Session): Unit

    /**
     * The [[Session]] `session` has been unbound.
     *
     * @param session The [[Session]] that has been unbound
     */
    def sessionUnbound(session: Session): Unit

    /**
     * The [[Session]] `session` has been closed.
     *
     * @param session The [[Session]] that has been closed
     */
    def sessionClosed(session: Session): Unit
  }

}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 26.10.13
 * Time: 18:35
 * To change this template use File | Settings | File Templates.
 */
trait Session {

  private[this] val listeners: java.util.concurrent.CopyOnWriteArraySet[Listener] =
    new java.util.concurrent.CopyOnWriteArraySet[Listener]()

  /**
   * A unique ID identifying this [[Session]]. Typically, this will be just the underlying [[io.netty.channel.Channel]]'s
   * `String` representation.
   *
   * @return This [[Session]]'s unique ID
   */
  def id: String

  /**
   * The Netty [[io.netty.channel.Channel]] this [[Session]] is bind to.
   *
   * @return The Netty [[io.netty.channel.Channel]] this [[Session]] is bind to.
   */
  def channel: Channel

  /**
   * This [[Session]]'s current [[Session.State]].
   *
   * @return This [[Session]]'s current [[Session.State]].
   */
  def currentState: Session.State

  /**
   * The remote [[RemoteSystem]] this [[Session]] has been bound to.
   *
   * @return The remote [[RemoteSystem]] this [[Session]] has been bound to
   */
  def authenticatedSystem: Option[RemoteSystem]

  /**
   * Closes the underlying [[io.netty.channel.Channel]], effectively terminating this [[Session]].
   *
   * Also, release all internal resources (if any). After calling this method, any references to this session should be released.
   */
  def close(): Unit

  /**
   * Return this [[Session]]'s [[SessionChannelAdapter]].
   *
   * @return This [[Session]]'s [[SessionChannelAdapter]]
   */
  def channelAdapter: SessionChannelAdapter

  /**
   * Add [[Listener]] `listener` to this session.
   *
   * @param listener The [[Listener]] to add, must not be `null`
   * @return `true` if `listener` was added, `false` otherwise because `listener` was already added before
   */
  def addListener(listener: Listener): Boolean = {
    require(listener != null, "Argument 'listener' must not be null")
    this.listeners.add(listener)
  }

  /**
   * Remove [[Listener]] `listener` from this session.
   *
   * @param listener The [[Listener]] to remove, must not be `null`
   * @return `true` if `listener` was removed, `false` otherwise because `listener` was already removed/never added
   */
  def removeListener(listener: Listener): Boolean = {
    require(listener != null, "Argument 'listener' must not be null")
    this.listeners.remove(listener)
  }

  /**
   * Clear all [[Listener]]s.
   */
  def clearListeners(): Unit = {
    this.listeners.clear()
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Fire events
  // -------------------------------------------------------------------------------------------------------------------

  protected[this] def fireSessionOpened(): Unit = {
    this.listeners.foreach(_.sessionOpened(this))
  }

  protected[this] def fireSessionBound(bindType: Session.BindType, system: io.garuda.common.authentication.RemoteSystem): Unit = {
    this.listeners.foreach(_.sessionBound(this, bindType, system))
  }

  protected[this] def fireSessionBindFailed(): Unit = {
    this.listeners.foreach(_.sessionBindFailed(this))
  }

  protected[this] def fireSessionUnbound(): Unit = {
    this.listeners.foreach(_.sessionUnbound(this))
  }

  protected[this] def fireSessionClosed(): Unit = {
    this.listeners.foreach(_.sessionClosed(this))
  }
}
