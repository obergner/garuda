package io.garuda.common.authentication

/**
 * Represents a remote `system`, i.e. either a client or a server.
 *
 * A [[RemoteSystem]] is uniquely identified by its `id`, passed either in a `BindRequest` (client) or `BindResponse` (server).
 */
trait RemoteSystem {

  def id: String
}
