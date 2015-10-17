package com.company.pack

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.nio.file.{Paths, Path}
import java.util.{UUID, Random}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import spray.client.pipelining._
import spray.http.{BodyPart, MediaTypes, MultipartFormData}

import scala.util.Try

object ClientFileUpload extends App {

  implicit val system = ActorSystem("simple-spray-client")

  import system.dispatcher

  val config = ConfigFactory.load()
  val host = Try(config.getString("http.host")).getOrElse("127.0.0.1")
  val port = Try(config.getInt("http.port")).getOrElse(8080)

  // execution context for futures below

  val pipeline = sendReceive
  val fileName = Paths.get("/tmp").resolve(UUID.randomUUID().toString).toAbsolutePath

  generateFile(fileName, 100)
  val payload = MultipartFormData(Seq(BodyPart(fileName.toFile, "file", MediaTypes.`multipart/form-data`)))
  val request = Post(s"http://$host:$port/upload", payload)

  pipeline(request).onComplete { res =>
    println(res)
    system.shutdown()
  }

  /**
   * Generate file by particular size
   *
   * @param filePath - path to file
   * @param size
   * @return
   */
  def generateFile(filePath: Path, size: Long): File = {
    val file: File = filePath.toAbsolutePath.toFile
    System.out.println(filePath.toAbsolutePath.toString)
    val bufferedOutputStream: BufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))

    val random: Random = new Random
    val bytes1Kb: Array[Byte] = new Array[Byte](1024)

      var i: Long = 0
      while (i < size) {
        {
          random.nextBytes(bytes1Kb)
          bufferedOutputStream.write(bytes1Kb)
        }
        {
          i += 1
        }
      }


    bufferedOutputStream.flush()
    bufferedOutputStream.close()
    file
  }

}


