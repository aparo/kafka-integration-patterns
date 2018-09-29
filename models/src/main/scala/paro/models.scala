package paro

import io.circe.generic.JsonCodec

@JsonCodec
final case class GeoPoint(lat: Double, lon: Double)

@JsonCodec
final case class Geoname(
    geonameid: Long,
    name: String,
    asciiname: String,
    alternatenames: List[String],
    latitude: Float,
    longitude: Float,
    location: GeoPoint,
    fclass: String,
    fcode: String,
    country: String,
    cc2: String,
    admin1: Option[String],
    admin2: Option[String],
    admin3: Option[String],
    admin4: Option[String],
    population: Double,
    elevation: Int,
    gtopo30: Int,
    timezone: String,
    moddate: String
)
