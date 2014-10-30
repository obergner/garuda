package io.garuda.server.session.authentication.support

import java.net.InetSocketAddress

import io.garuda.server.authentication.support.InMemoryMapBasedAuthenticator
import io.garuda.server.spi.authentication.{AuthenticatedClient, Credentials, InvalidCredentialsException}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 20.10.13
 * Time: 02:47
 * To change this template use File | Settings | File Templates.
 */
class InMemoryMapBasedAuthenticatorSpec extends Specification with NoTimeConversions {

  "InMemoryMapBasedAuthenticator" should {

    "reject invalid id/Password combination" in {
      val knownCredentials = immutable.Map("systemId1" -> "password1", "systemId2" -> "password2")
      val objectUnderTest = new InMemoryMapBasedAuthenticator(knownCredentials)

      val system = objectUnderTest.authenticate(Credentials("unknown", "wrong", new InetSocketAddress(80)))

      Await.result(system, 10 milliseconds) must throwAn[InvalidCredentialsException]
    }

    "accept valid id/Password combination" in {
      val knownCredentials = immutable.Map("systemId1" -> "password1", "systemId2" -> "password2")
      val objectUnderTest = new InMemoryMapBasedAuthenticator(knownCredentials)

      val system = objectUnderTest.authenticate(Credentials("systemId1", "password1", new InetSocketAddress(80)))

      Await.result(system, 10 milliseconds) mustEqual AuthenticatedClient("systemId1")
    }
  }
}
