package io.garuda.codec

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.10.13
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */
case class ErrorCode(code: Int, description: String) {

  ErrorCode.tmpCodeToErrorCode(code) = this
}

object ErrorCode {

  private val tmpCodeToErrorCode: scala.collection.mutable.Map[Int, ErrorCode] = scala.collection.mutable.Map[Int, ErrorCode]()

  val ESME_ROK = ErrorCode(0x00000000, "No Error. Specified in a response PDU to indicate the success of the corresponding request PDU.")

  val ESME_RINVMSGLEN = ErrorCode(0x00000001, "Message Length is invalid. short_message field or message_payload TLV has an invalid length (usually too long for the given MC or underlying network technology).")

  val ESME_RINVCMDLEN = ErrorCode(0x00000002, "Command Length is invalid. PDU length is considered invalid, either because the value is too short or too large for the given PDU.")

  val ESME_RINVCMDID = ErrorCode(0x00000003, "Invalid Command ID. Command ID is not recognised, either because the operation is not supported or unknown.")

  val ESME_RINVBNDSTS = ErrorCode(0x00000004, "Incorrect BIND Status for given command. PDU has been sent in the wrong session state. E.g. sending a submit_sm without first establishing a Bound_TX session state.")

  val ESME_RALYBND = ErrorCode(0x00000005, "ESME Already in Bound State. A bind request has been issued within a session that is already bound.")

  val ESME_RINVPRTFLG = ErrorCode(0x00000006, "Invalid Priority Flag. Priority flag contains an illegal or unsupported value.")

  val ESME_RINVREGDLVFLG = ErrorCode(0x00000007, "Invalid Registered Delivery Flag. Registered field contains an invalid setting.")

  val ESME_RSYSERR = ErrorCode(0x00000008, "System Error. MC system session indicating that all or part of the MC is currently unavailable. This can be returned in any response PDU.")

  val ESME_RINVSRCADR = ErrorCode(0x0000000A, "Invalid Source Address. Source address of message is considered invalid. Usually this is because the field is either too long or contains invalid characters.")

  val ESME_RINVDSTADR = ErrorCode(0x0000000B, "Invalid Destination Address. Destination address of message is considered invalid. Usually this is because the field is either zero length, too long or contains invalid characters.")

  val ESME_RINVMSGID = ErrorCode(0x0000000C, "Message ID is invalid. Message ID specified in cancel_sm, query_sm or other operations is invalid.")

  val ESME_RBINDFAIL = ErrorCode(0x0000000D, "Bind Failed. A generic failure scenario for a bind attempt. This may be due to a provisioning session, incorrect password or other reason. A MC will typically return this session for an invalid system_id, system_type, password or other attribute that may cause a bind failure.")

  val ESME_RINVPASWD = ErrorCode(0x0000000E, "Invalid Password. Password field in bind PDU is invalid. This is usually returned when the length is too short or too long. It is not supposed to be returned when the ESME has specified the incorrect password.")

  val ESME_RINVSYSID = ErrorCode(0x0000000F, "Invalid System ID. The System ID field in bind PDU is invalid. This is usually returned when the length is too short or too long. It is not supposed to be returned when the ESME has specified the incorrect system id.")

  val ESME_RCANCELFAIL = ErrorCode(0x00000011, "Cancel SM Failed. Generic failure session for cancel_sm operation.")

  val ESME_RREPLACEFAIL = ErrorCode(0x00000013, "Replace SM Failed. Generic failure for replace_sm operation.")

  val ESME_RMSGQFUL = ErrorCode(0x00000014, "Message Queue Full. Used to indicate a resource session within the MC. This may be interpreted as the maximum number of messages addressed to a single destination or a global maximum of undelivered messages within the MC.")

  val ESME_RINVSERTYP = ErrorCode(0x00000015, "Invalid Service Type. Service type is rejected either because it is not recognised by the MC or because its length is not within the defined range.")

  val ESME_RINVNUMDESTS = ErrorCode(0x00000033, "Invalid number of destinations. The number_of_dests field in the submit_multi PDU is invalid.")

  val ESME_RINVDLNAME = ErrorCode(0x00000034, "Invalid Distribution List name. The dl_name field specified in the submit_multi PDU is either invalid, or non-existent.")

