package com.radiance.jvm.boc

import com.radiance.jvm.Context

import scala.concurrent.Future

class BocModule(private val ctx: Context) {

  /**
   * @param block_boc
   *   Key block BOC encoded as base64
   */
  def getBlockchainConfig(
    block_boc: String
  ): Future[Either[Throwable, ResultOfGetBlockchainConfig]] = {
    ctx.execAsync[ParamsOfGetBlockchainConfig, ResultOfGetBlockchainConfig](
      "boc.get_blockchain_config",
      ParamsOfGetBlockchainConfig(block_boc)
    )
  }

  /**
   * Calculates BOC root hash
   * @param boc
   *   BOC encoded as base64
   */
  def getBocHash(boc: String): Future[Either[Throwable, ResultOfGetBocHash]] = {
    ctx.execAsync[ParamsOfGetBocHash, ResultOfGetBocHash]("boc.get_boc_hash", ParamsOfGetBocHash(boc))
  }

  /**
   * Extracts code from TVC contract image
   * @param tvc
   *   tvc
   */
  def getCodeFromTvc(
    tvc: String
  ): Future[Either[Throwable, ResultOfGetCodeFromTvc]] = {
    ctx.execAsync[ParamsOfGetCodeFromTvc, ResultOfGetCodeFromTvc]("boc.get_code_from_tvc", ParamsOfGetCodeFromTvc(tvc))
  }

  /**
   * Parses account boc into a JSON
   *
   * JSON structure is compatible with GraphQL API account object
   * @param boc
   *   BOC encoded as base64
   */
  def parseAccount(boc: String): Future[Either[Throwable, ResultOfParse]] = {
    ctx.execAsync[ParamsOfParse, ResultOfParse]("boc.parse_account", ParamsOfParse(boc))
  }

  /**
   * Parses block boc into a JSON
   *
   * JSON structure is compatible with GraphQL API block object
   * @param boc
   *   BOC encoded as base64
   */
  def parseBlock(boc: String): Future[Either[Throwable, ResultOfParse]] = {
    ctx.execAsync[ParamsOfParse, ResultOfParse]("boc.parse_block", ParamsOfParse(boc))
  }

  /**
   * Parses message boc into a JSON
   *
   * JSON structure is compatible with GraphQL API message object
   * @param boc
   *   BOC encoded as base64
   */
  def parseMessage(boc: String): Future[Either[Throwable, ResultOfParse]] = {
    ctx.execAsync[ParamsOfParse, ResultOfParse]("boc.parse_message", ParamsOfParse(boc))
  }

  /**
   * Parses shardstate boc into a JSON
   *
   * JSON structure is compatible with GraphQL API shardstate object
   * @param boc
   *   BOC encoded as base64
   * @param id
   *   Shardstate identificator
   * @param workchain_id
   *   Workchain shardstate belongs to
   */
  def parseShardstate(
    boc: String,
    id: String,
    workchain_id: Int
  ): Future[Either[Throwable, ResultOfParse]] = {
    ctx.execAsync[ParamsOfParseShardstate, ResultOfParse](
      "boc.parse_shardstate",
      ParamsOfParseShardstate(boc, id, workchain_id)
    )
  }

  /**
   * Parses transaction boc into a JSON
   *
   * JSON structure is compatible with GraphQL API transaction object
   * @param boc
   *   BOC encoded as base64
   */
  def parseTransaction(
    boc: String
  ): Future[Either[Throwable, ResultOfParse]] = {
    ctx.execAsync[ParamsOfParse, ResultOfParse]("boc.parse_transaction", ParamsOfParse(boc))
  }

}
