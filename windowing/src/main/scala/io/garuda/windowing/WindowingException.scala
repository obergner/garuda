package io.garuda.windowing

import io.garuda.codec.pdu.PduRequest

import scala.concurrent.duration.Duration

/**
 * Created by obergner on 11.04.14.
 */
abstract sealed class WindowingException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
}

class SlotAcquisitionTimedOutException(message: String,
                                       cause: Throwable,
                                       val request: PduRequest[_],
                                       val timeout: Duration)
  extends WindowingException(SlotAcquisitionTimedOutException.normalizeMessage(message, cause, request, timeout), cause) {

  def this(message: String, request: PduRequest[_], timeout: Duration) = this(message, null, request, timeout)

  def this(cause: Throwable, request: PduRequest[_], timeout: Duration) = this(null, cause, request, timeout)

  def this(request: PduRequest[_], timeout: Duration) = this(null, null, request, timeout)
}

object SlotAcquisitionTimedOutException {

  def normalizeMessage(message: String, cause: Throwable, request: PduRequest[_], timeout: Duration): String = {
    if (message != null) {
      return message
    }
    s"Acquiring a slot for [${request}] timed out after [${timeout.toMillis}] ms${if (cause != null) ": " + cause.getMessage else ""}"
  }
}

class WindowClosingOrClosedException(message: String, cause: Throwable, val request: PduRequest[_])
  extends WindowingException(message, cause) {

  def this(message: String, request: PduRequest[_]) = this(message, null, request)

  def this(cause: Throwable, request: PduRequest[_]) = this(cause.getMessage, cause, request)
}

object WindowClosingOrClosedException {

  def windowClosedWhileWaitingForResponse(request: PduRequest[_]): WindowClosingOrClosedException = {
    new WindowClosingOrClosedException(s"Window was closed while [${request}] was waiting for a response", request)
  }
}

class DuplicateSequenceNumberException(message: String,
                                       cause: Throwable,
                                       val request: PduRequest[_])
  extends WindowingException(DuplicateSequenceNumberException.normalizeMessage(message, cause, request), cause) {

  def this(message: String, request: PduRequest[_]) = this(message, null, request)

  def this(cause: Throwable, request: PduRequest[_]) = this(null, cause, request)

  def this(request: PduRequest[_]) = this(null, null, request)

  def duplicateSequenceNumber: Int = request.sequenceNumber.get
}

object DuplicateSequenceNumberException {

  def normalizeMessage(message: String, cause: Throwable, request: PduRequest[_]): String = {
    if (message != null) {
      return message
    }
    s"Illegal attempt to acquire a slot for [${request}] with sequence number [${request.sequenceNumber.get}]: " +
      s"there is another request stored with the same sequence number${if (cause != null) ": " + cause.getMessage else ""}"
  }
}

class SlotExpiredException(message: String,
                           cause: Throwable,
                           val request: PduRequest[_],
                           val timeout: Duration)
  extends WindowingException(SlotExpiredException.normalizeMessage(message, cause, request, timeout), cause) {

  def this(message: String, request: PduRequest[_], timeout: Duration) = this(message, null, request, timeout)

  def this(cause: Throwable, request: PduRequest[_], timeout: Duration) = this(null, cause, request, timeout)

  def this(request: PduRequest[_], timeout: Duration) = this(null, null, request, timeout)
}

object SlotExpiredException {

  def normalizeMessage(message: String, cause: Throwable, request: PduRequest[_], timeout: Duration): String = {
    if (message != null) {
      return message
    }
    s"Slot for [${request}] expired after [${timeout.toMillis}] ms without receiving a response${if (cause != null) ": " + cause.getMessage else ""}"
  }
}
