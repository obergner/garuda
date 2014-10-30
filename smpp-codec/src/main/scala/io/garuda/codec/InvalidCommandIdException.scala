package io.garuda.codec

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 30.10.13
 * Time: 21:54
 * To change this template use File | Settings | File Templates.
 */
class InvalidCommandIdException(invalidCommandId: Int, message: String, cause: Throwable)
  extends PduDecodingException(ErrorCode.ESME_RINVCMDID.code, message, cause) {

  def this(invalidCommandId: Int) = this(invalidCommandId, "", null)

  def this(invalidCommandId: Int, message: String) = this(invalidCommandId, message, null)

  def this(invalidCommandId: Int, cause: Throwable) = this(invalidCommandId, "", cause)
}
