package io.garuda.server.authentication.support

import io.garuda.server.spi.authentication.{AuthenticatedClient, AuthenticatorSupport, Credentials, InvalidCredentialsException}

import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.10.13
 * Time: 11:26
 * To change this template use File | Settings | File Templates.
 */
object AlwaysNackAuthenticator extends AuthenticatorSupport {

  protected[this] def doAuthenticate(creds: Credentials): Future[AuthenticatedClient] =
    Future.failed(new InvalidCredentialsException("Always Nack"))
}
