package io.garuda.server.session.support

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import io.garuda.server.session.ServerSessionConfig
import io.netty.util.{HashedWheelTimer, Timer}

/**
 * A factory for Netty [[io.netty.util.Timer]]s that will create and return one [[io.netty.util.Timer]] instance for
 * a configurable number of [[io.garuda.server.spi.session.ServerSession]]s. The rationale is that there is a
 * limit to the number of [[io.garuda.server.spi.session.ServerSession]]s a single [[io.netty.util.Timer]] can
 * handle with acceptable precision.
 *
 * Created by obergner on 28.03.14.
 */
class CachingTimerFactory(threadNamePrefix: String, numberOfSessionsPerTimer: Int)
  extends (ServerSessionConfig => Timer) {
  require(threadNamePrefix != null && !threadNamePrefix.isEmpty, "Argument 'threadNamePrefix' must not be empty: " + threadNamePrefix)
  require(numberOfSessionsPerTimer > 0, "Argument 'numberOfSessionsPerTimer' must be > 0: " + numberOfSessionsPerTimer)

  private[this] val yieldLimit = 100000

  private[this] val invocationCount = new AtomicInteger(0)

  private[this] val threadFactory = new CachedTimerThreadFactory(threadNamePrefix)

  private[this] val cachedTimer = new AtomicReference[CachedTimer](newCachedTimer(numberOfSessionsPerTimer))

  override def apply(config: ServerSessionConfig): Timer = {
    val currentInvocation: Int = invocationCount.incrementAndGet()
    if ((currentInvocation > 1) && ((currentInvocation - 1) % numberOfSessionsPerTimer == 0)) {
      val newCachedTimer1: CachedTimer = newCachedTimer((currentInvocation - 1) + numberOfSessionsPerTimer)
      cachedTimer.set(newCachedTimer1)
      newCachedTimer1.timer
    } else {
      /*
       * Goal: a Timer returned from this method should be responsible for EXACTLY numberOfSessionsPerTimer sessions.
       *
       * Constraint: do not create more HashedWheelTimers than absolutely necessary. A HashedWheelTimer, in its constructor,
       * will allocate a new Thread, which in turn will call out to the OS to allocate two stacks, a java and a native
       * stack. This is expensive. For some definition of expensive.
       *
       * Algorithm: create a new Timer every numberOfSessionsPerTimer invocations of this method.
       *
       * Challenge: suppose Thread1 enters this method and realizes it should create a new Timer, since
       * (currentInvocation % numberOfSessionsPerTimer) == 0. It starts to create this new Timer, BUT BEFORE IT can
       * cache it, Thread2 enters this method and "overtakes" Thread1.
       *
       * In this situation, without taking any further measures Thread2 would grab the "stale" cached Timer and return
       * that. This "stale" cached Timer would thus be responsible for numberOfSessionsPerTimer + 1 sessions. NOT GOOD.
       *
       * Solution: Each cached Timer knows about the upper limit of invocations (of this method) it can legally serve.
       * In our example, Thread2 would thus be able to realize that the currently cached Timer is "stale", and would
       * wait for Thread1 to create a new one.
       *
       * Shortcomings: This "solution" is not guaranteed to produce the desired effect. If e.g. Thread2 advances beyond
       * Thread1 for more than numberOfSessionsPerTimer invocations of this method it will effectively "miss out" on
       * one Timer instance.
       *
       * Note to self: stop trying to be clever. All this highfalutin' code-mongery would only come to bear in case an
       * insane number of connections/sessions were created almost exactly at once. In my book, that's not very likely.
       */
      var localCachedTimer = cachedTimer.get()
      var spinCount: Int = 0
      while (localCachedTimer.invocationLimit < currentInvocation) {
        spinCount += 1
        localCachedTimer = cachedTimer.get()
        if ((spinCount % yieldLimit) == 0) Thread.`yield`() // yield every yieldLimit iterations
      }
      localCachedTimer.timer
    }
  }

  private[this] def newCachedTimer(invocationLimit: Int): CachedTimer =
    CachedTimer(invocationLimit, new HashedWheelTimer(threadFactory))

  private final case class CachedTimer(invocationLimit: Int, timer: Timer)

  private final class CachedTimerThreadFactory(threadNamePrefix: String) extends ThreadFactory {

    val nextThreadId = new AtomicInteger(1)

    override def newThread(r: Runnable): Thread = {
      new Thread(r, threadNamePrefix + "#" + nextThreadId.getAndIncrement)
    }
  }

}
