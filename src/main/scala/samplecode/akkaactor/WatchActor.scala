package samplecode.akkaactor

import akka.actor.{Actor, Props, Terminated, ActorSystem }

class WatchActor extends Actor {

  // create a child actor upon its own creation'
  val child = context.actorOf(Props.empty, "child")

  // watch the child for 'Terminated'. (Parent won't become the watcher by default)
  context.watch( child )


  var lastSender = context.system.deadLetters

  override def preStart(): Unit = {
    println("WatchActor is online now...")
  }

  override def receive: Receive = {
    case "kill" =>
      // respond to 'kill' message, (who could that be ?)
      context.stop( child ); lastSender = sender()
    case Terminated(`child`) =>
      // respond to child's 'Terminated'
      println("child is gone~~~~")
      lastSender ! "finished"
  }
}

object WatchActorApp {
  def main(args: Array[String]): Unit = {
    println("----------- akkaactor.WatchActorApp starts -------------")
    val system = ActorSystem()
    val w = system.actorOf(Props[WatchActor])
    w ! "kill"
  }
}
