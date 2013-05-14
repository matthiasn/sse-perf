package controllers

import utilities.RequestLogger
import play.api.mvc._
import play.api.libs.EventSource
import akka.Metrics
import play.api.Play._

object Application extends Controller {
  
  val accessToken = current.configuration.getString("metrics.accessToken").getOrElse("")
    
  def index(token: String) = Action {
    implicit req => {
      RequestLogger.log(req, "/", 200)
      Ok(views.html.index(token)) 
    }
  }

  /** Serves Server Sent Events over HTTP connection */
  def metricsFeed = Action {
    implicit req => {
      RequestLogger.log(req, "/metricsFeed", 200)      
      Ok.feed(Metrics.out &> EventSource()).as("text/event-stream")
    }
  }

  def addClients(n: Int, url: String, token: String) = Action {
    implicit req => {
      if (token == accessToken) {
        RequestLogger.log(req, "/clients/add", 200)
        Metrics.wsClientSupervisor ! Metrics.AddClients(n, url)
        Ok("AddClients message dispatched: " + n + " " + url)
      }
      else {
        RequestLogger.log(req, "/clients/add", 401)
        Unauthorized("Sorry, but no.")
      }
    }
  }
  
  def removeAllClients(token: String) = Action {
    implicit req => {
      if (token == accessToken) {
        RequestLogger.log(req, "/clients/removeAll", 200)
        Metrics.wsClientSupervisor ! Metrics.RemoveAllClients
        Ok("RemoveAllClients message dispatched")
      }
      else {
        RequestLogger.log(req, "/clients/removeAll", 401)
        Unauthorized("Sorry, but no.")
      }
    }
  }
  
}