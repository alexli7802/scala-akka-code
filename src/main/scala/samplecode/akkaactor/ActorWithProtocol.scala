package samplecode.akkaactor

import akka.actor.{Actor,Stash}

class ActorWithProtocol extends Actor with Stash {

  override def receive: Receive = {
    case "open" => {
      unstashAll()
      context.become( {
        case "write" => // nested writing behavior
        case "close" =>
          unstashAll()
          context.unbecome()
        case msg => stash()
      }, discardOld = false )

    }
    case msg => stash()
  }
}
