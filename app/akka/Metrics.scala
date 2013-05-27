package akka

import akka.actor._
import akka.event.Logging

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json._

import scala.language.postfixOps
import scala.concurrent.duration._

import org.joda.time.DateTime

/** Actors related to Metrics collection */
object Metrics {
  val (out, channel) = Concurrent.broadcast[JsValue]

  /** SSE-Perf actor system */
  val system = ActorSystem("sse-perf")

  /** Supervisor for WS client */
  val wsClientSupervisor = system.actorOf(Props(new Supervisor(system.eventStream)).withDispatcher("my-thread-pool-dispatcher"), "WsClientSupervisor")

  /** Publishing status every 3 seconds */
  system.scheduler.schedule(3 seconds, 3 seconds, wsClientSupervisor, Publish)
  
  /** Protocol for Twitter Client actors */
  case class Received(bytes: Long)
  case class AddClients(n: Int, url: String)
  case object RemoveAllClients
  case object Publish
}

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

  def receive = {

    case Metrics.Received(bytes) => {
      bytesReceived += bytes
      bytesReceivedTotal += bytes
      activeClients.add(sender)
      chunksTotal += 1
      chunks += 1
    }

    case Metrics.AddClients(n, url) => {
      for (i <- 1 to n) {
        val client = context.actorOf(Props(new WsClient(url)).withDispatcher("my-thread-pool-dispatcher"))
        clients.add(client)
      }
    }

    case Metrics.RemoveAllClients => {
      context.children foreach { child => context.stop(child) }      
      clients = scala.collection.mutable.Set[ActorRef]()      
      WS.resetClient()
    }

    case Metrics.Publish => {

      val metrics = Json.obj(
        "bytesReceivedTotal" -> bytesReceivedTotal,
        "bytesReceived" -> bytesReceived,
        "msSinceLastReset" -> (DateTime.now.getMillis - lastReset.getMillis),
        "chunksTotal" ->  chunksTotal,
        "chunks" -> chunks,
        "activeClients" -> activeClients.size,
        "clients" -> clients.size)

      Metrics.channel.push(metrics)

      lastReset = DateTime.now
      bytesReceived = 0L
      activeClients = scala.collection.mutable.Set[ActorRef]()
      chunks = 0L
    }
  }
}

/** WS client actor. Not strictly necessary for establishing connections, only starts WS connection 
  * on creation and then passes metrics on to parent actor. Actor reference for is useful for 
  * counting active clients (in a Set of ActorRefs that gets emptied on each Publish message) */
class WsClient(url: String) extends Actor with ActorLogging {
  override val log = Logging(context.system, this)
  override def preStart() { }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, "Restarting due to [{}] when processing [{}]", reason.getMessage, message.getOrElse(""))
  }
  
  var bytesReceivedTotal: Long = 0L
  var bytesReceived: Long = 0L

  /** Iteratee for processing chunks from each WS client. */
  val chunkIteratee = Iteratee.foreach[Array[Byte]] { chunk => self ! Metrics.Received(chunk.size) }

  /** Connection to specified URL streaming chunks into supplied chunkIteratee */
  val conn = WS.url(url).withTimeout(-1).get(_ => chunkIteratee)
  
  /** Receives and passes on metrics. */
  def receive = {

    case Metrics.Received(bytes)  => {
      bytesReceived += bytes
      bytesReceivedTotal += bytes
      context.parent ! Metrics.Received(bytes)
    }
  }
} 