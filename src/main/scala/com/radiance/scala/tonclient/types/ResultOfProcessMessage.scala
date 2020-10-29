package com.radiance.scala.tonclient.types

import io.circe._
import io.circe.derivation._

case class ResultOfProcessMessage(
                                   /**
                                    * Parsed transaction.<p> In addition to the regular transaction fields there is a `boc` field encoded with `base64` which contains source transaction BOC.
                                    */
                                   transaction: String,

                                   /**
                                    * List of output messages' BOCs. Encoded as `base64`
                                    */
                                   outMessages: String,

                                   /**
                                    * Optional decoded message bodies according to the optional `abi` parameter.
                                    */
                                   decoded: String,

                                   /**
                                    * Transaction fees
                                    */
                                   fees: String
                                 )

object ResultOfProcessMessage {
  implicit val resultOfProcessMessageCodec: Codec[ResultOfProcessMessage] = deriveCodec[ResultOfProcessMessage]
}
