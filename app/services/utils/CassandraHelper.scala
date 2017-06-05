package services.utils

import java.net.URI

import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.NoHostAvailableException

import scala.util.{Failure, Success, Try}

/**
  * Helper object for establishing cassandra session and setting up keyspace & tables
  *
  */
object CassandraHelper {
  val KEYSPACE = "tweets"
  val TWEET_TABLE = "tweets"
  val HASHTAG_TABLE = "hashtags"
  val POPULAR_HASHTAG_TABLE = "popularhashtags"

  private val cassandraUrl = CassandraUri("cassandra://127.0.0.1:9042/tweets")
  private val createKeyspace = s"CREATE KEYSPACE IF NOT EXISTS $KEYSPACE WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 } AND DURABLE_WRITES = true;"
  private val createTableQuery = s"CREATE TABLE IF NOT EXISTS $TWEET_TABLE (user varchar, tweet varchar , retweets int, PRIMARY KEY (user, tweet)) with comment='tweets table';"
  private val createHashTagTable = s"CREATE TABLE IF NOT EXISTS $HASHTAG_TABLE ( hashtag_count counter, hashtag varchar PRIMARY KEY ) with comment='hashtags table';"
  private val createPopularHashTagTable = s"CREATE TABLE IF NOT EXISTS $POPULAR_HASHTAG_TABLE ( hashtag_count int, hashtag varchar PRIMARY KEY ) with comment='popular hashtags table';"


  /**
    * Establishes Cassandra connection and attempts to set up Keyspace and tables required by Actions in CassandraController
    *
    * @return Try of Cassandra session
    */
  def createCassandraSession(uri: CassandraUri = cassandraUrl,
                             defaultConsistencyLevel: ConsistencyLevel = QueryOptions.DEFAULT_CONSISTENCY_LEVEL) : Try[Session] = {
    val cluster = new Cluster.Builder().
      addContactPoints(uri.hosts.toArray: _*).
      withPort(uri.port).
      withQueryOptions(new QueryOptions().setConsistencyLevel(defaultConsistencyLevel)).build

    try {
      val session = cluster.connect
      session.execute(createKeyspace)
      session.execute(s"USE ${uri.keyspace}")
      session.execute(createTableQuery)
      session.execute(createHashTagTable)
      session.execute(createPopularHashTagTable)
      Success(session)
    }
    catch {
      case nhae: NoHostAvailableException ⇒ printf("Unable to connect to Cassandra. Is it running?\n"); Failure(nhae)
      case e: Exception ⇒ Failure(e)
    }
  }
}

case class CassandraUri (s: String) {
  private val uri = new URI(s)

  private val additionalHosts = Option(uri.getQuery) match {
    case Some(query) => query.split('&').map(_.split('=')).filter(param => param(0) == "host").map(param => param(1)).toSeq
    case None => Seq.empty
  }

  val host = uri.getHost
  val hosts = Seq(uri.getHost) ++ additionalHosts
  val port = uri.getPort
  val keyspace = uri.getPath.substring(1)
}
