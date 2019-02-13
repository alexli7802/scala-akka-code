package samplecode.akkaactor

import scala.concurrent.duration._
import akka.actor.{Actor,Timers,ActorSystem,Props}

object TActor {
  private case object TickKey
  private case object FirstTick
  private case object Tick
}

class TActor extends Actor with Timers {
  import TActor._

  //optional: 'FirstTick' message will be sent to 'self', after 500ms
  //          'TickKey' is ID for the timer
  timers.startSingleTimer(TickKey, FirstTick, 500.millis)

  override def receive: Receive = {
    case FirstTick => {
      println("FirstTick !")
      timers.startPeriodicTimer(TickKey, Tick, 3 seconds)
    }
    case Tick => println("Tick ~~~~")
  }
}

object ActorWithTimer {

  val system = ActorSystem()

  def main(args: Array[String]): Unit = {
    println("-------------- akkaactor.ActorWithTimer --------------")
    val ator = system.actorOf(Props[TActor])


  }
}
