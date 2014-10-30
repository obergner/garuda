package io.garuda.windowing

import io.garuda.codec.pdu.{PduRequest, PduResponse}

import scala.concurrent.Future
import scala.util.Try

/**
 * A `Slot` in a [[Window]] holds a [[io.garuda.codec.pdu.PduRequest]].
 *
 * Created by obergner on 31.08.14.
 */
trait Slot[Res <: PduResponse, Req <: PduRequest[Res]] {

  /**
   * This `Slot`'s unique key.
   *
   * @return This `Slot`'s unique key
   */
  def key: Int

  /**
   * The [[PduRequest]] stored in this `Slot`.
   *
   * @return The [[PduRequest]] stored in this `Slot`
   */
  def request: Req

  /**
   * When this `Slot` was acquired, as returned by [[System.nanoTime( )]].
   *
   * @return  When this `Slot` was acquired, as returned by [[System.nanoTime( )]].
   */
  def acquisitionTimeNanos: Long

  /**
   * When this `Slot` expired/will expire, as returned by [[System.nanoTime( )]].
   *
   * @return When this `Slot` expired/will expire, as returned by [[System.nanoTime( )]]
   */
  def expirationTimeNanos: Long

  /**
   * Try to release this slot, i.e. free a place in a [[Window]]. Return [[scala.util.Success]] if this `Slot` was actually released,
   * [[scala.util.Failure]] if it was already released before, was expired, or if the [[Window]] this `Slot` belongs to was closed.
   *
   * @param response The [[PduResponse]] to this `Slot`'s [[PduRequest]]
   * @return [[scala.util.Success]] if this `Slot` was actually released,
   *         [[scala.util.Failure]] if it was already released before, was expired, or if the [[Window]] this `Slot` belongs to was closed
   */
  def release(response: Res): Try[Unit]

  /**
   * Try to release this slot, i.e. free a place in a [[Window]]. Return [[scala.util.Success]] if this `Slot` was actually released,
   * [[scala.util.Failure]] if it was already released before, was expired, or if the [[Window]] this `Slot` belongs to was closed.
   *
   * @param error The error that causes this `Slot` to be released
   * @return [[scala.util.Success]] if this `Slot` was actually released,
   *         [[scala.util.Failure]] if it was already released before, was expired, or if the [[Window]] this `Slot` belongs to was closed
   */
  def release(error: Throwable): Try[Unit]

  /**
   * [[Future]] that will be completed as soon as this `Slot` is either released or expired or closed.
   *
   * @return A [[Future]] that will be completed as soon as this `Slot` is either released of expired or closed
   */
  def releaseFuture: Future[Res]
}
