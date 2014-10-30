package io.garuda.codec.util

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 13.08.13
 * Time: 21:11
 * To change this template use File | Settings | File Templates.
 */
object HexHelpers {

  def hexStringToByteArray(hex: String): Array[Byte] = {
    (for {i <- 0 to hex.length - 1 by 2 if i > 0 || !hex.startsWith("0x")} yield hex.substring(i,
      i + 2)).map(Integer.parseInt(_, 16).toByte).toArray
  }

  def byteArrayToHexString(bytes: Array[Byte]): String = {
    def cvtByte(b: Byte): String = {
      (if ((b & 0xff) < 0x10) "0" else "") + java.lang.Long.toString(b & 0xff, 16)
    }

    "0x" + bytes.map(cvtByte).mkString.toUpperCase
  }
}
