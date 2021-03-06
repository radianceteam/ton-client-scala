package com.radiance.jvm

import cats.implicits._
import com.radiance.jvm.app.AppObject
import com.radiance.jvm.client.ClientConfig
import com.typesafe.scalalogging.Logger
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Success, Try}

object Context {

  sealed trait OperationSystem
  case object Windows extends OperationSystem
  case object Linux extends OperationSystem
  case object MacOs extends OperationSystem
  case object UndefinedOs extends OperationSystem

  def define: OperationSystem =
    System.getProperty("os.name").toLowerCase match {
      case x if x.contains("win")                                           => Windows
      case x if x.contains("nix") || x.contains("nux") || x.contains("aix") => Linux
      case x if x.contains("mac")                                           => MacOs
    }

  private def trimExtension(name: String): String = name.split("\\.").head

  private def init(): Unit = {
    val libName: String = (define match {
      case Windows     =>
        List(
          "libton_client_scala_bridge.dll", //mingw64
          "cygton_client_scala_bridge.dll", //cygwin
          "ton_client_scala_bridge.dll" // win
        )
      case Linux       =>
        List(
          "libton_client_scala_bridge.so" // linux
        )
      case MacOs       => List()
      case UndefinedOs =>
        throw new IllegalStateException("Can't define current operation system")
    })
      .find(n => getClass.getResource(s"/$n") != null)
      .getOrElse(
        throw new IllegalStateException("Can't find compiled bridge library")
      )

    try {
      System.loadLibrary(trimExtension(libName))
    } catch {
      case _: Throwable => loadFromPath(libName)
    }
  }

  private def loadFromPath(name: String): Unit = {
    val tempDir = Files.createTempDirectory("ton_bridge")
    val tempDirPath = tempDir.toAbsolutePath
    copyFileFromClasspath(tempDirPath, name)
    System.load(Paths.get(tempDirPath.toString).resolve(name).toString)
  }

  private def copyFileFromClasspath(tempDir: Path, fileName: String): Unit = {
    val byteArr =
      Files.readAllBytes(Paths.get(getClass.getResource(s"/$fileName").toURI))
    Files.write(tempDir.resolve(fileName), byteArr)
    ()
  }

  def apply(config: ClientConfig)(implicit
    ec: ExecutionContext
  ): Context = {
    val ctx = new Context(-1)
    val create = ctx.createContext(config.asJson.deepDropNullValues.noSpaces)
    (for {
      json <- parse(create)
      response <- json.as[CreateContextResponse]
      res <- response.error match {
               case None        => ctx.setCtxId(response.result).asRight
               case Some(error) =>
                 new RuntimeException(
                   error.asJson.deepDropNullValues.spaces2
                 ).asLeft
             }
    } yield res).fold(
      t => throw t,
      identity
    )
  }
  init()
}

