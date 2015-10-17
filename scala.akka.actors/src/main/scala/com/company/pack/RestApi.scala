package com.company.pack

import akka.actor.ActorLogging
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import spray.http.MediaTypes._
import spray.http.MultipartFormData
import spray.routing.{HttpService, HttpServiceActor}
import spray.http._
import spray.routing._
import spray.http.BodyPart
import java.io.{ ByteArrayInputStream, InputStream, OutputStream }

/**
 *
 */
class RestApiActor(_requestTimeout: Timeout) extends HttpServiceActor with RestApi with ActorLogging {

  override def actorRefFactory = context

  implicit val requestTimeout = _requestTimeout

  def receive = runRoute(rootRoute)
}

trait RestApi extends HttpService with LazyLogging {

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  // catch exceptions - https://github.com/eigengo/activator-akka-spray/blob/master/src%2Fmain%2Fscala%2Fapi%2Fservices.scala

  val rootRoute = {
    get {
      pathSingleSlash {
        complete("DEFAULT_GET_VALUE")
      } ~
        path("value" / Segment) { id =>
          complete(s"get:$id")
        }
    } ~
      post {
        pathSingleSlash {
          complete("DEFAULT_POST_VALUE")
        } ~
          path("upload") {
            respondWithMediaType(`application/json`) {
              entity(as[MultipartFormData]) { formData =>
//                detachTo(singleRequestServiceActor) {
                  complete {
                    val details = formData.fields.map {
                      case (BodyPart(entity, headers)) =>
                        //val content = entity.buffer
                        val content = new ByteArrayInputStream(entity.data.toByteArray)
                        val contentType = "NONE";//Option(headers.find(h => h.is("Content-Type")).get.value).getOrElse("NO-TYPE")
                        val fileName = headers.find(h => h.is("Content-Disposition")).get.value.split("filename=").last

                        logger.info(s"size:$entity.data.length")
                        logger.info(s"contentType:$contentType")
                        logger.info(s"Find a file name:$fileName")

                        val result = true;//saveAttachment(fileName, content)
                        (contentType, fileName, result)
                      case _ =>
                    }
                    s"""{"status": "Processed POST request, details=$details" }"""
                  }
//                }
              }
            }

          }
      } ~
      // shutdown actors by command
      (put | parameter('method ! "put")) {
        path("stop") {
          complete {
//            actorSystem.shutdown()
            "Shutting down in 1 second..."
          }
        }
      }

  }
//
//
//  private def saveAttachment(fileName: String, content: Array[Byte]): Boolean = {
//    saveAttachment[Array[Byte]](fileName, content, {(is, os) => os.write(is)})
//    true
//  }
//
//  private def saveAttachment(fileName: String, content: InputStream): Boolean = {
//    saveAttachment[InputStream](fileName, content,
//    { (is, os) =>
//      val buffer = new Array[Byte](16384)
//      Iterator
//        .continually (is.read(buffer))
//        .takeWhile (-1 !=)
//        .foreach (read=>os.write(buffer,0,read))
//    }
//    )
//  }
//
//  private def saveAttachment[T](fileName: String, content: T, writeFile: (T, OutputStream) => Unit): Boolean = {
//    try {
//      val fos = new java.io.FileOutputStream(fileName)
//      writeFile(content, fos)
//      fos.close()
//      true
//    } catch {
//      case _ => false
//    }
//  }
}


//            entity(as[MultipartFormData]) { data =>
//              logger.info("Inside multipart")
//              complete {
//                data.fields.get("files[]") match {
//                  case Some(imageEntity) =>
//                    val size = imageEntity.entity.buffer.length
//                    logger.info(s"Uploaded $size")
//                    "OK"
//                  case None =>
//                    logger.info("No files")
//                    "Not OK"
//                }
//              }
//            }

