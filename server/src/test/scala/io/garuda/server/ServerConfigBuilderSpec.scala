package io.garuda.server

import java.net.InetSocketAddress

import org.specs2.mutable.Specification

/**
 * Created by obergner on 26.03.14.
 */
class ServerConfigBuilderSpec extends Specification {

  "ServerConfigBuilder" should {

    "reject null name" in {
      ServerConfigBuilder().name(null) must throwAn[IllegalArgumentException]
    }

    "reject empty name" in {
      ServerConfigBuilder().name("") must throwAn[IllegalArgumentException]
    }

    "reject null bind address" in {
      ServerConfigBuilder().bindTo(null) must throwAn[IllegalArgumentException]
    }

    "reject 0 soBacklog" in {
      ServerConfigBuilder().soBacklog(0) must throwAn[IllegalArgumentException]
    }

    "reject 0 soSendBufferSizeBytes" in {
      ServerConfigBuilder().soSendBufferSizeBytes(0) must throwAn[IllegalArgumentException]
    }

    "reject 0 soReceiveBufferSizeBytes" in {
      ServerConfigBuilder().soReceiveBufferSizeBytes(0) must throwAn[IllegalArgumentException]
    }

    "reject null boss event group" in {
      ServerConfigBuilder().bossEventLoopGroupFactory(null) must throwAn[IllegalArgumentException]
    }

    "reject null worker event group" in {
      ServerConfigBuilder().workerEventLoopGroupFactory(null) must throwAn[IllegalArgumentException]
    }

    "correctly buildInternal a default ServerConfig if no options are given" in {
      val defaultSmppServerConfig = ServerConfigBuilder().build()

      defaultSmppServerConfig.name mustEqual ServerConfig.DefaultName
      defaultSmppServerConfig.bindTo mustEqual new InetSocketAddress(ServerConfig.DefaultPort)
      defaultSmppServerConfig.soReuseAddress mustEqual ServerConfig.DefaultSoReuseAddress
      defaultSmppServerConfig.soKeepAlive mustEqual ServerConfig.DefaultSoKeepAlive
      defaultSmppServerConfig.soLinger mustEqual ServerConfig.DefaultSoLinger
      defaultSmppServerConfig.soBacklog mustEqual ServerConfig.DefaultSoBacklog
      defaultSmppServerConfig.tcpNoDelay mustEqual ServerConfig.DefaultTcpNoDelay
      //defaultSmppServerConfig.sessionFactory must be(StateBasedServerSessionFactory)
    }

    "correctly buildInternal a customr ServerConfig given all options" in {
      val expectedName = "expectedName"
      val expectedPort = 2345
      val expectedSoKeepAlive = false
      val expectedSoReuseAddr = true
      val expectedSoLinger = 312
      val expectedSoBacklog = 345
      val expectedSoSendBufSize = 200
      val expectedSoRcvBufSize = 233
      val expectedTcpNoDelay = false
      val defaultSmppServerConfig = ServerConfigBuilder()
        .name(expectedName)
        .bindTo(new InetSocketAddress(expectedPort))
        .soBacklog(expectedSoBacklog)
        .soKeepAlive(expectedSoKeepAlive)
        .soLinger(expectedSoLinger)
        .soReuseAddress(expectedSoReuseAddr)
        .soReceiveBufferSizeBytes(expectedSoRcvBufSize)
        .soSendBufferSizeBytes(expectedSoSendBufSize)
        .tcpNoDelay(expectedTcpNoDelay)
        .build()

      defaultSmppServerConfig.name mustEqual expectedName
      defaultSmppServerConfig.bindTo mustEqual new InetSocketAddress(expectedPort)
      defaultSmppServerConfig.soReuseAddress mustEqual Some(expectedSoReuseAddr)
      defaultSmppServerConfig.soKeepAlive mustEqual Some(expectedSoKeepAlive)
      defaultSmppServerConfig.soLinger mustEqual Some(expectedSoLinger)
      defaultSmppServerConfig.soBacklog mustEqual Some(expectedSoBacklog)
      defaultSmppServerConfig.tcpNoDelay mustEqual Some(expectedTcpNoDelay)
    }
  }
}
