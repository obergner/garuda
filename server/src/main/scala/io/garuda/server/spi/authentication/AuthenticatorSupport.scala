package io.garuda.server.spi.authentication

import com.typesafe.scalalogging.slf4j.Logging

import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 20.10.13
 * Time: 02:27
 * To change this template use File | Settings | File Templates.
 */
trait AuthenticatorSupport extends Authenticator with Logging {

  def authenticate(creds: Credentials): Future[AuthenticatedClient] = {
    logger.info(s"AUTHENTICATE: ${creds} ...")
    val authenticatedSystemFut = doAuthenticate(creds)
    logger.info(s"AUTHENTICATE: Authentication request issued (${creds})")
    authenticatedSystemFut
  }

  protected[this] def doAuthenticate(creds: Credentials): Future[AuthenticatedClient]
}
