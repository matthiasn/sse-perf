package controllers

import play.api.mvc._
import play.api.libs.EventSource
import akka.Metrics

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /** Serves Server Sent Events over HTTP connection */
  def metricsFeed = Action { implicit req =>  Ok.feed(Metrics.out &> EventSource()).as("text/event-stream") }
  
  def addClients(n: Int, url: String) = Action {
    implicit req => {
      Metrics.wsClientSupervisor ! Metrics.AddClients(n, url)
      Ok("AddClients message dispatched: " + n + " " + url)
    }
  }

  def removeClients(n: Int) = Action {
    implicit req => {
      Metrics.wsClientSupervisor ! Metrics.RemoveClients(n)
      Ok("RemoveClients message dispatched")
    }
  }
  
  def removeAllClients = Action {
    implicit req => {
      Metrics.wsClientSupervisor ! Metrics.RemoveAllClients
      Ok("RemoveAllClients message dispatched")
    }
  }
  
}