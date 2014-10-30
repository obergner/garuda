package io.garuda

/**
 * Created by obergner on 12.10.14.
 */
package object codec {

  /**
   * Key used to store the current [[io.garuda.codec.pdu.Pdu]]'s ID in SLF4J's [[org.slf4j.MDC]].
   */
  val PduIdContextKey: String = "pduId"
}
