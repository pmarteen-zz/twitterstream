package controllers

import java.util.concurrent.Executors
import javax.inject.Inject

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Inbox, Props}
import akka.stream.Materializer
import akka.stream.scaladsl._
import play.api.mvc._
import services.{Hashtag, HelloActor, MyTwitterListener, Tweet}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Controller interacting with twitter listener
  *
  * Author: peter.marteen
  */

class TwitterListenerController @Inject() (implicit system : ActorSystem, materializer : Materializer) extends Controller {

  val MAX_TWEETS = 100

  val twitterListener = new MyTwitterListener
  val actorRef : ActorRef = system.actorOf(Props[HelloActor], "helloActor")

  implicit val ec : ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  /**
    * Tests actor to actor communication.  Inbox is actor created on the fly to retrieve response from TwitActor
    */
  def actor = Action {

    val inbox = Inbox.create(system)
    inbox.send(actorRef, "Peter")

    Ok(inbox.receive(5.seconds).asInstanceOf[String])
  }

  /**
    * Displays tweets from TwitterListener tweet stream
    */
  def stream = Action {
      val source: Source[Tweet, NotUsed] = twitterListener.listen

      Ok.chunked(source.map { tweet =>
        "Text: "  + tweet.body + " User: "+ tweet.user + "\n"
      }.limit(MAX_TWEETS))
    }

  /**
    * Obtains Future[Seq[Set[Tweet]] from Twitter listener and displays size and hashtags for each tweet
    */
  def hashTagSequence = Action {
    val seq = twitterListener.hashTags

    //DS2
    val seqSource = Source.fromFuture(seq)

    //Flatten Source of Seq[Set[Tweet]] to Source of Set[Tweet]
    val setSource = seqSource.mapConcat(identity)

    Ok.chunked(setSource map {
      se => se.foldLeft("Hashtag count: " + se.size)((acc, hashtag) => acc + " Hashtag: " + hashtag.name.filter(_ >= ' ') ) + "\n"
    })
    }




}
