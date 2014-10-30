package io.garuda.codec

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 30.10.13
 * Time: 20:34
 */
class TerminatingNullByteNotFoundException(errorCode: Int, message: String, cause: Throwable)
  extends InvalidPduFieldException(errorCode, message, cause) {

  def this(errorCode: ErrorCode, message: String, cause: Throwable) = this(errorCode.code, message, cause)

  def this(errorCode: ErrorCode) = this(errorCode, "", null)

  def this(errorCode: ErrorCode, message: String) = this(errorCode, message, null)

  def this(errorCode: ErrorCode, cause: Throwable) = this(errorCode, "", cause)
}
