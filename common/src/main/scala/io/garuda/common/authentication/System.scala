package io.garuda.common.authentication

/**
 * Represents a remote `system`, i.e. either a client or a server.
 *
 * A [[System]] is uniquely identified by its `id`, passed either in a `BindRequest` (client) or `BindResponse` (server).
 */
trait System {

  def id: String
}
