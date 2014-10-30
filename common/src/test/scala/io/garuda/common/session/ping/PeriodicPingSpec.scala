package io.garuda.common.session.ping

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.EnquireLink
import io.garuda.common.builder.BuilderContext
import io.netty.channel._
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.local.LocalAddress
import io.netty.util.HashedWheelTimer
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Specification, Tags}
import org.specs2.specification.AroundOutside
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * Created by obergner on 04.04.14.
 */
class PeriodicPingSpec extends Specification with Tags with NoTimeConversions with Logging {

  logger.info(s"Initialized logging subsystem for ${this}")

  case class builder_context(name: String) extends AroundOutside[BuilderContext] {

    private[this] lazy val builderContext = BuilderContext.withDefaultMetricRegistry(name, 10 milliseconds)

    override def outside: BuilderContext = builderContext

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        outside.open()
        AsResult(a)
      } finally {
        outside.close()
      }
    }
  }

  "PeriodicPing" should {

    "send out an enquire_link after pingIntervalMillis have elapsed" in builder_context("send out an enquire_link after pingIntervalMillis have elapsed") {
      (builderContext: BuilderContext) => {
        val enquireLinkIntervalMillis = 200L
        val enquireLinkResponseTimeoutMillis = 10000L
        val periodicPingConfig = PeriodicPingConfig(enquireLinkIntervalMillis, enquireLinkResponseTimeoutMillis, Function.const(new
            HashedWheelTimer()), builderContext)
        val objectUnderTest = PeriodicPing(periodicPingConfig)

        val pingSentAt = new AtomicLong(0)
        val pingSent = new CountDownLatch(1)
        val pingReceiver = new ChannelOutboundHandlerAdapter() {
          override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
            msg match {
              case _: EnquireLink =>
                pingSentAt.set(System.currentTimeMillis())
                pingSent.countDown()
            }
            super.write(ctx, msg, promise)
          }
        }

        val start = System.currentTimeMillis()
        val channel = new EmbeddedChannel(objectUnderTest, pingReceiver)
        channel.bind(new LocalAddress("periodic-ping-spec-1"))

        val pingHasActuallyBeenSent = pingSent.await(enquireLinkIntervalMillis + 200L, TimeUnit.MILLISECONDS)
        channel.close()
        pingHasActuallyBeenSent must beTrue
        (pingSentAt.get() - start) must beGreaterThanOrEqualTo(enquireLinkIntervalMillis)
      }
    } tag "integration"

    "send an enquire_link after receiving an enquire_link_resp" in builder_context("send an enquire_link after receiving an enquire_link_resp") {
      (builderContext: BuilderContext) => {
        val enquireLinkIntervalMillis = 200L
        val enquireLinkResponseTimeoutMillis = 10000L
        val periodicPingConfig = PeriodicPingConfig(enquireLinkIntervalMillis, enquireLinkResponseTimeoutMillis, Function.const(new
            HashedWheelTimer()), builderContext)
        val objectUnderTest = PeriodicPing(periodicPingConfig)

        val secondPingSentAt = new AtomicLong(0)
        val secondPingSent = new CountDownLatch(2)
        val pingReceiver = new ChannelOutboundHandlerAdapter() {
          override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
            msg match {
              case enquireLink: EnquireLink =>
                if (secondPingSent.getCount == 1) {
                  secondPingSentAt.set(System.currentTimeMillis())
                } else {
                  ctx.channel().asInstanceOf[EmbeddedChannel].writeInbound(enquireLink.createResponse(0))
                }
                secondPingSent.countDown()
            }
            super.write(ctx, msg, promise)
          }
        }

        val start = System.currentTimeMillis()
        val channel = new EmbeddedChannel(objectUnderTest, pingReceiver)
        channel.bind(new LocalAddress("periodic-ping-spec-2"))

        val secondPingHasActuallyBeenSent = secondPingSent.await(2 * enquireLinkIntervalMillis + 400L, TimeUnit.MILLISECONDS)
        channel.close()
        secondPingHasActuallyBeenSent must beTrue
        (secondPingSentAt.get() - start) must beGreaterThanOrEqualTo(2 * enquireLinkIntervalMillis)
      }
    } tag "integration"

    "close the current channel after enquireLinkResponseTimeoutMillis if not receiving an enquire_link_resp" in
      builder_context("close the current channel after enquireLinkResponseTimeoutMillis if not receiving an enquire_link_resp") {
        (builderContext: BuilderContext) => {
          val enquireLinkIntervalMillis = 200L
          val enquireLinkResponseTimeoutMillis = 300L
          val periodicPingConfig = PeriodicPingConfig(enquireLinkIntervalMillis, enquireLinkResponseTimeoutMillis, Function.const(new
              HashedWheelTimer()), builderContext)
          val objectUnderTest = PeriodicPing(periodicPingConfig)

          val channelClosedAt = new AtomicLong(0)
          val channelHasBeenClosed = new CountDownLatch(1)
          val pingReceiver = new ChannelOutboundHandlerAdapter() {
            override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
              msg match {
                case enquireLink: EnquireLink =>
                  ctx.channel().closeFuture().addListener(new ChannelFutureListener {
                    override def operationComplete(future: ChannelFuture): Unit = {
                      channelClosedAt.set(System.currentTimeMillis())
                      channelHasBeenClosed.countDown()
                    }
                  })
              }
              super.write(ctx, msg, promise)
            }
          }

          val start = System.currentTimeMillis()
          val channel = new EmbeddedChannel(objectUnderTest, pingReceiver)
          channel.bind(new LocalAddress("periodic-ping-spec-3"))

          val channelHasActuallyBeenClosed = channelHasBeenClosed.await(enquireLinkIntervalMillis + enquireLinkResponseTimeoutMillis + 400L, TimeUnit.MILLISECONDS)
          channel.close()
          channelHasActuallyBeenClosed must beTrue
          (channelClosedAt.get() - start) must beGreaterThanOrEqualTo(enquireLinkIntervalMillis + enquireLinkResponseTimeoutMillis)
        }
      } tag "integration"
  }
}
