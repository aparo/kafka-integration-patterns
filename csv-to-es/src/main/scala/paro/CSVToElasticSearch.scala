package paro

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.alpakka.elasticsearch.IncomingMessage
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchFlow
import akka.stream.scaladsl.{
  Broadcast,
  FileIO,
  Flow,
  GraphDSL,
  RunnableGraph,
  Sink
}
import akka.stream.{ActorMaterializer, ClosedShape}
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.system.SystemMetrics
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

import scala.concurrent.Await
import scala.util.Try

object CSVToElasticSearch extends App {

  // initializzazione monitoring
  Kamon.addReporter(new PrometheusReporter())
  SystemMetrics.startCollecting()
  val processedLines = Kamon.counter("processedLines")
  val italianGeonames = Kamon.counter("italianGeonames")
  val otherGeonames = Kamon.counter("otherGeonames")

  implicit var system = ActorSystem("IngestionSystem")
  implicit val materializer = ActorMaterializer()

  sys.ShutdownHookThread {
    import scala.concurrent.duration._
    Await.result(system.terminate(), 10.seconds)
    SystemMetrics.stopCollecting()
    println("Finished Closing")
  }

  implicit def emptyToOption(value: String): Option[String] = {
    if (value == null) return None
    val clean = value.trim
    if (clean.isEmpty) {
      None
    } else {
      Some(clean)
    }
  }

  def fixNullInt(value: Any): Int = {
    if (value == null) 0
    else {
      Try(value.asInstanceOf[Int]).toOption.getOrElse(0)
    }
  }

  implicit val client: RestClient =
    RestClient.builder(new HttpHost("0.0.0.0", 9200)).build()

  import spray.json._
  import DefaultJsonProtocol._

  implicit val geoPoint: JsonFormat[GeoPoint] = jsonFormat2(GeoPoint.apply)
  implicit val geoname: JsonFormat[Geoname] = jsonFormat20(Geoname.apply)

  RunnableGraph
    .fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val src = FileIO
          .fromPath(Paths.get("/opt/data/IT.txt"))
          .via(
            CsvParsing.lineScanner(
              delimiter = CsvParsing.Tab,
              maximumLineLength = 1024 * 1024,
              quoteChar = 0x01,
              escapeChar = 0x02
            )
          )
          .map(_.map(_.utf8String).toArray)
        //.via(CsvToMap.toMapAsStrings())

        val transformFlow = Flow[Array[String]].map { row =>
          val id = row(0).toLong
          val lat = row(4).toFloat
          val lon = row(5).toFloat
          val g = Geoname(
            id,
            row(1),
            row(2),
            Option(row(3))
              .map(_.split(",").map(_.trim).filterNot(_.isEmpty).toList)
              .getOrElse(Nil),
            lat,
            lon,
            GeoPoint(lat, lon),
            row(6),
            row(7),
            row(8),
            row(9),
            row(10),
            row(11),
            row(12),
            row(13),
            row(14).toDouble,
            fixNullInt(row(15)),
            row(16).toInt,
            row(17),
            row(18)
          )
          // we fire an increment
          processedLines.increment()
          g
        }
        val bcast = builder.add(Broadcast[Geoname](2))
        val filterItaly =
          Flow[Geoname].filter(g => g.country == "IT")
        val filterOthers =
          Flow[Geoname].filter(g => g.country != "IT")

        val toESJson = Flow[Geoname].map { geoname =>
          IncomingMessage(
            Some(geoname.geonameid.toString),
            geoname
          )

        }

        val esItalySink = ElasticsearchFlow
          .create[Geoname](
            indexName = "geonames-italy",
            typeName = "_doc"
          )

        val esOtherSink = ElasticsearchFlow
          .create[Geoname](
            indexName = "geonames-others",
            typeName = "_doc"
          )
        val sink = Sink.ignore

        src ~> transformFlow ~> bcast ~> filterItaly ~> toESJson
          .via(esItalySink) ~> sink
        bcast ~> filterOthers ~> toESJson.via(esOtherSink) ~> sink
        ClosedShape
    })
    .run()

}
