package io.garuda.server.spi.authentication

import io.garuda.common.authentication.System
import io.garuda.server.session.SystemServerSessionConfig

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.10.13
 * Time: 23:41
 * To change this template use File | Settings | File Templates.
 */
case class AuthenticatedClient(id: String,
                               systemSmppServerSessionConfig: Option[SystemServerSessionConfig])
  extends System

object AuthenticatedClient {

  def apply(systemId: String): AuthenticatedClient = AuthenticatedClient(systemId, None)
}
