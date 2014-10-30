package io.garuda.windowing

import io.garuda.codec.pdu.{PduRequest, PduResponse}

import scala.concurrent.duration.Duration
import scala.util.Try

/**
 * Created by obergner on 06.04.14.
 */
trait Window {

  /**
   * A unique ID assigned to this [[Window]]. This will typically be just the ID of the `session` this [[Window]] is
   * bound to.
   *
   * @return This [[Window]]'s unique ID
   */
  def id: String

  /**
   * This [[Window]]'s `capacity`, i.e. the maximum number of `slots`/`requests` it can store at any given time.
   *
   * @return This [[Window]]'s `capacity`, i.e. the maximum number of slots/requests it can store at any given time
   */
  def capacity: Int

  /**
   * The number of `slots` currently in use.
   *
   * @return The number of `slots` currently in use
   */
  def usedSlotsCount: Int

  /**
   * The number of free/available `slots`.
   *
   * @return The number of free/available `slots`
   */
  def availableSlotsCount: Int = capacity - usedSlotsCount

  /**
   * By default, requests will wait for up to `defaultExpireTimeout` for their corresponding response before being
   * expired and thus discarded with a [[SlotExpiredException]].
   */
  def defaultExpireTimeout: Duration

  /**
   * Try to acquire a free/available `slot`.
   *
   * @param request
   * @tparam P
   * @tparam R
   * @return
   */
  def tryAcquireSlot[P <: PduResponse, R <: PduRequest[P]](request: R): Try[Slot[P, R]]

  /**
   * Open this [[Window]], i.e. start threads/tasks etc. It is illegal for a client to use a [[Window]] before it has
   * been opened.
   *
   * @return This [[Window]]
   */
  def open(): Window

  /**
   * Close this [[Window]], freeing all its resources. A [[Window]], once closed, cannot be reused.
   *
   * @return This [[Window]]
   */
  def close(): Window
}
