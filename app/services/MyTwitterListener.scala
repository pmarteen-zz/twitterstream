package services

import java.util.concurrent.Executors

import akka.NotUsed
import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import twitter4j._

import scala.concurrent.{Future, ExecutionContext}

/**
  * Listener that "Akka"-fies and listens to Twitter4j twitter stream
  *
  * Author: peter.marteen
  */
final case class Hashtag(name : String)

case class Tweet(body : String, user : String) {
  def hashTags: Set[Hashtag] =
    body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
}

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
    val (actorRef, publisher) = Source.actorRef[Tweet](100, OverflowStrategy.dropHead).toMat(Sink.asPublisher(false))(Keep.both).run()

    val statusListener: StatusListener = new StatusListener {
      override def onStallWarning(stallWarning: StallWarning): Unit = {}

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}

      override def onScrubGeo(l: Long, l1: Long): Unit = {}

      //Statuses will be asynchronously sent to publisher actor
      override def onStatus(status: Status): Unit = {
        actorRef ! new Tweet(status.getText, status.getUser.getName)
      }

      override def onTrackLimitationNotice(i: Int): Unit = {}

      override def onException(e: Exception): Unit = e.printStackTrace()
    }

    //Tie our listener to the TwitterStream and start listening
    ts.addListener(statusListener)
    ts.sample("en")

    //Return Akka source of Tweets we just defined
    Source.fromPublisher(publisher)
  }

  /**
    * Filters tweet stream for those containing hashtags
    *
    * @return Future of Seq containing set of hashtags in each tweet
    */
  def hashTags = {
    val source: Source[Tweet, NotUsed] = this.listen

    source.filter(_.hashTags.nonEmpty).take(100).map(_.hashTags).runWith(Sink.seq)
  }

}


class HelloActor extends Actor {
  def receive = {
    case msg: String => sender ! s"Hello, $msg"
  }
}

