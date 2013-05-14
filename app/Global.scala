import akka.actor.PoisonPill
import akka.Metrics
import utilities.Mongo
import play.api.GlobalSettings

object Global extends GlobalSettings {

  override def onStart(application: play.api.Application) { } 
   
  override def onStop(application: play.api.Application) {
    Mongo.connection.close()
    Metrics.wsClientSupervisor ! PoisonPill
  }
  
}
