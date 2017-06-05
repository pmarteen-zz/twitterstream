package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import play.api.libs.EventSource
import play.api.libs.ws.WSClient
import play.api.mvc._
import services._

import scala.concurrent.ExecutionContext


/**
  * Controller interacting with twitter listener
  *
  * Author: peter.marteen
  */
class TwitterListenerController @Inject()(configuration: play.api.Configuration)(implicit system: ActorSystem, materializer: Materializer,
                                                                                 wsClient : WSClient, ec: ExecutionContext) extends Controller {

  val MAX_TWEETS = configuration.underlying.getInt("tweets.max")
  val twitterListener = new MyTwitterListener

  /**
    * Displays tweets from TwitterListener tweet stream
    */
  def stream = Action {
    val twitSource = twitterListener.listen
    Ok.chunked(twitSource.take(MAX_TWEETS).via(Tweet.mapJson) via EventSource.flow)
  }

  /**
    * Filters tweets from sample based on a given string
    */
  def filter(filterString : String) = Action {
    val source = twitterListener.listen
    val filter = Flow[Tweet].filter{ tweet ⇒
      tweet.body.contains(filterString)
    }
    Ok.chunked(source.via(filter).take(MAX_TWEETS) via Tweet.mapJson via EventSource.flow)
  }
  /**
    * Displays hashtags from tweet stream
    */
  def hashtags = Action {
    val hashtagSource = twitterListener.hashtagSource

    Ok.chunked(hashtagSource.take(MAX_TWEETS).map(set ⇒ set.mkString) via EventSource.flow)
  }

}
