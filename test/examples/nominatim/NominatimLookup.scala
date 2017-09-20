package examples.nominatim

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.ws.ahc.AhcWSClient
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

// SEE: Prefix.cc Lookup - http://prefix.cc/foaf.file.json

class NominatimLookup {

  def nominatim(address: String) = {

    val url = "http://nominatim.openstreetmap.org/search"

    val parameters = Map(
      "q" -> address,
      "addressdetails" -> "1",
      "format" -> "json",
      "limit" -> "4",
      "addressdetails" -> "1",
      "dedupe" -> "1",
      "extratags" -> "1",
      "namedetails" -> "1").toList

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val wsClient = AhcWSClient()

    val ret = wsClient.url(url)
      .withQueryString(parameters: _*)
      .get()
      .map { response =>

        response.status match {
          case 200 => response.body
          case _   => "{}"
        }

      }
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

    ret

  }

}

object MainNominatimLookup extends App {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val client = new NominatimLookup

  val json_mapper = new ObjectMapper
  val json_reader = json_mapper.reader()

  val result = Await.ready(client.nominatim("135 pilkington avenue, birmingham"), Duration.Inf)
    .value.get.get

  val json_list: List[JsonNode] = json_reader.readTree(result).elements().toList

  // simulazione di output...
  if (json_list.size > 0) {
    println(s"RESULTS [${json_list.size}]")
    json_list
      .zipWithIndex
      .foreach {
        case (node, i) =>
          println(s"result ${i + 1}")
          println(node.get("place_id"))
          println(node.get("address").get("road").asText() + ", " + node.get("address").get("house_number").asText())
      }
  } else {
    println("cannot find results...")
  }

}

