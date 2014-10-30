package io.garuda.server

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.common.metrics.ManagedMetricsSupport
import io.garuda.common.session.SessionGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.{Future, FutureListener, GlobalEventExecutor}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * `SMPP` server.
 *
 * @param serverConfig Configuration, must not be `null`
 * @throws IllegalArgumentException If `serverConfig` is `null`
 */
@throws[IllegalArgumentException]("If serverConfig is null")
class Server(serverConfig: ServerConfig)
  extends ManagedMetricsSupport
  with Logging {
  require(serverConfig != null, "Argument 'serverConfig' must not be null")

  io.garuda.common.logging.slf4j.setupNettyInternalLogger()

  protected[this] val metricRegistry: MetricRegistry = serverConfig.metricRegistry

  val sessions: SessionGroup = SessionGroup(new
      DefaultChannelGroup("all-sessions", GlobalEventExecutor.INSTANCE))
  metrics.gauge[Int]("all-sessions-count") {
    sessions.size
  }

  private[this] val isStarted: CountDownLatch = new CountDownLatch(1)

  private[this] val isStopped: CountDownLatch = new CountDownLatch(3)

  /**
   * Start this server, binding it to its configured server socket.
   *
   * @throws IllegalStateException If already started or already stopped
   */
  @throws[IllegalStateException]("If already started or already stopped")
  final def start(): Unit = {
    if (isStarted.getCount <= 0) {
      throw new IllegalStateException("Server is already started")
    }
    if (isStopped.getCount <= 1) {
      throw new
          IllegalStateException("Server has been stopped or is stopping. A Server cannot be restarted once stopped.")
    }
    try {
      val startTime: Long = System.currentTimeMillis()
      logger.info(s"START:   ${this} ...")

      serverConfig.builderContext.open()
      val serverBootstrap = ServerBootstrapFactory(serverConfig, sessions.channelGroup)
      val serverChannel = serverBootstrap.bind(serverConfig.bindTo).sync().channel()
      sessions.channelGroup.add(serverChannel)

      isStarted.countDown()
      logger.info(s"STARTED: ${this} in [${System.currentTimeMillis - startTime}] ms")
    } catch {
      case t: Throwable => onStartFailed(t)
    }
  }

  private[this] def onStartFailed(t: Throwable) {
    logger.error(s"START: ${this} -> FAILED: ${t.getMessage}", t)
    gracefullyShutdownEventLoopGroups()
    logger.info(s"Boss and Worker Event Loop Groups shut down after failing to start ${this}")
    throw t
  }

  private[this] def gracefullyShutdownEventLoopGroups(): Unit = {
    serverConfig.bossEventLoopGroup.shutdownGracefully().addListener(new FutureListener[Any] {
      def operationComplete(future: Future[Any]): Unit = {
        if (future.isSuccess) {
          isStopped.countDown()
          logger.info(s"Boss Event Loop Group [${serverConfig.bossEventLoopGroup}] has been shut down")
        } else {
          logger.warn(s"Failed to shut down Boss Event Loop Group [${serverConfig.bossEventLoopGroup}]: ${future.cause.getMessage}", future.cause)
        }
      }
    })
    serverConfig.workerEventLoopGroup.shutdownGracefully().addListener(new FutureListener[Any] {
      def operationComplete(future: Future[Any]): Unit = {
        if (future.isSuccess) {
          isStopped.countDown()
          logger.info(s"Worker Event Loop Group [${serverConfig.workerEventLoopGroup}] has been shut down")
        } else {
          logger.warn(s"Failed to shut down Worker Event Loop Group [${serverConfig.workerEventLoopGroup}]: ${future.cause.getMessage}", future.cause)
        }
      }
    })
  }

  /**
   * Wait until this server is started. Return `true` if this server could be started within the specified `duration`,
   * `false` otherwise.
   *
   * @param duration How long to wait for this server to be started
   * @param timeUnit [[TimeUnit]]
   * @return `true` if this server could be started within the specified `duration`, `false` otherwise
   */
  final def waitUntilStartedFor(duration: Long, timeUnit: TimeUnit): Boolean = {
    isStarted.await(duration, timeUnit)
  }

  /**
   * Stop this server, releasing all resources. This method is '''asynchronous''', i.e. all live [[io.garuda.server.spi.session.ServerSession]]s,
   * all thread pools a.s.f. will be stopped in the background.
   *
   * @throws IllegalStateException If this server has already been stopped or is currently stopping
   */
  @throws[IllegalStateException]("If this server has already been stopped or is currently stopping")
  final def stop(): Unit = {
    if (isStopped.getCount <= 1) {
      throw new IllegalStateException("SMPP server [" + this + "] has already been stopped or is currently stopping")
    }
    val startTime: Long = System.currentTimeMillis
    logger.info(s"STOP:    ${this} ...")
    closeAllSessions()
    gracefullyShutdownEventLoopGroups()
    serverConfig.builderContext.close()
    unregisterAllMetrics()
    logger.info(s"Initiated shutdown of Boss and Worker Event Group - they will be stopped asynchronously in the background")

    logger.info(s"STOPPED: ${this} in [${System.currentTimeMillis - startTime}] ms")
  }

  private[this] final def closeAllSessions(): Unit = {
    /*
     * FIXME: The completion callbacks will be executed on a thread from the default Scala Execution context. This seems
     * redundant, since the ChannelGroupFuture wrapped by the Scala Future returned from sessions.stop() will be
     * completed by a Netty managed thread.
     */
    logger.info(s"About to stop ${sessions.size} session(s)/channel(s) - it/they will be closed asynchronously in the background")
    val allSessionsClosed = sessions.close()
    allSessionsClosed onSuccess {
      case _ =>
        logger.info(s"All sessions/channels have been successfully closed")
        isStopped.countDown()
    }
    allSessionsClosed onFailure {
      case t: Throwable => logger.warn(s"Failed to stop all sessions/channels on shutdown: ${t.getMessage}", t.getCause)
    }
  }

  /**
   * Stop this server, and wait until all its resources have been released for up to `duration`.
   *
   * @param duration The duration to wait for this server to release all its resources
   * @param timeUnit [[TimeUnit]]
   * @throws IllegalStateException If this server has already been stopped or is currently stopping
   * @return `true` if all resources were successfully released within `duration`, `false` otherwise
   */
  @throws[IllegalStateException]("If this server has already been stopped or is currently stopping")
  final def stopAndWaitFor(duration: Long, timeUnit: TimeUnit): Boolean = {
    stop()
    waitUntilStoppedFor(duration, timeUnit)
  }

  /**
   * Wait until this server has been stopped, i.e. released all its resources, for up to `duration` measured in `timeUnit`.
   * Return `true` if this server's resources could successfully be released within `duration`, `false` otherwise.
   *
   * @param duration How long to wait for this server to release all its resources
   * @param timeUnit [[TimeUnit]]
   * @return `true` if this server's resources could successfully be released within `duration`, `false` otherwise
   */
  final def waitUntilStoppedFor(duration: Long, timeUnit: TimeUnit): Boolean = {
    isStopped.await(duration, timeUnit)
  }

  override def toString: String = {
    "Server(serverConfig:" + serverConfig + ")"
  }
}

/**
 * Factory for [[Server]]s and [[ServerBuilder]]s.
 */
object Server {

  /**
   * Create a new [[Server]] instance from the supplied [[ServerConfig]].
   *
   * @param config Configuration for the new [[Server]] instance
   * @throws IllegalArgumentException If `config` is `null`
   * @return A new [[Server]] instance
   */
  @throws[IllegalArgumentException]("If config is null")
  def apply(config: ServerConfig): Server = new Server(config)

  /**
   * Create a new [[ServerBuilder]] instance.
   *
   * @return A new [[ServerBuilder]] instance
   */
  def builder(): ServerBuilder = ServerBuilder()
}

/**
 * Builder for [[Server]]s.
 */
final class ServerBuilder protected[server]()
  extends ServerConfigBuilderSupport[ServerBuilder, Server] {

  /**
   * Build a new [[Server]] instance.
   *
   * @return A new [[Server]] instance
   */
  def build(): Server = {
    new Server(buildServerConfig())
  }
}

/**
 * Factory for [[ServerBuilder]]s.
 */
object ServerBuilder {

  /**
   * Create a new [[ServerBuilder]] instance.
   * @return A new [[ServerBuilder]] instance
   */
  def apply(): ServerBuilder = new ServerBuilder
}
