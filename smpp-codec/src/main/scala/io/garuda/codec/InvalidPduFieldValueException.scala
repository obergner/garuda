package io.garuda.codec

/**
 * Indicates that a [[io.garuda.codec.pdu.Pdu]] field contains an invalid value. Examples include invalid [[io.garuda.codec.pdu.DataCodingScheme]]s,
 * [[io.garuda.codec.pdu.EsmClass]]es.
 *
 * User: obergner
 * Date: 01.11.13
 * Time: 20:22
 */
class InvalidPduFieldValueException(errorCode: Int, message: String, cause: Throwable)
  extends InvalidPduFieldException(errorCode, message, cause) {

  def this(errorCode: ErrorCode, message: String, cause: Throwable) = this(errorCode.code, message, cause)

  def this(errorCode: ErrorCode) = this(errorCode, "", null)

  def this(errorCode: ErrorCode, message: String) = this(errorCode, message, null)

  def this(errorCode: ErrorCode, cause: Throwable) = this(errorCode, "", cause)
}

