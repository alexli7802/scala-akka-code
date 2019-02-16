package samplecode.akkaactor

import akka.actor.{Actor, ActorLogging, ActorRef}

case object GiveMeThings
case class Give(thing: Any)

trait ProducerBehavior {
  self: Actor =>

  val producerBehavior: Receive = {
    case GiveMeThings => sender() ! Give("thing")
  }
}

trait ConsumerBehavior {
  this: Actor with ActorLogging =>

  val consumerBehavior: Receive = {
    case ref: ActorRef => ref ! GiveMeThings
    case Give(thing) => log.info("Got a thing! It's {}", thing)
  }
}

class Producer extends Actor with ProducerBehavior {
  def receive: Receive = producerBehavior
}

class Consumer extends Actor with ActorLogging with ConsumerBehavior {
  def receive = consumerBehavior
}

class ProducerConsumer extends Actor
                       with ActorLogging
                       with ProducerBehavior
                       with ConsumerBehavior {

  def receive = producerBehavior.orElse(consumerBehavior)
}
