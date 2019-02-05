package samplecode.akkastream

/*  Why 'akka Stream' ??
*   - working with 'streamed data' is inevitable at the age of 'big data'.
*   - 'actor' is a messaging-based model, not a perfect foundation for streaming the data
*     message can be lost
*     mailbox can be overflown
*   - implements the core value of 'Reactive Streams': back-pressure
*
*
* */
import java.nio.file.Paths

import scala.concurrent._
import scala.concurrent.duration._
import akka.{Done, NotUsed}
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.io.Udp.SO
import akka.stream._
import akka.stream.scaladsl._

object Starter {

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher

  // -------------------------- sample 1 --------------------------
  def printNumbers(): Future[Done] = {
    val source: Source[Int,NotUsed] = Source(1 to 100)
    val ret: Future[Done] = source.runForeach(i => println(i))
    ret
  }

  // -------------------------- sample 2 --------------------------
  def dumpFactorials(): Future[IOResult] = {
    val source = Source(1 to 100)
    val factorials: Source[BigInt, NotUsed] =
      source.scan(BigInt(1))((acc, elem) => acc * elem)

    factorials
      .map( num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("/tmp/factorials.txt")))
  }

  // -------------------------- sample 3 --------------------------
  def tweets(): Future[Done] = {
    tweetSrc
      .map( t => t.hashtags )      // Tweet => Set[Hashtag]
      .reduce( _ ++ _ )
      .mapConcat( identity )       // ???
      .map( htag => htag.name.toUpperCase )
      .runWith( Sink.foreach(println) )
  }

  // -------------------------- sample 4 --------------------------
  def reusedCase(): Future[IOResult] = {
    factorialSource.map( _.toString ).runWith( lineSink("/tmp/factorial2.txt") )
  }

  // -------------------------- sample 5 --------------------------
  def zipStreams(): Future[Done] = {
    factorialSource
      .zipWith( Source(0 to 100) )((fac, num) => s"$num! = $fac")
      .throttle(1, 1.second)       // 1 element/sec
      .runForeach( println )
  }

  // -------------------------- sample 6 --------------------------
  def tweets2() = {
    val authors: Source[Author, NotUsed] =
      tweetSrc
        .filter( t => t.hashtags.contains(Hashtag("#akka")) )
        .map( _.author )

    authors.runWith( printSink() )
  }

  // -------------------------- sample 7 --------------------------
  def tweets3() = {
    tweetSrc
      .mapConcat( t => t.hashtags )         // behind the scene: why not use 'flatMap'
      .runWith( printSink() )
  }

  // -------------------------- sample 8 --------------------------
  def tweets4(): RunnableGraph[NotUsed] = {
    val dumpAuthors: Sink[Author, Future[IOResult]] = Flow[Author]
      .map(a => a.handle)
      .toMat( lineSink("/tmp/authors.txt") )(Keep.right)

    val dumpHtags: Sink[Hashtag, Future[IOResult]] = Flow[Hashtag]
      .map(htag => htag.name)
      .toMat( lineSink("/tmp/hashtags.txt") )(Keep.right)

    RunnableGraph.fromGraph( GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val bcast = b.add( Broadcast[Tweet](2) )
      tweetSrc ~> bcast.in
      bcast.out(0) ~> Flow[Tweet].map(_.author) ~> dumpAuthors
      bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> dumpHtags
      ClosedShape
    } )
  }

  // -------------------------- sample 8 --------------------------
  def tweets5(): RunnableGraph[Future[Int]] =
    tweetSrc
      .map(_ => 1)
      .toMat( Sink.fold[Int, Int](0)(_ + _) )(Keep.right)

  /*
  *   reusable Source / Sink
  * */
  final case class Author(handle: String)
  final case class Hashtag(name: String)
  final case class Tweet(author: Author, ts: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") => Hashtag( t.replaceAll("[^#\\w]", "") )
    }.toSet
  }

  def tweetSrc: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil
  )

  def factorialSource: Source[BigInt, NotUsed] =
    Source(1 to 100).scan(BigInt(1))( _ * _ )

  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    val fileSink: Sink[ByteString, Future[IOResult]] =
      FileIO.toPath( Paths.get(filename) )

    // add adaptor Flow for (String => ByteString)
    val adaptFlow = Flow[String].map( s => ByteString(s + "\n"))

    adaptFlow.toMat( fileSink )(Keep.right)
  }

  def printSink(): Sink[Any, Future[Done]] =
    Sink.foreach( println )

  /*
  * ======================================== main application ========================================
  * */
  def main(args: Array[String]) = {
    println("akka-stream Starter ...")

//    printNumbers().onComplete { case _ => sys.terminate() }
//    dumpFactorials().onComplete( _ => sys.terminate() )
//    tweets().onComplete( _ => sys.terminate() )
//    tweets2.onComplete( _ => sys.terminate() )
//    tweets3.onComplete( _ => sys.terminate() )
//    tweets4().run()
//    tweets5().run().foreach{ c => println(s"incoming tweets counted to $c"); sys.terminate() }
//    reusedCase().onComplete( _ => sys.terminate() )
//    zipStreams().onComplete( _ => sys.terminate() )
  }
}
