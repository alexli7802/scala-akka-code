package samplecode.akkaactor

import akka.actor.{Actor}

class HotSwapActor extends Actor {

  // message-loop: in 'angry'
  def angry: Receive = {
    case "foo" => sender() ! "I am already angry?"
    case "bar" => context.become(happy)
  }

  // message-loop: in 'happy'
  def happy: Receive = {
    case "bar" => sender() ! "I am already happy :-)"
    case "foo" => context.become(angry)
  }

  // message-loop: initial
  override def receive: Receive = {
    case "foo" => context.become(angry)
    case "bar" => context.become(happy)
  }
}
