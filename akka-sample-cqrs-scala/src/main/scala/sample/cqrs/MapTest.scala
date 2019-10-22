package sample.cqrs

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.madhukaraphatak.sizeof.SizeEstimator

object MapTest {
  def main(args: Array[String]): Unit = {

    val system = ActorSystem("iot-system")


    val actor = system.actorOf(Props[MapActor])
    actor ! "bla"
  }
}

import java.util.UUID



class MapActor extends Actor {

  val runtime: Runtime = Runtime.getRuntime
  var testMap = scala.collection.mutable.Map("test" -> UUID.randomUUID().toString)

  override def receive: Receive = {
    case c: String => {

      (1 to 20).map(num => {
//        println(num)
        testMap(num.toString) = UUID.randomUUID().toString
      })

      println("DONE! JVM Used Mem Size - "+(runtime.totalMemory() - runtime.freeMemory())/(1e+9)+" GB")
//      self ! PoisonPill

    }
  }
}