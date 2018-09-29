package paro

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape, IOResult}
import akka.util.ByteString
import io.circe.syntax._
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.system.SystemMetrics

import scala.concurrent.{Await, Future}
import scala.util.Try

object CSVToJson extends App {

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

  RunnableGraph
    .fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val src = FileIO
          .fromPath(Paths.get("/opt/data/allCountries.txt"))
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
          Flow[Geoname].filter(g => g.country=="IT")
        val filterOthers =
          Flow[Geoname].filter(g => g.country!="IT")

        val toJson = Flow[Geoname].map(_.asJson.noSpaces + "\n").map(ByteString(_))

        val fileItalySink: Sink[ByteString, Future[IOResult]] = // (2)
          FileIO.toPath(Paths.get("/tmp/italy.txt"))

        val fileOtherSink: Sink[ByteString, Future[IOResult]] = // (2)
          FileIO.toPath(Paths.get("/tmp/other.txt"))

        val sink = Sink.ignore

        src ~> transformFlow ~> bcast ~> filterItaly ~> toJson ~> fileItalySink
        bcast ~> filterOthers ~> toJson ~> fileOtherSink
        ClosedShape
    })
    .run()

}
