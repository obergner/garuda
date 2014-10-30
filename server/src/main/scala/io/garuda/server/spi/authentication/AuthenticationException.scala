package io.garuda.server.spi.authentication

/**
 * Created by obergner on 17.05.14.
 */
abstract sealed class AuthenticationException(message: String, cause: Throwable)
  extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
}

class InvalidCredentialsException(message: String, cause: Throwable) extends AuthenticationException(message, cause) {

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
}

class TechnicalAuthenticationException(message: String, cause: Throwable)
  extends AuthenticationException(message, cause) {

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
}
