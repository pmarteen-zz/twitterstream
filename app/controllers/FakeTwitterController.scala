package controllers

import java.util.Random
import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.libs.EventSource
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.Tweet

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

/**
  * Controller for generating a stream of fake tweets and performing various operations on them
  */
class FakeTwitterController @Inject()(configuration: play.api.Configuration)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) extends Controller  {
  val MAX_TWEETS = configuration.underlying.getInt("tweets.max")

  /**
    * Standard event feed of fake tweets
    * @return - Action listing fake tweets from Source
    */
  def fakeStream = Action {
    Ok.chunked(fakeTweets().via(Tweet.mapJson) via EventSource.flow)
  }

  /**
    * Given a list of subjects from the given @queryString, creates a single stream combining fake streams about the
    * subjects
    * @param queryString - A list of subjects from which fake tweet streams will be generated from, separated by ','
    * @return - Action listing a source of joined fake tweet streams
    */
  def union(queryString : String) = Action {
    val keywordSources = Source(queryString.split(",").toList)

    val responses: Source[Tweet, NotUsed] = keywordSources.flatMapMerge(5, fakeTweets)
    Ok.chunked(responses.via(Tweet.mapJson) via EventSource.flow)
  }

  /**
    * Given the standard fake tweet stream, filters based on @filterString parameter
    * @param filterString - used to filter based on the existence of the string in the tweet body
    * @return - Action listing a source of the fake tweets after filter is applied
    */
  def filter(filterString : String) = Action {
    val source = fakeTweets()
    val filter = Flow[Tweet].filter{ tweet ⇒
      tweet.body.contains(filterString)
    }
    Ok.chunked(source.via(filter) via Tweet.mapJson via EventSource.flow)
  }

  /**
    * Returns single most retweeted tweet from fake tweet source
    */
  def mostRetweeted = Action {
    val source = fakeTweets()
    val zeroTweet = Tweet("foo","bar",-1)
    val mostFold = Flow[Tweet].fold(zeroTweet){
      (mostTweet, tw) ⇒
        if (tw.retweets > mostTweet.retweets) tw else mostTweet
    }

    Ok.chunked(source.via(mostFold) via Tweet.mapJson)
  }

  /**
    * Sorts fake tweet stream based on retweets
    * @return - Action listing fake tweets in order of retweets, descending
    */
  def descendingByRetweet = Action.async {
    val source = fakeTweets()
    val fList = source.runWith(Sink.seq)

    fList.map(list ⇒ Ok(mergeSort(list.toList).foldLeft("")((acc, tw) ⇒ acc + Json.toJson(tw) + "\n")))
  }
  // Generates source of randomly generated tweets
  private def fakeTweets(keyword : String = "Scala") = {
    Source(1 to MAX_TWEETS).map { _ =>
      val (prefix, author, retweets) = prefixAndAuthor
      Tweet(s"$prefix $keyword", author, retweets)
    }
  }
  //Provides body prefixes, authors, and retweet values for generated tweets
  private def prefixAndAuthor = {
    val prefixes = List("Tweet about", "Just heard about", "I love", "Me gusta", "I can't get enough of")
    val authors = List("Bobby", "Joey", "Johnny", "Sally", "Jimmy")
    val rand = new Random()
    (prefixes(rand.nextInt(prefixes.length)), authors(rand.nextInt(authors.length)), rand.nextInt(100))
  }

  /**
    * Merge sort implementation used to sort tweets based on retweets value
    */
  def mergeSort(input : List[Tweet]) : List[Tweet] = {
    val middle = input.length / 2
    if (middle == 0) input
    else {
      val (left, right) = input.splitAt(middle)
      merge(mergeSort(left), mergeSort(right))
    }
  }

  @tailrec
  private def merge(left : List[Tweet], right : List[Tweet], acc : List[Tweet] = List()) : List[Tweet] = {
    (left, right) match {
      case (List(), _) ⇒ acc ++ right
      case (_, List()) ⇒ acc ++ left
      case (lhead :: ltail, rhead :: rtail) ⇒
        if(lhead.retweets > rhead.retweets) merge(ltail, right, acc :+ lhead)
        else merge(left, rtail, acc :+ rhead)
    }
  }
}
