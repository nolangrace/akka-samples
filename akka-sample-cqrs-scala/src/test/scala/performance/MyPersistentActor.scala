package performance

import java.util.UUID

import akka.actor.PoisonPill
import akka.persistence.PersistentActor

case class WarmUp()
case class PersistAsyncMessage(message:String)
case class PersistMessage(message:String)
case class PersistBatchMessage(message:String, batchNum:Int)



class MyPersistentActor extends PersistentActor {

  override def persistenceId = "PersistentActor-"+UUID.randomUUID().toString

  override def receiveRecover: Receive = {
    case _ => // handle recovery here
  }

  var count = 0

  var names = ""

  override def receiveCommand: Receive = {
    case PersistMessage(m) => {
      persist(s"evt-$m") { e =>

      }
    }
    case PersistAsyncMessage(m) => {
      persistAsync(s"evt-$m") { e =>
      }
    }
    case PersistBatchMessage(m, batchNum) => {
      count+=1
      names += s"evt-$m-$count,"

      if(count%batchNum == 0) {
        persist(names) { e =>
          names = ""
        }
      }
    }
    case c: WarmUp => {
      sender() ! c
    }
    case c: StopMeasure => {
      sender() ! c
//      self ! PoisonPill
    }
    case c: Any => {
        sender() ! c
    }
  }

  override protected def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit =
    event match {
//      case Evt(data) => sender() ! s"Rejected: $data"
      case _         => super.onPersistRejected(cause, event, seqNr)
    }

  override protected def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit =
    event match {
//      case Evt(data) => sender() ! s"Failure: $data"
      case _         => super.onPersistFailure(cause, event, seqNr)
    }
}
