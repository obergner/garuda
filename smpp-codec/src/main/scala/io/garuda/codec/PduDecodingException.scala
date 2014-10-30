package io.garuda.codec

/**
 * Indicates that a [[io.garuda.codec.pdu.Pdu]] could not be decoded from its byte stream representation.
 *
 * This exception and its subclasses is only thrown when decoding failed completely, e.g. the type of [[io.garuda.codec.pdu.Pdu]]
 * is unknown (invalid [[io.garuda.codec.pdu.CommandId]]) or the byte stream representation is otherwise completely
 * mangled.
 *
 * User: obergner
 * Date: 29.10.13
 * Time: 23:33
 * To change this template use File | Settings | File Templates.
 */
class PduDecodingException(errorCode: Int, message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this(errorCode: ErrorCode, message: String, cause: Throwable) = this(errorCode.code, message, cause)

  def this(errorCode: ErrorCode) = this(errorCode, "", null)

  def this(errorCode: ErrorCode, message: String) = this(errorCode, message, null)

  def this(errorCode: ErrorCode, cause: Throwable) = this(errorCode, "", cause)

  def error: ErrorCode = ErrorCode(errorCode)
}
