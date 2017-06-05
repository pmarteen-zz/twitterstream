package services.utils

import com.datastax.spark.connector._
import org.apache.spark.{SparkConf, SparkContext}
import CassandraHelper._

/**
  * Helper object for establishing spark connection with Cassandra
  */
object SparkHelper {
  private val SPARK_CONNECTION_HOST = "spark.cassandra.connection.host"
  private val HOME = "127.0.0.1"
  private val conf = new SparkConf().set(SPARK_CONNECTION_HOST, HOME)
  private val sparkContext = new SparkContext("local[*]","twitterstream", conf)

  def hashTagTable = sparkContext.cassandraTable(KEYSPACE, HASHTAG_TABLE)
}
