package samplecode.akkaactor

/* A quick example of UserActor
*  - why 'receive' returns 'Unit' only ?
*  - where to create the actor ?
*     . system.actorOf(...) for top-level actors
*     . context.actorOf(...) for child actors
*  - why only 'ActorRef' returns from actor creation ?
*  - What does the lifecycle look like ?
*    created -> .preStart()/.postRestart() -> message-processing -> .postStop()/.preRestart() ->
*  - What's difference between 'restart' and 're-create' ?
*    . restart : triggered by supervision strategy, not change 'ActorRef', incarnation and UID,
*                not observable to watching actors
*    . recreate: 'Terminated' to watching actors, 'ActorRef'/incarnation/UID will change,
*                and actor path is cleaned and reused.
*  - What's the differences between 'ActorRef' and 'ActorSelection' ?
*    . ActorRef      : pointing to actor instance
*    . ActorSelection: pointing to actor path/s (may be empty), can be resolved to ActorRef, using
*                      'Identify' message
*  - Where to initialize actor ?
*    . constructor: fresh 'create', every 'restart'
*    . preRestart : fresh 'create', and called by postRestart (skip-able)
*  - What will cause an actor to re-start ?
*    . failure in processing a message
*    . supervisor/parent is restarted
*    . sibling's failure
*  - Who is the sender() ?
*    . will be 'deadLetters', if not within an actor
*  - When to use a mediator actor to forward messages?
*    . routers
*    . load-balancers
*    . replicators
*  - How does the actor terminate ?
*    suspend mailbox -> 'stop' to children -> process 'Terminated' from children -> postStop() ->
*      dump mailbox -> publish 'Terminated'
*  - When is the best time to recreate an actor
*    In response to its 'Terminated' message, which ensure the name and path are freed
*  - What's the difference in between 'stop', 'PoisonPill' and 'Kill' ?
*    stop      : graceful way
*    PoisonPill: ??
*    Kill      : throw 'ActorKilledException' and up to the supervisor to stop/restart/resume
*
*
*
*
*  Key Notes:
*  - Declaring one actor within another is DANGEROUS!! (never pass 'this' into Props)
*  - Actor starts asynchronously when created
*  - path is empty before actor is created. UID is to universally uniquely identify
*    an actor instance/incarnation
*  - Watching an already terminated actor will trigger 'Terminated' message immediately. 'unwatch'
*    ignores 'Terminated' messages.
*  - Actor has a logical path, and a physical path
*  - 'ask' pattern
*    1. there's performance penalty, don't use it for 'too frequent' request messages
*    2. 'ask' + 'pipeTo' is a common combo
*    3. within actor's context, DON'T call actor's method in 'Future.onComplete(...)"
*  - Stop an actor (best practice: use protocol-level message to stop your actor)
*    1. via ActorContext: context.stop( child/self )
*    2. via ActorSystem :
*  - stop an actor is asynchronous, so if you want to stop multi-actor in a certain order, you need
*    'akka.pattern.gracefulStop'
* */

import akka.actor.{Actor, ActorSystem, Props, ReceiveTimeout}
import akka.event.Logging
import akka.util.Timeout
import scala.util.{Success,Failure}
import scala.concurrent.Future
import scala.concurrent.duration._

object MyActor {
  // best practice: create 'Props' for the actor
  def props(num: Int): Props = Props( new MyActor(num) )

  // best practice: messages to respond are defined in companion object
  case class Greeting(from: String)
  case object Goodbye

  case class SimpleReq(req: String, ms: Int)
}

class MyActor(num: Int) extends Actor {
  import MyActor._

  val log = Logging(context.system, this)

  // optional: enable receive timeout
  context.setReceiveTimeout(3 seconds)

  // best practice: clean up the old instance, and hand over to the fresh one
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    super.preRestart(reason, message)

  // 'receive' MUST be exhaustive to all cases, otherwise 'UnhandledMessage' will be
  // added to 'EventStream'.
  override def receive: Receive = {
    case Greeting(from) => log.info(s"received message: Greeting($from)" )
    case Goodbye        => log.info( "received message: Goodbye")
    case SimpleReq(req, ms) => {
      Thread.sleep(ms)
      sender() ! s"answer: [$req]"
    }
    case ReceiveTimeout => log.info("no messages in last 3 seconds ~~~")
    case _              => log.info( "undefined message!!")
  }
}



object MyActorApp {

  import akka.pattern.{ask,AskTimeoutException}

  // best practice: ActorSystem should be initiated only once per application
  val sys = ActorSystem()
  implicit val ec = sys.dispatcher

  // best practice: use ActorSystem to create top-level actors
  val myActor = sys.actorOf(MyActor.props(6))

  def test_tellActor(): Unit = {
    myActor ! MyActor.Greeting("hello")
    myActor ! MyActor.Goodbye
  }

  def test_askActor() = {
    val ans: Future[String] = myActor.ask(MyActor.SimpleReq("how are you?", 1000))(1 seconds)
      .mapTo[String]

    ans.onComplete {
      case Success(aws) => println("answer from Actor: " + aws)
      case Failure(_: AskTimeoutException) => println("ask time out !")
    }
  }


  def main(args: Array[String]): Unit = {
    println("------------------ akkaactor.MyActorApp starts ------------------")



  }
}