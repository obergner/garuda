package io.garuda.server.session.support

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu._
import io.garuda.codec.util.HexHelpers
import io.garuda.codec.{ErrorCode, SmppPduDecoder, SmppPduFrameDecoder}
import io.garuda.common.concurrent.CallerThreadExecutionContext
import io.garuda.common.session.support.SessionListenerAdapter
import io.garuda.common.spi.session.Session
import io.garuda.common.spi.session.Session.BindType
import io.garuda.server.authentication.support.FixedPasswordAuthenticator
import io.garuda.server.session.ServerSessionChannelHandler
import io.garuda.server.spi.session.{InboundPduRequestHandler, InboundPduRequestHandlerFactory}
import io.garuda.server.{ServerConfig, ServerConfigBuilder}
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.local.LocalAddress
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.AroundOutside
import org.specs2.time.NoTimeConversions

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by obergner on 29.03.14.
 */
class StateBasedServerSessionPduRequestProcessingSpec extends Specification with NoTimeConversions with Logging {

  io.garuda.common.logging.slf4j.setupNettyInternalLogger()
  logger.info(s"Initializing logging subsystem for test ${this}")

  def embeddedChannel(smppServerConfig: ServerConfig): EmbeddedChannel = {
    new EmbeddedChannel(new SmppPduFrameDecoder(new SmppPduDecoder), new
        ServerSessionChannelHandler(smppServerConfig))
  }

