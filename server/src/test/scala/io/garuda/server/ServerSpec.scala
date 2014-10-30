package io.garuda.server

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.cloudhopper.smpp.impl.DefaultSmppClient
import com.cloudhopper.smpp.pdu.{EnquireLink, Unbind, UnbindResp}
import com.cloudhopper.smpp.{SmppBindType, SmppSessionConfiguration}
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.InterfaceVersion
import io.garuda.server.authentication.support.FixedPasswordAuthenticator
import io.garuda.server.session.support.StateBasedServerSessionFactory
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Around, Specification}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 19.10.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
class ServerSpec extends Specification with Logging {

  logger.info(s"Initialized logging subsystem for ${this}")

  val ServerHost = "localhost"

  val ServerPort = 2775

  val ServerAddress = new InetSocketAddress(ServerHost, ServerPort)

  val Password = "Password"

  def objectUnderTest(): Server = Server.builder()
    .name("ServerSpec")
    .bindTo(ServerAddress)
    .sessionFactory(StateBasedServerSessionFactory)
    .sessionConfigBuilder()
    .bindConfigBuilder().authenticator(FixedPasswordAuthenticator(Password))
    .build()

  def createDefaultSessionConfiguration(): SmppSessionConfiguration = {
    val config = new SmppSessionConfiguration(SmppBindType.TRANSCEIVER, "id", Password)
    config.setInterfaceVersion(InterfaceVersion.SMPP5_0.asByte)
    config.setName("Test.ServerSpec.1")
    config.setHost(ServerHost)
    config.setPort(ServerPort)
    config.setWindowSize(1)
    config.setBindTimeout(400)
    config.setConnectTimeout(100)
    config.getLoggingOptions.setLogBytes(true)
    config.getLoggingOptions.setLogPdu(true)
    config
  }

  object smpp_server extends Around {
    override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
      val server = objectUnderTest()
      try {
        server.start()
        AsResult(t)
      } finally {
        server.stopAndWaitFor(1000L, TimeUnit.MILLISECONDS)
      }
    }
  }

  val testClient = new DefaultSmppClient()

  sequential

  "Server" should {

    "accept a correct bind request" in smpp_server {
      testClient.bind(createDefaultSessionConfiguration()) must not(throwA[Exception])
    }

    "respond with an enquire link response to an enquire link request" in smpp_server {
      val enquireLink = new EnquireLink
      enquireLink.setCommandStatus(0)
      enquireLink.setSequenceNumber(1)
      enquireLink.calculateAndSetCommandLength()
      val session = testClient.bind(createDefaultSessionConfiguration())

      val enquireLinkResponse = session.enquireLink(enquireLink, 200L)

      enquireLinkResponse.getCommandStatus mustEqual 0
      enquireLinkResponse.getSequenceNumber mustEqual enquireLink.getSequenceNumber
    }

    "respond with an unbind response to an unbind request" in smpp_server {
      val unbind = new Unbind
      unbind.setCommandStatus(0)
      unbind.setSequenceNumber(1)
      unbind.calculateAndSetCommandLength()
      val session = testClient.bind(createDefaultSessionConfiguration())

      val unbindResponseWindowFuture = session.sendRequestPdu(unbind, 200, true)

      unbindResponseWindowFuture.await(200) must beTrue
      val unbindResponse = unbindResponseWindowFuture.getResponse
      unbindResponse must beAnInstanceOf[UnbindResp]
    }
  }
}
