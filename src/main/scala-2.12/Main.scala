import java.util.{Base64, Properties}
import java.io._
import java.util.concurrent.ExecutionException

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import play.api.libs.json._

/**
  * Created by Jochen Lutz on 2017-09-07.
  */
object Main {
  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def getStringOrDefault(path: String, default: String): String = if (underlying.hasPath(path)) {
      underlying.getString(path)
    } else {
      default
    }

    def getOptionalString(path: String): Option[String] = if (underlying.hasPath(path)) {
      Some(underlying.getString(path))
    } else {
      None
    }
  }

  val config = ConfigFactory.load

  val kafkaProps = new Properties()
  kafkaProps.put("bootstrap.servers", config.getStringOrDefault("kafka.server", "localhost:9092"))
  kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  kafkaProps.put("max.block.ms", config.getStringOrDefault("kafka.maxBlockMs", "5000"))

  val kafka: KafkaProducer[String, String] = try {
    new KafkaProducer[String, String](kafkaProps)
  }
  catch {
    case e: Exception => {
      println(s"Kafka startup error: ${e.getMessage}")
      System.exit(2)
      null
    }
  }

  val kafkaTopic = config.getStringOrDefault("kafka.topic", "org.streampipes.biggis.rasterdata.demo-source-endless-geotiff")
  val interval = config.getStringOrDefault("source.interval", "5").toInt

  def main(args: Array[String]) = {
    val allFiles = readAllFiles

    if (allFiles.isEmpty) {
      println("No tiles found. Exiting.")
      System.exit(1)
    }

    val messages = buildMessages(allFiles.get)

    var i = 0
    while (true) {
      try {
        val f = kafka.send(messages(i))
        f.get
      }
      catch {
        case e: ExecutionException => {
          println(s"Kafka execution exception: ${e.getMessage}")
        }
        case e: Exception => {
          println(s"Kafka not accessible: ${e.getMessage}")
        }
      }

      Thread.sleep(interval * 1000)
    }

    i = i+1

    if (i >= messages.size)
      i = 0
  }

  def readAllFiles: Option[List[File]] = {
    val folder = new File("tiles")
    if (folder.exists && folder.isDirectory)
      return Some(folder.listFiles.toList.sortBy(_.getName))

    return None
  }

  def buildMessages(files: List[File]): Array[ProducerRecord[String, String]] = {
    files.map(file => buildMessage(file)).toArray
  }

  def buildMessage(file: File): ProducerRecord[String, String] = {
    val fis = new FileInputStream(file)
    var content: Array[Byte] = new Array[Byte](file.length.toInt)
    fis.read(content)
    val b64 = Base64.getEncoder.encodeToString(content)

    val json = Json.obj(
      "latitude" -> 0,//TODO
      "longitude" -> 0,
      "altitude" -> 0,
      "raster-data" -> b64
    )

    return new ProducerRecord(kafkaTopic, "raster-data-test-1", json.toString())
  }
}