  val ESME_RINVDESTFLAG = ErrorCode(0x00000040, "Destination flag is invalid (submit_multi). The dest_flag field in the submit_multi PDU has been encoded with an invalid setting.")

  val ESME_RINVSUBREP = ErrorCode(0x00000042, "Submit w/replace functionality has been requested where it is either unsupported or inappropriate for the particular MC. This can typically occur with submit_multi where the context of “replace if present” is often a best effort operation and MCs may not support the feature in submit_multi. Another reason for returning this session would be where the feature has been denied to an ESME.")

  val ESME_RINVESMCLASS = ErrorCode(0x00000043, "Invalid esm_class field data. The esm_class field has an unsupported setting.")

  val ESME_RCNTSUBDL = ErrorCode(0x00000044, "Cannot Submit to Distribution List. Distribution lists are not supported, are denied or unavailable.")

  val ESME_RSUBMITFAIL = ErrorCode(0x00000045, "submit_sm, data_sm or submit_multi failed. Generic failure session for submission operations.")

  val ESME_RINVSRCTON = ErrorCode(0x00000048, "Invalid Source address TON. The source TON of the message is either invalid or unsupported.")

  val ESME_RINVSRCNPI = ErrorCode(0x00000049, "Invalid Source address NPI. The source NPI of the message is either invalid or unsupported.")

  val ESME_RINVDSTTON = ErrorCode(0x00000050, "Invalid Destination address TON. The destination TON of the message is either invalid or unsupported.")

  val ESME_RINVDSTNPI = ErrorCode(0x00000051, "Invalid Destination address NPI. The destination NPI of the message is either invalid or unsupported.")

  val ESME_RINVSYSTYP = ErrorCode(0x00000053, "Invalid system_type field. The System type of bind PDU has an incorrect length or contains illegal characters.")

  val ESME_RINVREPFLAG = ErrorCode(0x00000054, "Invalid replace_if_present flag. The replace_if_present flag has been encoded with an invalid or unsupported setting.")

  val ESME_RINVNUMMSGS = ErrorCode(0x00000055, "Invalid number of messages.")

  val ESME_RTHROTTLED = ErrorCode(0x00000058, "Throttling session (ESME has exceeded allowed message limits). This type of session is usually returned where an ESME has exceeded a predefined messaging rate restriction applied by the operator.")

  val ESME_RINVSCHED = ErrorCode(0x00000061, "Invalid Scheduled Delivery Time. Scheduled delivery time is either the incorrect length or is invalid.")

  val ESME_RINVEXPIRY = ErrorCode(0x00000062, "Invalid message validity period (Expiry time). Expiry time is either the incorrect length or is invalid.")

  val ESME_RINVDFTMSGID = ErrorCode(0x00000063, "Predefined Message ID is Invalid or specified predefined message was not found. The default (pre-defined) message id is either invalid or refers to a non-existent pre-defined message.")

  val ESME_RX_T_APPN = ErrorCode(0x00000064, "ESME Receiver Temporary App Error Code. Rx or Trx ESME is unable to process a delivery due to a temporary problem and is requesting that the message be retried at some future point.")

  val ESME_RX_P_APPN = ErrorCode(0x00000065, "ESME Receiver Permanent App Error Code. Rx or Trx ESME is unable to process a delivery due to a permanent problem relating to the given destination address and is requesting that the message and all other messages queued to the same destination should NOT be retried any further.")

  val ESME_RX_R_APPN = ErrorCode(0x00000066, "ESME Receiver Reject Message Error Code. Rx or Trx ESME is unable to process a delivery due to a problem relating to the given message and is requesting that the message is rejected and not retried. This does not affect other messages queued for the same ESME or destination address.")

  val ESME_RQUERYFAIL = ErrorCode(0x00000067, "query_sm request failed. Generic failure scenario for a query request.")

  val ESME_RINVTLVSTREAM = ErrorCode(0x000000C0, "Error in the optional part of the PDU Body. Decoding of TLVs (Optional Parameters) has resulted in one of the following scenarios: (1) PDU decoding completed with 1- 3 octets of data remaining, indicating a corrupt PDU. (2) A TLV indicated a length that was not present in the remaining PDU data (e.g. a TLV specifying a length of 10 where only 6 octets of PDU data remain).")