class Context private (var contextId: Int)(implicit
  val ec: ExecutionContext
) {

  private val logger = Logger[Context]

  private val maxId = new AtomicInteger(0)

  private val callbackMap: ConcurrentHashMap[Promise[String], Request] =
    new ConcurrentHashMap[Promise[String], Request]()

  private val appPromiseMap: ConcurrentHashMap[Int, Promise[String]] =
    new ConcurrentHashMap[Int, Promise[String]]()

  private val appCallbackMap: ConcurrentHashMap[Int, AppObject[Any, Any]] =
    new ConcurrentHashMap[Int, AppObject[Any, Any]]()

  @nowarn
  @native private[jvm] def createContext(config: String): String

  @nowarn
  @native private[jvm] def destroyContext(context: Int): Unit

  @nowarn
  @native private[jvm] def asyncRequest(
    context: Int,
    functionName: String,
    params: String,
    promise: Promise[String]
  ): Unit

  @nowarn
  @native private[jvm] def asyncRequestWithAppId(
    context: Int,
    functionName: String,
    params: String,
    appId: Int
  ): Unit

  @nowarn
  @native private[jvm] def unregisterAppId(
    context: Int,
    functionName: String,
    params: String,
    appId: Int
  ): Unit

  @nowarn
  @native private[jvm] def syncRequest(
    context: Int,
    functionName: String,
    params: String
  ): String

  private[jvm] def asyncHandler(
    code: Int,
    params: String,
    promise: Promise[String],
    finished: Boolean
  ): Unit = {
    OperationCode.fromInt(code) match {
      case SuccessCode =>
        promise.tryComplete(Success(Option(params).getOrElse("")))

      case ErrorCode =>
        callbackMap.remove(promise)
        val cursor = parse(params).getOrElse(Json.Null).hcursor
        val exception = (for {
          code <- cursor.get[Int]("code")
          message <- cursor.get[String]("message")
        } yield new IllegalStateException(s"Code: $code; Message: $message"))
          .getOrElse(throw new RuntimeException(s"Unexpected message format: $params"))
        promise.tryFailure(exception)

      case NopCode    =>
        logger.debug(s"Nop operation code was received.")
      case Unknown(i) =>
        logger.debug(s"Unknown operation code was received. Value: $i")

      case CustomCode    =>
        Option(callbackMap.get(promise))
          .foreach(
            callback =>
              callback(
                parse(params).getOrElse(
                  Json.fromString(s"Can't parse params in callback:\n$params")
                )
              )
          )
      case AppNotifyCode =>
        throw new IllegalStateException("Unexpected response AppNotifyCode")

      case AppRequestCode =>
        throw new IllegalStateException("Unexpected response AppRequestCode")
    }
    if (finished) {
      callbackMap.remove(promise)
    }
    ()
  }

  // TODO fix finished param
  @nowarn
  private[jvm] def asyncHandlerWithAppId(
    code: Int,
    params: String,
    appId: Int,
    finished: Boolean
  ): Unit = {
    OperationCode.fromInt(code) match {
      case SuccessCode    =>
        logger.debug("SuccessCode was received")
        logger.debug(s"Params: $params")
        Option(appPromiseMap.remove(appId)).foreach(p => p.success(params))
      case ErrorCode      =>
        logger.error(s"ErrorCode was received. Message: $params")
        Option(appPromiseMap.remove(appId)).foreach(p => p.failure(new IllegalStateException(params)))
      case NopCode        =>
        logger.debug("NopCode was received")
      case CustomCode     =>
        logger.debug("CustomCode was received")
      case AppNotifyCode  =>
        logger.debug("AppNotifyCode was received")
      case AppRequestCode =>
        logger.debug("AppRequestCode was received")
        logger.debug(s"Params: $params")
        val app = appCallbackMap.get(appId)
        val str = app.resolveRequest(params)
        logger.debug(s"Response: $str")
        asyncRequestWithAppId(contextId, app.functionName, str, appId)

    }
  }

  private[Context] def setCtxId(i: Int): Context = {
    contextId = i
    this
  }

  def destroy(): Unit = {
    if (contextId >= 0) {
      destroyContext(contextId)
      contextId = -1
    }
  }

  private def callNativeAsync(
    functionName: String,
    params: String
  ): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    asyncRequest(contextId, functionName, params, promise)
    promise.future
  }

  private def callNativeAsyncWithCallback(
    functionName: String,
    params: String,
    callback: Request
  ): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    callbackMap.put(promise, callback)
    asyncRequest(contextId, functionName, params, promise)
    promise.future
  }

  private[jvm] def registerAppObject[Out: Decoder, T, V](
    functionName: String,
    params: String,
    app_object: AppObject[T, V]
  ): Future[Either[Throwable, Out]] = {
    val promise: Promise[String] = Promise[String]()
    val appId = maxId.incrementAndGet()
    appPromiseMap.put(appId, promise)
    appCallbackMap.put(appId, app_object.asInstanceOf[AppObject[Any, Any]])
    asyncRequestWithAppId(contextId, functionName, params, appId)
    promise.future
      .map(r => parse(r).flatMap(_.as[Out]))
      .recover { case e => e.asLeft }
  }

  private[jvm] def executeWithAppObject[T: Encoder](
    functionName: String,
    arg: T,
    appId: Int
  ): Future[Either[Throwable, Unit]] = {
    val promise: Promise[String] = Promise[String]()
    appPromiseMap.put(appId, promise)
    asyncRequestWithAppId(contextId, functionName, arg.asJson.noSpaces, appId)
    promise.future.map(_ => ().asRight).recover { case e => e.asLeft }
  }

  private[jvm] def unregisterAppObject(
    handleValue: Int,
    functionName: String
  ): Future[Either[Throwable, Unit]] = {
    val promise: Promise[String] = Promise[String]()
    val appId = handleValue
    appPromiseMap.put(appId, promise)
    appCallbackMap.remove(appId)
    unregisterAppId(contextId, functionName, s"""{"handle":$handleValue}""", appId)
    promise.future.map(_ => ().asRight).recover { case e => e.asLeft }
  }

  private[jvm] def execAsync[In: Encoder, Out: Decoder](
    functionName: String,
    arg: In
  ): Future[Either[Throwable, Out]] =
    callNativeAsync(functionName, arg.asJson.deepDropNullValues.noSpaces)
      .map(r => parse(r).flatMap(_.as[Out]))
      .recover { case e => e.asLeft }

  private[jvm] def execSync[In: Encoder, Out: Decoder](
    functionName: String,
    arg: In
  ): Either[Throwable, Out] = {
    for {
      response <- Try {
                    syncRequest(
                      contextId,
                      functionName,
                      arg.asJson.deepDropNullValues.noSpaces
                    )
                  }.toEither
      json <- parse(response)
      obj <- json.as[OperationResponse[Out]]
      res <- obj match {
               case OperationResponse(Right(r)) => r.asRight
               case OperationResponse(Left(t))  => new Exception(t.asJson.spaces2).asLeft
             }
    } yield res
  }

  private[jvm] def execAsyncWithCallback[In: Encoder, Out: Decoder](
    functionName: String,
    arg: In,
    callback: Request
  ): Future[Either[Throwable, Out]] =
    callNativeAsyncWithCallback(
      functionName,
      arg.asJson.deepDropNullValues.noSpaces,
      callback
    ).map(r => parse(r).flatMap(_.as[Out])).recover { case e => e.asLeft }

}
