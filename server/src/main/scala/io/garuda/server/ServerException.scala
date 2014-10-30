package io.garuda.server

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 29.10.13
 * Time: 20:48
 * To change this template use File | Settings | File Templates.
 */
abstract class ServerException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this() = this("", null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this("", cause)
}
