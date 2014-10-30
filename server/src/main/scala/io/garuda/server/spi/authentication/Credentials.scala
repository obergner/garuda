package io.garuda.server.spi.authentication

import java.net.SocketAddress

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.10.13
 * Time: 23:34
 * To change this template use File | Settings | File Templates.
 */
case class Credentials(systemId: String, password: String, clientIp: SocketAddress)
