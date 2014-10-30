package io.garuda.server.spi.authentication

import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 20.10.13
 * Time: 02:24
 * To change this template use File | Settings | File Templates.
 */
trait Authenticator {

  def authenticate(creds: Credentials): Future[AuthenticatedClient]
}
