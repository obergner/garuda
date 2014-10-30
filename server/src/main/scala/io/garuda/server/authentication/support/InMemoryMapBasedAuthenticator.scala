package io.garuda.server.authentication.support

import io.garuda.server.spi.authentication.{AuthenticatedClient, AuthenticatorSupport, Credentials, InvalidCredentialsException}

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 20.10.13
 * Time: 02:35
 * To change this template use File | Settings | File Templates.
 */
class InMemoryMapBasedAuthenticator(systemIdToPassword: immutable.Map[String, String]) extends AuthenticatorSupport {

  protected[this] def doAuthenticate(creds: Credentials): Future[AuthenticatedClient] = {
    val authSysOpt: Option[AuthenticatedClient] = systemIdToPassword.collectFirst[AuthenticatedClient] {
      case (creds.systemId, creds.password) => AuthenticatedClient(creds.systemId)
    }
    authSysOpt match {
      case Some(authenticatedSystem) => Future.successful(authenticatedSystem)
      case _ => Future.failed(new InvalidCredentialsException("Invalid credentials").fillInStackTrace())
    }
  }
}
