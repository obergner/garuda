package io.garuda.common.session

import io.garuda.common.spi.session.Session
import io.netty.channel.Channel
import io.netty.channel.group.{ChannelGroup, ChannelGroupFuture, ChannelGroupFutureListener, DefaultChannelGroup}
import io.netty.util.concurrent.ImmediateEventExecutor

import scala.collection.JavaConverters._
import scala.concurrent._

/**
 * Created by obergner on 29.03.14.
 */
class SessionGroup(val channelGroup: ChannelGroup)
  extends scala.collection.mutable.Set[Session]
  with scala.collection.mutable.SetLike[Session, SessionGroup] {

  private[this] val wrappedChannelGroup: scala.collection.mutable.Set[Channel] = channelGroup.asScala

  override def iterator: Iterator[Session] =
    for (sess <- wrappedChannelGroup.iterator.map[Session] {
      ch: Channel => ch.attr(Session.AttachedSmppServerSession).get()
    }
         if sess != null) yield sess

  override def contains(elem: Session): Boolean = wrappedChannelGroup.exists {
    ch => ch.attr(Session.AttachedSmppServerSession).get() == elem
  }

  override def -=(elem: Session): this.type = {
    wrappedChannelGroup -= elem.channel
    this
  }

  override def +=(elem: Session): this.type = {
    wrappedChannelGroup += elem.channel
    this
  }

  override def size: Int = super.size - 1 // Do not count serverChannel

  override def empty: SessionGroup = SessionGroup.empty

  def close(): Future[Unit] = {
    val closePromise = promise[Unit]
    channelGroup.close().addListener(new ChannelGroupFutureListener {
      override def operationComplete(future: ChannelGroupFuture): Unit = {
        if (future.isSuccess) {
          closePromise.success(Unit)
        } else {
          closePromise.failure(future.cause)
        }
      }
    })
    closePromise.future
  }
}

object SessionGroup {

  private[this] val EmptyChannelGroupName: String = "EmptyChannelGroup"

  def empty: SessionGroup =
    new SessionGroup(new DefaultChannelGroup(EmptyChannelGroupName, ImmediateEventExecutor.INSTANCE))

  def apply(channelGroup: ChannelGroup): SessionGroup = new SessionGroup(channelGroup)
}
