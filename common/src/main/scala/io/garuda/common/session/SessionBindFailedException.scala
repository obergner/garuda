package io.garuda.common.session

import io.garuda.codec.ErrorCode

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 29.10.13
 * Time: 23:22
 * To change this template use File | Settings | File Templates.
 */
class SessionBindFailedException(message: String, cause: Throwable)
  extends SessionException(ErrorCode.ESME_RBINDFAIL.code, message, cause) {

  def this() = this("", null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this("", cause)
}
