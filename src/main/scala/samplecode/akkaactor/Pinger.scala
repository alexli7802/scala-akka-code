package samplecode.akkaactor

import akka.actor.{ ActorSystem, Actor, ActorRef, Props, PoisonPill }
import language.postfixOps
import scala.concurrent.duration._

case object Ping
case object Pong

class Pinger extends Actor {

  var countDown = 100

  override def receive: Receive = {
    case Pong => {
      // respond to 'Pong'
      println(s"${self.path} received pong, count down $countDown")
      if (countDown > 0) {
        countDown -= 1
        sender() ! Ping      // keep interacting (countDown >= 1)
      } else {
        sender() ! PoisonPill    // terminate the peer actor
        self ! PoisonPill        // terminate itself
      }
    }
  }
}

class Ponger(pinger: ActorRef) extends Actor {
  override def receive: Receive = {
    case Ping => {
      // respond to 'Ping'
      println(s"${self.path} received ping")
      pinger ! Pong
    }
  }
}

object PingPong {

  val system = ActorSystem("pingpong")
  val pinger = system.actorOf(Props[Pinger], "pinger")
  val ponger = system.actorOf(Props(classOf[Ponger], pinger), "ponger")

  implicit val disp = system.dispatcher

  def main(args: Array[String]): Unit = {

    println("-------------- akkaactor.PingPong starts --------------")

    system.scheduler.scheduleOnce(500 millis) { ponger ! Ping }
  }
}