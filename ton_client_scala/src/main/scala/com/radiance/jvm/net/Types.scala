package com.radiance.jvm.net

import com.radiance.jvm._
import io.circe._
import io.circe.derivation._
import io.circe.generic.extras

object AggregationFnEnum {

  sealed trait AggregationFn

  /**
   * Returns an average value for a field in filtered records
   */
  case object AVERAGE extends AggregationFn

  /**
   * Returns count of filtered record
   */
  case object COUNT extends AggregationFn

  /**
   * Returns the maximal value for a field in filtered records
   */
  case object MAX extends AggregationFn

  /**
   * Returns the minimal value for a field in filtered records
   */
  case object MIN extends AggregationFn

  /**
   * Returns a sum of values for a field in filtered records
   */
  case object SUM extends AggregationFn

  implicit val encoder: Encoder[AggregationFn] =
    extras.semiauto.deriveEnumerationEncoder[AggregationFn]
}

case class EndpointsSet(endpoints: List[String])

object EndpointsSet {
  implicit val codec: Codec[EndpointsSet] = deriveCodec[EndpointsSet]
}

case class FieldAggregation(field: String, fn: AggregationFnEnum.AggregationFn)

object FieldAggregation {
  implicit val encoder: Encoder[FieldAggregation] = deriveEncoder[FieldAggregation]
}

sealed trait NetErrorCode {
  val code: String
}

object NetErrorCode {
  case object ClockOutOfSync extends NetErrorCode {
    override val code: String = "606"
  }
  case object GetSubscriptionResultFailed extends NetErrorCode {
    override val code: String = "604"
  }
  case object GraphqlError extends NetErrorCode {
    override val code: String = "608"
  }
  case object GraphqlWebsocketInitError extends NetErrorCode {
    override val code: String = "613"
  }
  case object InvalidServerResponse extends NetErrorCode {
    override val code: String = "605"
  }
  case object NetworkModuleResumed extends NetErrorCode {
    override val code: String = "614"
  }
  case object NetworkModuleSuspended extends NetErrorCode {
    override val code: String = "609"
  }
  case object NoEndpointsProvided extends NetErrorCode {
    override val code: String = "612"
  }
  case object NotSupported extends NetErrorCode {
    override val code: String = "611"
  }
  case object QueryFailed extends NetErrorCode {
    override val code: String = "601"
  }
  case object SubscribeFailed extends NetErrorCode {
    override val code: String = "602"
  }
  case object WaitForFailed extends NetErrorCode {
    override val code: String = "603"
  }
  case object WaitForTimeout extends NetErrorCode {
    override val code: String = "607"
  }
  case object WebsocketDisconnected extends NetErrorCode {
    override val code: String = "610"
  }
}

case class OrderBy(path: String, direction: SortDirectionEnum.SortDirection)

object OrderBy {
  implicit val encoder: Encoder[OrderBy] = deriveEncoder[OrderBy]
}

case class ParamsOfAggregateCollection(
  collection: String,
  filter: Option[Value],
  fields: Option[List[FieldAggregation]]
)

object ParamsOfAggregateCollection {
  implicit val encoder: Encoder[ParamsOfAggregateCollection] = deriveEncoder[ParamsOfAggregateCollection]
}

case class ParamsOfBatchQuery(operations: List[ParamsOfQueryOperationADT.ParamsOfQueryOperation])

object ParamsOfBatchQuery {
  implicit val encoder: Encoder[ParamsOfBatchQuery] = deriveEncoder[ParamsOfBatchQuery]
}

case class ParamsOfFindLastShardBlock(address: String)

object ParamsOfFindLastShardBlock {
  implicit val encoder: Encoder[ParamsOfFindLastShardBlock] =
    deriveEncoder[ParamsOfFindLastShardBlock]
}

case class ParamsOfQuery(query: String, variables: Option[Value])

object ParamsOfQuery {
  implicit val encoder: Encoder[ParamsOfQuery] = deriveEncoder[ParamsOfQuery]
}

