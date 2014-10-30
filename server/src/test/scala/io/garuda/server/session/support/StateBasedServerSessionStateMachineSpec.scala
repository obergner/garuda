package io.garuda.server.session.support

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu._
import io.garuda.codec.{SmppPduDecoder, SmppPduFrameDecoder}
import io.garuda.common.concurrent.CallerThreadExecutionContext
import io.garuda.common.session.support.SessionListenerAdapter
import io.garuda.common.spi.session.Session
import io.garuda.common.spi.session.Session.BindType
import io.garuda.server.authentication.support.FixedPasswordAuthenticator
import io.garuda.server.session.ServerSessionChannelHandler
import io.garuda.server.{ServerConfig, ServerConfigBuilder}
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.local.LocalAddress
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.AroundOutside

/**
 * Created by obergner on 29.03.14.
 */
class StateBasedServerSessionStateMachineSpec extends Specification with Logging {

  io.garuda.common.logging.slf4j.setupNettyInternalLogger()
  logger.info(s"Initializing logging subsystem for test ${this}")

  def embeddedChannel(smppServerConfig: ServerConfig): EmbeddedChannel = {
    new EmbeddedChannel(new SmppPduFrameDecoder(new SmppPduDecoder), new
        ServerSessionChannelHandler(smppServerConfig))
  }

  case class embedded_channel(smppServerConfig: ServerConfig) extends AroundOutside[EmbeddedChannel] {

    private[this] lazy val objectUnderTest = embeddedChannel(smppServerConfig)

    override def outside: EmbeddedChannel = objectUnderTest

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        AsResult(a)
      } finally {
        outside.close()
      }
    }
  }

  def bindRequest(password: String): BindTransceiver = BindTransceiver(1,
    "spec",
    password,
    "systemType",
    InterfaceVersion.SMPP5_0,
    Address(Ton.Alphanumeric, Npi.IsdnNumberingPlan, "abcdefgh"))

  val validPassword = "valid"

  "StateBasedServerSession as a state machine" should {

    "transition from state Initial to state Open when being activated" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        embeddedCh.bind(new LocalAddress("spec-channel"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()
        objectUnderTest.currentState must be(Session.State.Open)
      }
    }

    "transition from state Open to state Closed when receiving a failed bind request" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        embeddedCh.bind(new LocalAddress("spec-channel"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()

        val sessionHasBeenClosed = new CountDownLatch(1)
        val closeListener = new SessionListenerAdapter() {
          override def sessionClosed(session: Session): Unit = sessionHasBeenClosed.countDown()
        }
        objectUnderTest.addListener(closeListener)

        objectUnderTest.currentState must be(Session.State.Open)

        val invalidBindRequest = bindRequest("invalid")
        embeddedCh.writeInbound(invalidBindRequest)

        sessionHasBeenClosed.await(200, TimeUnit.MILLISECONDS) must beTrue
      }
    }

    "transition from state Open to state Bound when receiving a successful bind request" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword)).build()) {
      (embeddedCh: EmbeddedChannel) => {
        embeddedCh.bind(new LocalAddress("spec-channel"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()

        val sessionHasBeenBound = new CountDownLatch(1)
        val bindListener = new SessionListenerAdapter() {
          override def sessionBound(session: Session, bindType: BindType, system: io.garuda.common.authentication.System): Unit = sessionHasBeenBound.countDown()
        }
        objectUnderTest.addListener(bindListener)

        objectUnderTest.currentState must be(Session.State.Open)

        val validBindRequest = bindRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        sessionHasBeenBound.await(200, TimeUnit.MILLISECONDS) must beTrue
        objectUnderTest.currentState mustEqual Session.State.Bound(Session.BindType.Transceiver)
      }
    }

    "transition from state Open to state Closed when underlying channel is closed" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        embeddedCh.bind(new LocalAddress("spec-channel"))
        embeddedCh.bind(new LocalAddress("spec-channel"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()

        val sessionHasBeenClosed = new CountDownLatch(1)
        val closeListener = new SessionListenerAdapter() {
          override def sessionClosed(session: Session): Unit = sessionHasBeenClosed.countDown()
        }
        objectUnderTest.addListener(closeListener)

        objectUnderTest.currentState must be(Session.State.Open)

        embeddedCh.close()

        sessionHasBeenClosed.await(200, TimeUnit.MILLISECONDS) must beTrue
        objectUnderTest.currentState must be(Session.State.Closed)

      }
    }

    "transition from state Bound to state Closed when underlying channel is closed" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .bindConfigBuilder().authenticator(FixedPasswordAuthenticator(validPassword)).build()) {
      (embeddedCh: EmbeddedChannel) => {
        embeddedCh.bind(new LocalAddress("spec-channel"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()

        val sessionHasBeenBound = new CountDownLatch(1)
        val sessionHasBeenClosed = new CountDownLatch(1)
        val closeListener = new SessionListenerAdapter() {
          override def sessionBound(session: Session, bindType: BindType, system: io.garuda.common.authentication.System): Unit =
            sessionHasBeenBound.countDown()

          override def sessionClosed(session: Session): Unit = sessionHasBeenClosed.countDown()
        }
        objectUnderTest.addListener(closeListener)

        objectUnderTest.currentState must be(Session.State.Open)

        val validBindRequest = bindRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        sessionHasBeenBound.await(200, TimeUnit.MILLISECONDS) must beTrue
        objectUnderTest.currentState mustEqual Session.State.Bound(Session.BindType.Transceiver)

        embeddedCh.close()

        sessionHasBeenClosed.await(200, TimeUnit.MILLISECONDS) must beTrue
        objectUnderTest.currentState must be(Session.State.Closed)
      }
    }
  }
}
