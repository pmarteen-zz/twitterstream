package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import play.api.libs.json.{JsValue, Json}
import twitter4j._

/**
  * Listener that creates an Akka source from a Twitter4j twitter stream
  *
  * Author: peter.marteen
  */

class MyTwitterListener {

  //Akka Actor system and materializer must be initialized
  implicit val system = ActorSystem("TwitterListener")
  implicit val materializer = ActorMaterializer()

  val ts: TwitterStream = TwitterStreamFactory.getSingleton

  /**
    * Registers listener to twitterStream and starts listening to all english tweets
    *
    * @return Akka Source of Tweets
    */
  def listen: Source[Tweet, NotUsed] = {

    // Create ActorRef Source producing Tweet events
    val (actorRef, publisher) = Source.actorRef[Tweet](100, OverflowStrategy.fail).toMat(Sink.asPublisher(false))(Keep.both).run

    val statusListener: StatusListener = new StatusListener {
      override def onStallWarning(stallWarning: StallWarning): Unit = {}

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}

      override def onScrubGeo(l: Long, l1: Long): Unit = {}

      //Statuses will be asynchronously sent to publisher actor
      override def onStatus(status: Status): Unit = {
        actorRef ! Tweet(status.getText, status.getUser.getName, status.getRetweetCount)
      }

      override def onTrackLimitationNotice(i: Int): Unit = {}

      override def onException(e: Exception): Unit = e.printStackTrace()
    }

    //Tie our listener to the TwitterStream and start listening
    ts.clearListeners()
    ts.addListener(statusListener)
    ts.sample("en")

    //Return Akka source of Tweets we just defined
    Source.fromPublisher(publisher)
  }

  def close() = {
    ts.cleanUp()
  }

  /**
    * Filters tweet stream for those containing hashtags
    */
  def hashtagSource: (Source[Set[Hashtag], NotUsed]) = {
    val src = this.listen
    src.filter(_.hashTags.nonEmpty).map(_.hashTags)
  }
}

final case class Hashtag(name : String)

//Representation of tweet
case class Tweet(val body : String, val user : String, val retweets : Int) {
  def hashTags: Set[Hashtag] =
    body.split(" |\n").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet

  //used to write tweet to table
  def serialize : String = {
    val trimBody = body.replace("'","")
    val trimUser = user.replace("'","")
    s"'$trimBody','$trimUser',$retweets"
  }
}
object Tweet {
  implicit val tweetInfoFormat = Json.format[Tweet]
  //Deserialize json format to Tweet object
  def apply(jsv : JsValue) : Tweet = {
    new Tweet((jsv \ "message").as[String], (jsv \ "author").as[String], (jsv \ "retweets").as[Int])
  }

  //Flow used to serialize tweets to json format
  def mapJson = Flow[Tweet].map { tweet =>
    Json.toJson(tweet)
  }
}
