package io.garuda.codec

/**
 * Base class for exceptions indicating that a [[io.garuda.codec.pdu.Pdu]] could be recognized, yet contains an invalid
 * field. Examples include null-terminated string without terminating null byte, invalid [[io.garuda.codec.pdu.DataCodingScheme]]s
 * and unknown [[io.garuda.codec.pdu.Ton]]s.
 *
 * User: obergner
 * Date: 01.11.13
 * Time: 19:35
 */
abstract class InvalidPduFieldException(errorCode: Int, message: String, cause: Throwable)
  extends RuntimeException(message, cause) {

  def this(errorCode: ErrorCode, message: String, cause: Throwable) = this(errorCode.code, message, cause)

  def this(errorCode: ErrorCode) = this(errorCode, "", null)

  def this(errorCode: ErrorCode, message: String) = this(errorCode, message, null)

  def this(errorCode: ErrorCode, cause: Throwable) = this(errorCode, "", cause)

  def error: ErrorCode = ErrorCode(errorCode)
}
