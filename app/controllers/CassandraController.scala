package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.datastax.driver.core.Session
import controllers.stages.LoadStage
import play.api.libs.EventSource
import play.api.mvc.{Action, Controller}
import services.utils.CassandraHelper._
import services.utils.{CassandraHelper, CassandraUri}
import services.{Hashtag, MyTwitterListener, Tweet}

import scala.concurrent.ExecutionContext

/**
  * Controller interacting with loading tweets into Cassandra.
  * See Cassandra section in README for setup info.
  *
  * Author: peter.marteen
  */
class CassandraController @Inject()(configuration: play.api.Configuration)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) extends Controller {
  val cUri = CassandraUri(configuration.underlying.getString("cassandra.uri"))
  val MAX_TWEETS = configuration.underlying.getInt("tweets.max")
  val twitterListener = new MyTwitterListener

  implicit val session: Session = CassandraHelper.createCassandraSession(cUri).get

  /**
    * Loads tweets from tweet stream into tweets table
    */
  def load = Action {

    val source = twitterListener.listen
    val loadFlow = LoadStage.getFlow((s: Session, tweet: Tweet) ⇒ s.execute(s"INSERT INTO $TWEET_TABLE (tweet, user, retweets) values (${tweet.serialize});"))
    //Flow stage using take, as the limit function throws an exception when threshold is exceeded
    val filter = Flow[Tweet].take(MAX_TWEETS)

    val flow = source.via(filter).via(loadFlow)
    Ok.chunked(flow via Tweet.mapJson via EventSource.flow)

  }
  /**
    * Loads hashtags from tweet stream into hashtags table
    */
  def hashtagLoad = Action {
    val source = twitterListener.hashtagSource

    val loadFlow = LoadStage.getFlow(loadHashtags)
    //Flow stage using take, as the limit function throws an exception when threshold is exceeded
    val filter = Flow[Set[Hashtag]].take(MAX_TWEETS)

    val hashtagSource = source.via(filter).via(loadFlow)

    Ok.chunked(hashtagSource.map(set ⇒ set.mkString) via EventSource.flow)

  }


  private def loadHashtags(s: Session, hashtags: Set[Hashtag]) = {
    for (hashTag ← hashtags) {
      s.execute(s"UPDATE $HASHTAG_TABLE SET hashtag_count = hashtag_count + 1 WHERE hashtag='" + hashTag.name.replace("'", "") + "';")
    }
  }
}
