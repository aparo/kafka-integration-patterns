package paro

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.elasticsearch.IncomingMessage
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchFlow
import akka.stream.scaladsl.{Keep, Sink}
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.system.SystemMetrics
import org.apache.http.HttpHost
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  StringDeserializer
}
import org.elasticsearch.client.RestClient
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object KafkaToElasticSearch extends App {
  val ip =
    if (args.length > 0) args(0) else sys.env.getOrElse("KAFKA_BROKER", "kafka")

  // initializzazione monitoring
  Kamon.addReporter(new PrometheusReporter())
  SystemMetrics.startCollecting()
  val readRecords = Kamon.counter("readForES")
  val sentToElasticSearch = Kamon.counter("sentToES")

  implicit val system = ActorSystem("IngestionSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  sys.ShutdownHookThread {
    import scala.concurrent.duration._
    Await.result(system.terminate(), 10.seconds)
    SystemMetrics.stopCollecting()
    println("Finished Closing")
  }

  implicit val client: RestClient =
    RestClient.builder(new HttpHost("elasticsearch", 9200)).build()

  import spray.json._
  import DefaultJsonProtocol._

  implicit val geoPoint: JsonFormat[GeoPoint] = jsonFormat2(GeoPoint.apply)
  implicit val geoname: JsonFormat[Geoname] = jsonFormat20(Geoname.apply)

  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(s"$ip:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val control =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("italy"))
      .map { msg =>
        readRecords.increment()
        val geoname = io.circe.parser
          .parse(msg.record.value())
          .toOption
          .get
          .as[Geoname]
          .toOption
          .get
        IncomingMessage(
          Some(geoname.geonameid.toString),
          geoname,
          msg.committableOffset
        )
      }
      .via( // write to elastic
        ElasticsearchFlow
          .createWithPassThrough[Geoname, ConsumerMessage.CommittableOffset](
            indexName = "geonames-italy",
            typeName = "_doc"
          )
      )
      .map { messageResults =>
        messageResults
          .map { result =>
            if (!result.success)
              throw new Exception("Failed to write message to elastic")
            // Commit to kafka
            sentToElasticSearch.increment()
            result.passThrough
          }
          .maxBy(_.partitionOffset.offset)
          .commitScaladsl()
      }
      .toMat(Sink.ignore)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()

  Await.result(control.drainAndShutdown(), Duration.Inf)

}
