package io.garuda.common.session.support

import io.garuda.common.spi.session.Session
import io.garuda.common.spi.session.Session.BindType

/**
 * An implementation of [[Session.Listener]] providing empty default implementations of all
 * listener methods. These may be selectively overridden in subclasses.
 */
trait SessionListenerAdapter extends Session.Listener {

  /**
   * The [[io.garuda.common.spi.session.Session]] `session` has been opened, but is not yet bound.
   *
   * @param session The [[Session]] that has been opened
   */
  override def sessionOpened(session: Session): Unit = {}

  /**
   * An attempt to bind/authenticate [[Session]] `session` has failed.
   *
   * @param session The [[Session]] that failed authentication
   */
  override def sessionBindFailed(session: Session): Unit = {}

  /**
   * The [[Session]] `session` has been unbound.
   *
   * @param session The [[Session]] that has been unbound
   */
  override def sessionUnbound(session: Session): Unit = {}

  /**
   * The [[Session]] `session` has been successfully bound, with [[Session.BindType]] `bindType`.
   *
   * @param session The [[Session]] that has been bound
   * @param bindType The [[Session.BindType]] this `session` has been bound as
   * @param system The [[io.garuda.common.authentication.System]] representing the remote user
   */
  override def sessionBound(session: Session, bindType: BindType, system: io.garuda.common.authentication.System): Unit = {}

  /**
   * The [[Session]] `session` has been closed.
   *
   * @param session The [[Session]] that has been closed
   */
  override def sessionClosed(session: Session): Unit = {}
}
