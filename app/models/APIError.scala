package models

import play.api.http.Status

sealed abstract class APIError(
                                val httpResponseStatus: Int,
                                val reason: String
                              )

object APIError {


  final case class NotFound(override val reason: String) extends APIError(Status.NOT_FOUND, reason)

  final case class DatabaseError(override val reason: String) extends APIError(Status.INTERNAL_SERVER_ERROR, reason)

  final case class BadAPIResponse(upstreamStatus: Int, upstreamMessage: String)
    extends APIError(Status.INTERNAL_SERVER_ERROR, s"Bad response from upstream; got status: $upstreamStatus, and got reason $upstreamMessage")
}

//^ 31/7 20:41 - Using abstract class over a trait as can pass constructor params
// More info https://www.geeksforgeeks.org/difference-between-traits-and-abstract-classes-in-scala/