case class ParamsOfQueryCollection(
  collection: String,
  filter: Option[Value],
  result: String,
  order: Option[List[OrderBy]],
  limit: Option[Long]
)

object ParamsOfQueryCollection {
  implicit val encoder: Encoder[ParamsOfQueryCollection] =
    deriveEncoder[ParamsOfQueryCollection]
}

case class ParamsOfQueryCounterparties(
  account: String,
  result: String,
  first: Option[Long],
  after: Option[String]
)

object ParamsOfQueryCounterparties {
  implicit val encoder: Encoder[ParamsOfQueryCounterparties] =
    deriveEncoder[ParamsOfQueryCounterparties]
}

object ParamsOfQueryOperationADT {

  sealed trait ParamsOfQueryOperation

  case class AggregateCollection(value: ParamsOfAggregateCollection) extends ParamsOfQueryOperation

  case class QueryCollection(value: ParamsOfQueryCollection) extends ParamsOfQueryOperation

  case class QueryCounterparties(value: ParamsOfQueryCounterparties) extends ParamsOfQueryOperation

  case class WaitForCollection(value: ParamsOfWaitForCollection) extends ParamsOfQueryOperation

  import com.radiance.jvm.DiscriminatorConfig._
  implicit val encoder: Encoder[ParamsOfQueryOperation] =
    extras.semiauto.deriveConfiguredEncoder[ParamsOfQueryOperation]
}

case class ParamsOfSubscribeCollection(
  collection: String,
  filter: Option[Value],
  result: String
)

object ParamsOfSubscribeCollection {
  implicit val encoder: Encoder[ParamsOfSubscribeCollection] =
    deriveEncoder[ParamsOfSubscribeCollection]
}

case class ParamsOfWaitForCollection(
  collection: String,
  filter: Option[Value],
  result: String,
  timeout: Option[Long]
)

object ParamsOfWaitForCollection {
  implicit val encoder: Encoder[ParamsOfWaitForCollection] =
    deriveEncoder[ParamsOfWaitForCollection]
}

case class ResultOfAggregateCollection(values: Value)

object ResultOfAggregateCollection {
  implicit val decoder: Decoder[ResultOfAggregateCollection] =
    deriveDecoder[ResultOfAggregateCollection]
}

case class ResultOfBatchQuery(results: List[Value])

object ResultOfBatchQuery {
  implicit val decoder: Decoder[ResultOfBatchQuery] = deriveDecoder[ResultOfBatchQuery]
}

case class ResultOfFindLastShardBlock(block_id: String)

object ResultOfFindLastShardBlock {
  implicit val decoder: Decoder[ResultOfFindLastShardBlock] =
    deriveDecoder[ResultOfFindLastShardBlock]
}

case class ResultOfQuery(result: Value)

object ResultOfQuery {
  implicit val decoder: Decoder[ResultOfQuery] = deriveDecoder[ResultOfQuery]
}

case class ResultOfQueryCollection(result: List[Value])

object ResultOfQueryCollection {
  implicit val decoder: Decoder[ResultOfQueryCollection] =
    deriveDecoder[ResultOfQueryCollection]
}

case class ResultOfSubscribeCollection(handle: Long)

object ResultOfSubscribeCollection {
  implicit val codec: Codec[ResultOfSubscribeCollection] =
    deriveCodec[ResultOfSubscribeCollection]
}

case class ResultOfWaitForCollection(result: Value)

object ResultOfWaitForCollection {
  implicit val decoder: Decoder[ResultOfWaitForCollection] =
    deriveDecoder[ResultOfWaitForCollection]
}

object SortDirectionEnum {

  sealed trait SortDirection

  case object ASC extends SortDirection

  case object DESC extends SortDirection

  implicit val encoder: Encoder[SortDirection] =
    extras.semiauto.deriveEnumerationEncoder[SortDirection]
}
