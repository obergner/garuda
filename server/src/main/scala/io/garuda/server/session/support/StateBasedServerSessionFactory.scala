package io.garuda.server.session.support

import io.garuda.server.session.ServerSessionConfig
import io.garuda.server.spi.session.SmppServerSessionFactory
import io.netty.channel.Channel

/**
 * Created by obergner on 27.03.14.
 */
object StateBasedServerSessionFactory extends SmppServerSessionFactory[StateBasedServerSession] {

  override def apply(channel: Channel, config: ServerSessionConfig): StateBasedServerSession = {
    new StateBasedServerSession(channel, config)
  }
}