  val ESME_RTLVNOTALLWD = ErrorCode(0x000000C1, "TLV not allowed. A TLV has been used in an invalid context, either inappropriate or deliberately rejected by the operator.")

  val ESME_RINVTLVLEN = ErrorCode(0x000000C2, "Invalid Parameter Length. A TLV has specified a length that is considered invalid.")

  val ESME_RMISSINGTLV = ErrorCode(0x000000C3, "Expected TLV missing. A mandatory TLV such as the message_payload TLV within a data_sm PDU is missing.")

  val ESME_RINVTLVVAL = ErrorCode(0x000000C4, "Invalid TLV Value. The data content of a TLV is invalid and cannot be decoded.")

  val ESME_RDELIVERYFAILURE = ErrorCode(0x000000FE, "Transaction Delivery Failure. A data_sm or submit_sm operation issued in transaction mode has resulted in a failed delivery.")

  val ESME_RUNKNOWNERR = ErrorCode(0x000000FF, "Unknown Error. Some unexpected session has occurred.")

  val ESME_RSERTYPUNAUTH = ErrorCode(0x00000100, "ESME Not authorised to use specified service_type. Specific service_type has been denied for use by the given ESME.")

  val ESME_RPROHIBITED = ErrorCode(0x00000101, "ESME Prohibited from using specified operation. The PDU request was recognised but is denied to the ESME.")

  val ESME_RSERTYPUNAVAIL = ErrorCode(0x00000102, "Specified service_type is unavailable. Due to a service outage within the MC, a service is unavailable.")

  val ESME_RSERTYPDENIED = ErrorCode(0x00000103, "Specified service_type is denied. Due to inappropriate message content wrt. the selected service_type.")

  val ESME_RINVDCS = ErrorCode(0x00000104, "Invalid Data Coding Scheme. Specified DCS is invalid or MC does not support it.")

  val ESME_RINVSRCADDRSUBUNIT = ErrorCode(0x00000105, "Source Address Sub unit is Invalid.")

  val ESME_RINVDSTADDRSUBUNIT = ErrorCode(0x00000106, "Destination Address Sub unit is Invalid.")

  val ESME_RINVBCASTFREQINT = ErrorCode(0x00000107, "Broadcast Frequency Interval is invalid. Specified value is either invalid or not supported.")

  val ESME_RINVBCASTALIAS_NAME = ErrorCode(0x00000108, "Broadcast Alias Name is invalid. Specified value has an incorrect length or contains invalid/unsupported characters.")

  val ESME_RINVBCASTAREAFMT = ErrorCode(0x00000109, "Broadcast Area Format is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RINVNUMBCAST_AREAS = ErrorCode(0x0000010A, "Number of Broadcast Areas is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RINVBCASTCNTTYPE = ErrorCode(0x0000010B, "Broadcast Content Type is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RINVBCASTMSGCLASS = ErrorCode(0x0000010C, "Broadcast Message Class is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RBCASTFAIL = ErrorCode(0x0000010D, "broadcast_sm operation failed.")

  val ESME_RBCASTQUERYFAIL = ErrorCode(0x0000010E, "query_broadcast_sm operation failed.")

  val ESME_RBCASTCANCELFAIL = ErrorCode(0x0000010F, "cancel_broadcast_sm operation failed.")

  val ESME_RINVBCAST_REP = ErrorCode(0x00000110, "Number of Repeated Broadcasts is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RINVBCASTSRVGRP = ErrorCode(0x00000111, "Broadcast Service Group is invalid. Specified value violates protocol or is unsupported.")

  val ESME_RINVBCASTCHANIND = ErrorCode(0x00000112, "Broadcast Channel Indicator is invalid. Specified value violates protocol or is unsupported.")

  private[this] val codeToErrorCode: scala.collection.immutable.Map[Int, ErrorCode] = tmpCodeToErrorCode.toMap

  def apply(code: Int): ErrorCode = {
    codeToErrorCode.getOrElse(code, ErrorCode(code, "Reserved: " + code))
  }
}