  case class embedded_channel(smppServerConfig: ServerConfig) extends AroundOutside[EmbeddedChannel] {

    override def outside: EmbeddedChannel = embeddedChannel(smppServerConfig)

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        smppServerConfig.builderContext.open()
        AsResult(a)
      } finally {
        outside.close()
        DelegatingInboundPduRequestHandler.reset()
        smppServerConfig.builderContext.close()
      }
    }
  }

  def bindTransceiverRequest(password: String): BindTransceiver = BindTransceiver(1,
    "spec",
    password,
    "systemType",
    InterfaceVersion.SMPP5_0,
    Address(Ton.Alphanumeric, Npi.IsdnNumberingPlan, "abcdefgh"))

  def bindReceiverRequest(password: String): BindReceiver = BindReceiver(1,
    "spec",
    password,
    "systemType",
    InterfaceVersion.SMPP5_0,
    Address(Ton.Alphanumeric, Npi.IsdnNumberingPlan, "abcdefgh"))

  object DelegatingInboundPduRequestHandler extends InboundPduRequestHandler {

    private[this] val _delegate: AtomicReference[InboundPduRequestHandler] = new
        AtomicReference[InboundPduRequestHandler]()

    def delegate(value: InboundPduRequestHandler): Unit = this._delegate.set(value)

    override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
      assert(this._delegate.get() != null, "Delegate MUST NOT be null")
      this._delegate.get()[Res, Req](pduRequest)
    }

    def reset(): Unit = this._delegate.set(null)
  }

  val delegatingInboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[InboundPduRequestHandler] =
    Function.const[InboundPduRequestHandler, Session](DelegatingInboundPduRequestHandler)(_)

  val validPassword = "valid"

  sequential

  "StateBaseSmppServerSession as a PDU request handler" should {

    "forward received SubmitSm to configured InboundPduRequestHandler" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server-1"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30001),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        val submitSmForwarded = new CountDownLatch(1)
        val recordingInboundPduRequestHandler: InboundPduRequestHandler = new InboundPduRequestHandler() {
          override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
            if (pduRequest.equals(submitSm)) {
              submitSmForwarded.countDown()
              Future.successful[Res](submitSm.createResponse(0, "message-id-1").asInstanceOf[Res])
            } else {
              Future.failed(new IllegalArgumentException(s"Unexpected PDU request: ${pduRequest}"))
            }
          }
        }
        DelegatingInboundPduRequestHandler.delegate(recordingInboundPduRequestHandler)

        embeddedCh.bind(new LocalAddress("spec-channel"))

        val validBindRequest = bindTransceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        embeddedCh.writeInbound(submitSm)
        submitSmForwarded.await(200, TimeUnit.MILLISECONDS) must beTrue
      }
    }

    "send back SubmitSmResp produced by InboundPduRequestHandler to connected client" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30002),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        val recordingInboundPduRequestHandler: InboundPduRequestHandler = new InboundPduRequestHandler() {
          override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
            if (pduRequest.equals(submitSm)) {
              Future.successful[Res](submitSm.createResponse(0, "message-id-1").asInstanceOf[Res])
            } else {
              Future.failed(new IllegalArgumentException(s"Unexpected PDU request: ${pduRequest}"))
            }
          }
        }
        DelegatingInboundPduRequestHandler.delegate(recordingInboundPduRequestHandler)

        embeddedCh.bind(new LocalAddress("spec-server-2"))

        val validBindRequest = bindTransceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        embeddedCh.writeInbound(submitSm)

        // Skip BindTransceiverResponse we got in response to our BindTransceiver request
        embeddedCh.readOutbound()
        embeddedCh.readOutbound() must beAnInstanceOf[SubmitSmResponse]
      }
    }

    "send back GenericNack to connected client if InboundPduRequestHandler fails" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30003),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        val recordingInboundPduRequestHandler: InboundPduRequestHandler = new InboundPduRequestHandler() {
          override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
            if (pduRequest.equals(submitSm)) {
              Future.failed(new IllegalArgumentException(s"FAIL ON PURPOSE: ${pduRequest}"))
            } else {
              Future.successful[Res](submitSm.createResponse(0, "message-id-1").asInstanceOf[Res])
            }
          }
        }
        DelegatingInboundPduRequestHandler.delegate(recordingInboundPduRequestHandler)

        embeddedCh.bind(new LocalAddress("spec-server-3"))

        val validBindRequest = bindTransceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        embeddedCh.writeInbound(submitSm)

        // Skip BindTransceiverResponse we got in response to our BindTransceiver request
        embeddedCh.readOutbound()
        embeddedCh.readOutbound() must beAnInstanceOf[GenericNack]
      }
    }

    "send back GenericNack to connected client if slot in inbound window expires" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .inboundWindowConfigBuilder()
      .slotExpirationTimeout(10 milliseconds)
      .slotExpirationMonitoringInterval(50 milliseconds)
      .end()
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30004),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        val submitSmFailed = new CountDownLatch(1)
        val recordingInboundPduRequestHandler: InboundPduRequestHandler = new InboundPduRequestHandler() {
          override def apply[Res <: PduResponse, Req <: PduRequest[Res]](pduRequest: Req): Future[Res] = {
            Thread.sleep(200)
            submitSmFailed.countDown()
            Future.successful[Res](submitSm.createResponse(0, "message-id-1").asInstanceOf[Res])
          }
        }
        DelegatingInboundPduRequestHandler.delegate(recordingInboundPduRequestHandler)

        embeddedCh.bind(new LocalAddress("spec-server-4"))
        val objectUnderTest = embeddedCh.attr(Session.AttachedSmppServerSession).get()

        val sessionHasBeenBound = new CountDownLatch(1)
        val bindListener = new SessionListenerAdapter() {
          override def sessionBound(session: Session, bindType: BindType, system: io.garuda.common.authentication.System): Unit = sessionHasBeenBound.countDown()
        }
        objectUnderTest.addListener(bindListener)

        val validBindRequest = bindTransceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)
        sessionHasBeenBound.await(200, TimeUnit.MILLISECONDS) must beTrue

        embeddedCh.writeInbound(submitSm)
        submitSmFailed.await(200, TimeUnit.MILLISECONDS) must beTrue

        // Skip BindTransceiverResponse we got in response to our BindTransceiver request
        embeddedCh.readOutbound()
        val expectedGenericNack = embeddedCh.readOutbound()
        expectedGenericNack must beAnInstanceOf[GenericNack]
      }
    }

    "send back a GenericNack to a connected client bound as a Receiver" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30002),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        embeddedCh.bind(new LocalAddress("spec-server-5"))

        val validBindRequest = bindReceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        embeddedCh.writeInbound(submitSm)

        // Skip BindTransceiverResponse we got in response to our BindReceiver request
        embeddedCh.readOutbound()
        val resp = embeddedCh.readOutbound()
        resp must beAnInstanceOf[GenericNack]

        val genericNack = resp.asInstanceOf[GenericNack]
        genericNack.commandStatus must_== ErrorCode.ESME_RINVBNDSTS.code
      }
    }

    "send back an Unbind after a GenericNack to a connected client bound as a Receiver" in embedded_channel(ServerConfigBuilder()
      .bindTo(new LocalAddress("spec-server"))
      .sessionConfigBuilder()
      .userExecutionContext(CallerThreadExecutionContext)
      .inboundPduRequestHandlerFactory(delegatingInboundPduRequestHandlerFactory)
      .bindConfigBuilder()
      .authenticator(FixedPasswordAuthenticator(validPassword))
      .build()) {
      (embeddedCh: EmbeddedChannel) => {
        val submitSm = SubmitSm(Some(30002),
          "",
          Address(Ton.International, Npi.IsdnNumberingPlan, "40404"),
          Address(Ton.International, Npi.IsdnNumberingPlan, "44951361920"),
          0x00,
          0x00,
          0x00,
          "",
          "",
          0x01,
          0x00,
          0x00,
          0x00,
          HexHelpers.hexStringToByteArray("4024232125262f3a"))
        embeddedCh.bind(new LocalAddress("spec-server-5"))

        val validBindRequest = bindReceiverRequest(validPassword)
        embeddedCh.writeInbound(validBindRequest)

        embeddedCh.writeInbound(submitSm)

        // Skip BindTransceiverResponse we got in response to our BindReceiver request
        embeddedCh.readOutbound()
        // Also skip GenericNack
        embeddedCh.readOutbound()
        val resp = embeddedCh.readOutbound()
        resp must beAnInstanceOf[Unbind]
      }
    }
  }
}
