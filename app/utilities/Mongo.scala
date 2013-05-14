package utilities

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api.MongoDriver
import play.modules.reactivemongo.json.collection.JSONCollection

/** Mongo connection object */
object Mongo {
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost:27017"))
  val db = connection("sse-perf")

  def accessLog: JSONCollection = db.collection[JSONCollection]("accessLog")
}