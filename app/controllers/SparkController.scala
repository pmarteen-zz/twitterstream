package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc.{Action, Controller}
import services.utils.CassandraHelper._
import services.utils.SparkHelper

/**
  * Controller using Spark interacting with Cassandra
  * See Cassandra section in README for Cassandra setup info.
  *
  * Author: peter.marteen
  */
class SparkController @Inject()(configuration: play.api.Configuration)(implicit system: ActorSystem, materializer: Materializer) extends Controller  {

  /**
    * Populates a Cassandra table of all hashtags contained in existing hashtag table that occurred more than once.
    * The keyspace used, source hashtag table name, and filtered hashtable table name are all defined in CassandraHelper.
    *
    * Note that the CassandraController.hashtagLoad action should be hit prior to this one to populate source table of hashtags
    */
  def filterPopularHashtags = Action {
    import com.datastax.spark.connector._

    val filterIndex = SparkHelper.hashTagTable.filter(r ⇒ r.getInt("hashtag_count") > 1)
    filterIndex.saveToCassandra(KEYSPACE, POPULAR_HASHTAG_TABLE)
    Ok(s"Loaded table : $POPULAR_HASHTAG_TABLE!")
  }

}
