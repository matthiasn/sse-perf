package controllers

import play.api.mvc._
import play.api.Play._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utilities.RequestLogger

object AccessLog extends Controller {
  
  val accessToken = current.configuration.getString("metrics.accessToken").getOrElse("")

  def index(token: String) = Action {
    implicit req => {
      if (token == accessToken) {
        RequestLogger.log(req, "/accessLog", 200)
        Async { RequestLogger.latestVisitor(500).map { visitors => Ok(views.html.accesslog(visitors)) }   
        }
      }
      else {
        RequestLogger.log(req, "/accessLog", 401)
        Unauthorized("Sorry, but no.")
      }
    }
  }
  
}