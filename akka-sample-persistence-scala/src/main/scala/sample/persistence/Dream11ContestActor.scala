package sample.persistence

//#persistent-actor-example
import akka.actor._
import akka.persistence._

case class JoinContestCmd(user: String)

case class JoinContestMsg(id:Long, data: String)

sealed trait ContestEvt
case class JoinRequestSent(data: String) extends ContestEvt
case class JoinSuccess(id: Long, data: String) extends ContestEvt
case class JoinFailure(id: Long, data: String) extends ContestEvt

class Dream11ContestActor extends PersistentActor with AtLeastOnceDelivery with akka.actor.ActorLogging {
  override def persistenceId = "sample-id-1"

  val voltDBActor:ActorRef = context.actorOf(Props[VoltDBDeliveryActor], "VoltDBActor")

  override def receiveRecover: Receive = {
    case evt: ContestEvt => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case JoinContestCmd(user) => {
      log.info("Recieved Join Request: "+user)
      persist(JoinRequestSent(user))(updateState)
    }

    //On Either of the Persists below we could add a tag that would be used by CQRS Actors
    case JoinSuccess(id, data) => persist(JoinSuccess(id, data))(updateState)
    case JoinFailure(id, data) => persist(JoinFailure(id, data))(updateState)
  }

  def updateState(evt: ContestEvt): Unit = evt match {
    case JoinRequestSent(data) =>
      deliver(voltDBActor.path)(deliveryId => JoinContestMsg(deliveryId, data))

    case JoinSuccess(id, user) => {
      log.info("Contest Join Success Confirmed: "+user)
      confirmDelivery(id)
    }

    case JoinFailure(id, user) => {
      log.info("Contest Join Fail Confirmed: "+user)
      confirmDelivery(id)
    }
  }

}
//#persistent-actor-example

class VoltDBDeliveryActor extends Actor with akka.actor.ActorLogging {
  def receive = {
    case JoinContestMsg(deliveryId, user) => {
      // Write to VOLTDB and send response back to
      log.info("VoltDB Actor Received Join Request: "+user)
      sender() ! JoinSuccess(deliveryId, user)
    }
  }
}


object Dream11PersistentActorExample extends App {

  val system = ActorSystem("example")
  val persistentActor = system.actorOf(Props[Dream11ContestActor], "persistentActor-4-scala")

  persistentActor ! JoinContestCmd("foo")
  persistentActor ! JoinContestCmd("baz")
  persistentActor ! JoinContestCmd("bar")
  persistentActor ! JoinContestCmd("buzz")

  Thread.sleep(10000)
  system.terminate()
}
