package com.radiance.scala.tonclient

import com.radiance.scala.types.ClientTypes.ClientError
import io.circe.{Decoder, DecodingFailure, HCursor}


case class OperationResponse[T : Decoder](result: Option[T], error: Option[ClientError])

object OperationResponse {
  implicit def OperationResponseDecoder[T: Decoder]: Decoder[OperationResponse[T]] = (c: HCursor) => {
    c.downField("result").success.map(_.as[T].map(r => OperationResponse(Some(r), None)))
      .orElse(c.downField("error").success.map(u => u.as[ClientError].map(u => OperationResponse[T](None: Option[T], Some(u)))))
      .getOrElse(Left(DecodingFailure("""Can't find nor "result" neither "error" fields""", Nil)))
  }
}