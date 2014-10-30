package io.garuda.server.authentication.support

import io.garuda.server.spi.authentication.{AuthenticatedClient, AuthenticatorSupport, Credentials, InvalidCredentialsException}

import scala.concurrent.Future

/**
 * Created by obergner on 26.03.14.
 */
class FixedPasswordAuthenticator(password: String) extends AuthenticatorSupport {

  override protected[this] def doAuthenticate(creds: Credentials): Future[AuthenticatedClient] = {
    if (password.equals(creds.password))
      Future.successful(AuthenticatedClient(creds.systemId))
    else
      Future.failed(new InvalidCredentialsException("Invalid credentials").fillInStackTrace())
  }
}

object FixedPasswordAuthenticator {

  def apply(password: String): FixedPasswordAuthenticator = new FixedPasswordAuthenticator(password)
}
