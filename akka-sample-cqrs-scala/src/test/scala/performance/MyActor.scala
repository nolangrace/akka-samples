package performance

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

class MyActor extends Actor {

  var count = 0

  var names = ""

  val persistentActor:ActorRef = context.actorOf(Props[MyPersistentActor])
  override def receive: Receive = {
    case c: String => {

    }
    case c: WarmUp => {
      sender() ! c
    }
    case c: StopMeasure => {
      sender() ! c
      self ! PoisonPill
    }
    case c: Any => {
      sender() ! c
    }
  }
}
