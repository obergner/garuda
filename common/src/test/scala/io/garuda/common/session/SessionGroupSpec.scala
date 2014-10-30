package io.garuda.common.session

import io.garuda.common.spi.session.Session.State
import io.garuda.common.spi.session.{Session, SessionChannelAdapter}
import io.netty.channel.Channel
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.ImmediateEventExecutor
import org.specs2.mutable.{Specification, Tags}
import org.specs2.time.NoTimeConversions

/**
 * Created by obergner on 29.03.14.
 */
class SessionGroupSpec extends Specification with NoTimeConversions with Tags {

  private[this] class SpecSession(override val channel: Channel) extends Session {

    override def id: String = channel.toString

    override def currentState: State = State.Open

    override def authenticatedSystem: Option[io.garuda.common.authentication.System] = None

    override def close(): Unit = {}

    override def channelAdapter: SessionChannelAdapter = null
  }

  private[this] val loggingHandler = new LoggingHandler()

  def newSmppServerSession(): Session = {
    val embeddedChannel = new EmbeddedChannel(loggingHandler)
    embeddedChannel.attr(Session.AttachedSmppServerSession).set(new SpecSession(embeddedChannel))
    embeddedChannel.attr(Session.AttachedSmppServerSession).get()
  }

  def newChannelGroup(): ChannelGroup = new DefaultChannelGroup("spec", ImmediateEventExecutor.INSTANCE)

  "SessionGroup +=" should {

    "add associated Channel to wrapped ChannelGroup" in {
      val channelGroup = newChannelGroup()
      val session = newSmppServerSession()

      val objectUnderTest = new SessionGroup(channelGroup)

      objectUnderTest += session

      channelGroup.contains(session.channel) must beTrue
    } tag "unit"
  }

  "SessionGroup -=" should {

    "remove associated Channel from wrapped ChannelGroup" in {
      val channelGroup = newChannelGroup()
      val session = newSmppServerSession()
      channelGroup.add(session.channel)

      val objectUnderTest = new SessionGroup(channelGroup)

      objectUnderTest -= session

      channelGroup.contains(session.channel) must beFalse
    } tag "unit"
  }

  "SessionGroup contains" should {

    "return true if wrapped ChannelGroup contains associated Channel" in {
      val channelGroup = newChannelGroup()
      val session = newSmppServerSession()
      channelGroup.add(session.channel)

      val objectUnderTest = new SessionGroup(channelGroup)

      objectUnderTest.contains(session) must beTrue
    } tag "unit"
  }

  "SessionGroup iterator" should {

    "contain all added sessions" in {
      val channelGroup = newChannelGroup()
      val sessionOne = newSmppServerSession()
      channelGroup.add(sessionOne.channel)
      val sessionTwo = newSmppServerSession()
      channelGroup.add(sessionTwo.channel)
      val expectedSessionSet = scala.collection.mutable.Set[Session](sessionOne, sessionTwo)

      val objectUnderTest = new SessionGroup(channelGroup)

      val actualSessionSet = scala.collection.mutable.Set[Session]()
      for (sess <- objectUnderTest.iterator) {
        actualSessionSet add sess
      }

      actualSessionSet mustEqual expectedSessionSet
    } tag "unit"

    "skip channels with no associated session" in {
      val channelGroup = newChannelGroup()
      val sessionOne = newSmppServerSession()
      channelGroup.add(sessionOne.channel)
      val sessionTwo = newSmppServerSession()
      channelGroup.add(sessionTwo.channel)
      val channelWithoutSession = new EmbeddedChannel(loggingHandler)
      channelGroup.add(channelWithoutSession)
      val expectedSessionSet = scala.collection.mutable.Set[Session](sessionOne, sessionTwo)

      val objectUnderTest = new SessionGroup(channelGroup)

      val actualSessionSet = scala.collection.mutable.Set[Session]()
      for (sess <- objectUnderTest.iterator) {
        actualSessionSet add sess
      }

      actualSessionSet mustEqual expectedSessionSet
    } tag "unit"
  }
}
