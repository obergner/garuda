package io.garuda.common.session

import io.garuda.codec.ErrorCode

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 29.10.13
 * Time: 23:09
 * To change this template use File | Settings | File Templates.
 */
abstract class SessionException(val errorCode: Int, message: String, cause: Throwable)
  extends RuntimeException(message, cause) {

  def this(errorCode: ErrorCode, message: String, cause: Throwable) = this(errorCode.code, message, cause)

  def this(errorCode: ErrorCode) = this(errorCode, "", null)

  def this(errorCode: ErrorCode, message: String) = this(errorCode, message, null)

  def this(errorCode: ErrorCode, cause: Throwable) = this(errorCode, "", cause)

  def error: ErrorCode = ErrorCode(errorCode)
}
