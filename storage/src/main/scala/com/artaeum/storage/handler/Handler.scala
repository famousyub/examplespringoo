package com.artaeum.storage.handler

import java.io.File
import java.nio.file.Files
import java.util.Base64

import colossus.core.ServerContext
import colossus.protocols.http.HttpMethod.{Delete, Get, Post}
import colossus.protocols.http.UrlParsing.{/, Root, on}
import colossus.protocols.http.{ContentType, Http, HttpBody, HttpBodyDecoder, HttpBodyEncoder, HttpCodes, HttpResponse, RequestHandler}
import colossus.service.Callback
import colossus.service.GenRequestHandler.PartialHandler
import com.artaeum.storage.model.Image
import com.artaeum.storage.service.ImageService
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromArray}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

import scala.util.{Failure, Properties, Success, Try}

class Handler(ctx: ServerContext) extends RequestHandler(ctx) {

  val USER: String = "storage"
  val PASSWORD: String = Properties.envOrElse("STORAGE_SERVICE_PASSWORD", "password")

  implicit val imageEncoder: HttpBodyEncoder[File] = new HttpBodyEncoder[File] {
    override def encode(data: File): HttpBody = new HttpBody(Files.readAllBytes(data.toPath))
    override def contentType: String = "image/jpeg"
  }

  implicit val fileEntityDecoder: HttpBodyDecoder[Image] = new HttpBodyDecoder[Image] {
    implicit val codec: JsonValueCodec[Image] = JsonCodecMaker.make[Image](CodecMakerConfig())
    override def decode(body: Array[Byte]): Try[Image] = Try(readFromArray[Image](body))
  }

  override def handle: PartialHandler[Http] = {
    case req @ Get on Root / "storage" / "health" =>
      Callback.successful(req
        .ok("""{"status":"UP"}""")
        .withContentType(ContentType.ApplicationJson))
    case req @ Get on Root / "storage" / "images" / resource / name =>
      val image = ImageService.load(resource, name)
      if (image.exists) {
        Callback.successful(req.ok(image))
      } else {
        Callback.successful(req
          .notFound("""{"error":"Not Found"}""")
          .withContentType(ContentType.ApplicationJson))
      }
    case req @ Post on Root / "storage" / "images" / resource => this.ifAuth(req, () =>
      req.body.as[Image] match {
        case Success(image) =>
          ImageService.save(image, resource)
          Callback.successful(req.ok("OK"))
        case Failure(_) => Callback.successful(req
          .notFound("""{"error":"Bad Request"}""")
          .withContentType(ContentType.ApplicationJson))
      }
    )
    case req @ Delete on Root / "storage" / "images" / resource / name =>
      ImageService.delete(resource, name)
      Callback.successful(req
        .ok("""{"message":"OK"}"""")
        .withContentType(ContentType.ApplicationJson))
  }

  private def ifAuth(req: Request, fn: () => Callback[HttpResponse]): Callback[HttpResponse] = {
    if (req.head.headers.firstValue("Authorization").contains(this.authHeader)) {
      fn()
    } else {
      Callback.successful(req
        .respond(HttpCodes.UNAUTHORIZED, """{"error":"UNAUTHORIZED"}""")
        .withContentType(ContentType.ApplicationJson))
    }
  }

  private def authHeader: String = "Basic %s".format(
    Base64.getEncoder.encodeToString("%s:%s".format(USER, PASSWORD).getBytes()))
}
