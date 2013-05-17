import akka.actor.PoisonPill
import akka.Metrics
import play.api.GlobalSettings

object Global extends GlobalSettings {

  override def onStart(application: play.api.Application) { } 
   
  override def onStop(application: play.api.Application) {
    Metrics.wsClientSupervisor ! PoisonPill
  }
  
}
