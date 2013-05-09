package akka

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import akka.actor._
import akka.event.Logging
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json._

import scala.concurrent.duration._
import org.joda.time.DateTime

/** Actors related to Metrics collection */
object Metrics {

  val (out, metricsChannel) = Concurrent.broadcast[JsValue]
  
  /** BirdWatch actor system */
  val system = ActorSystem("sse-perf")

  /** Supervisor for Tweet stream client */
  val wsClientSupervisor = system.actorOf(Props(new Supervisor(system.eventStream)), "WsClientSupervisor")

  /** Checking status of Twitter Streaming API connection every 30 seconds */
  system.scheduler.schedule(3 seconds, 3 seconds, wsClientSupervisor, PublishMetrics)
  
  /** Protocol for Twitter Client actors */
  case class Received(bytes: Long)
  case class AddClients(n: Int, url: String)
  case object RemoveAllClients
  case object PublishMetrics

  class Supervisor(eventStream: akka.event.EventStream) extends Actor with ActorLogging {
    override val log = Logging(context.system, this)
    override def preStart() { println("WsClient Supervisor starting") }
    override def preRestart(reason: Throwable, message: Option[Any]) {
      log.error(reason, "Restarting due to [{}] when processing [{}]", reason.getMessage, message.getOrElse(""))
    }

    var bytesReceivedTotal: Long = 0L
    var bytesReceived: Long = 0L    
    var chunksTotal: Long = 0L 
    var chunks: Long = 0L
    var lastReset = DateTime.now    
    var activeClients = scala.collection.mutable.Set[ActorRef]()
    var clients = scala.collection.mutable.Set[ActorRef]()
    
    /** Receives control messages for starting / restarting supervised client and adding or removing topics */
    def receive = {

      case Received(bytes)  => {
        bytesReceived += bytes
        bytesReceivedTotal += bytes
        activeClients.add(sender)
        chunksTotal += 1
        chunks += 1
      }

      case AddClients(n, url) => {
        for (i <- 1 to n) {
          val client = context.actorOf(Props(new WsClient(url)))
          clients.add(client)
        }
      }

      case RemoveAllClients => {
        clients.foreach { ref => context.stop(ref) }

        WS.resetClient()
        clients = scala.collection.mutable.Set[ActorRef]()
      }

      case PublishMetrics => {

        metricsChannel.push( Json.obj(
          "bytesReceivedTotal" -> bytesReceivedTotal,
          "bytesReceived" -> bytesReceived,
          "msSinceLastReset" -> (DateTime.now.getMillis - lastReset.getMillis),
          "chunksTotal" ->  chunksTotal,
          "chunks" -> chunks,
          "activeClients" -> activeClients.size,
          "clients" -> clients.size )
        )
       
        lastReset = DateTime.now
        bytesReceived = 0L    
        activeClients = scala.collection.mutable.Set[ActorRef]()
        chunks = 0L
      }
       
    }
  }

  /** Image retrieval actor, receives Tweets, retrieves the Twitter profile images for each user and passes them on to
    * conversion actor. */
  class WsClient(url: String) extends Actor with ActorLogging {
    override val log = Logging(context.system, this)
    override def preStart() { }
    override def preRestart(reason: Throwable, message: Option[Any]) {
      log.error(reason, "Restarting due to [{}] when processing [{}]", reason.getMessage, message.getOrElse(""))
    }

    var bytesReceivedTotal: Long = 0L
    var bytesReceived: Long = 0L

    /** Iteratee for processing each chunk from Twitter stream of Tweets. Parses Json chunks 
      * as Tweet instances and publishes them to eventStream. */
    val chunkIteratee = Iteratee.foreach[Array[Byte]] { chunk => self ! Received(chunk.size) }
    
    val conn = WS.url(url).withTimeout(-1).get(_ => chunkIteratee)

    /** Connects to Twitter Streaming API and retrieve a stream of Tweets for the specified search word or words.
      * Passes received chunks of data into tweetIteratee */
    def receive = {

      case Received(bytes)  => {
        bytesReceived += bytes
        bytesReceivedTotal += bytes
        context.parent ! Received(bytes)
      }
    }
  } 
  
}