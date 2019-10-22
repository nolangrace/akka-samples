package performance

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

case class StopMeasure()

class PersistencePerformance()
  extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val loadCycles = system.settings.config.getInt("performance.cycles.load")



  class Measure(numberOfMessages: Int) {
    private val NanoToSecond = 1000.0 * 1000 * 1000

    private var startTime: Long = 0L
    private var stopTime: Long = 0L

    def startMeasure(): Unit = {
      startTime = System.nanoTime
    }

    def stopMeasure(): Double = {
      stopTime = System.nanoTime
      (NanoToSecond * numberOfMessages / (stopTime - startTime))
    }
  }

  implicit val timeout = Timeout(100 seconds)

  def stressActor(actor: ActorRef, description: String, persistType:String): Unit = {
    val m = new Measure(loadCycles)

    if(persistType!="none") {
      val future = actor ? WarmUp

      Await.result(future, timeout.duration)
    }

    m.startMeasure()
    (1 to loadCycles).foreach { i =>
      if(persistType.equals("async")){
        actor ! PersistAsyncMessage(s"msg${i}")
      }else if(persistType.equals("batch")){
        actor ! PersistBatchMessage(s"msg${i}", 50)
      }else if(persistType.equals("normal")){
        actor ! PersistMessage(s"msg${i}")
      }


    }
    actor ! StopMeasure
    expectMsg(500.seconds, StopMeasure)
    println(f"\nthroughput = ${m.stopMeasure()}%.2f $persistType $description per second")


  }

  def stressEventSourcedPersistentActor(failAt: Option[Long], persistType:String, numberOfActors:Int): Unit = {
    (1 to numberOfActors).par.map(_ =>
      system.actorOf(Props[MyPersistentActor])
    ).map(actor => stressActor(actor, "persistent events", persistType))
  }

  def stressNormalActor(failAt: Option[Long]): Unit = {
    val normalActor:ActorRef = system.actorOf(Props[MyActor])
    stressActor(normalActor, "normal messages", "none")
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }

  }

  "Warmup normal actor" should {
    "exercise" in {
      stressNormalActor(None)
    }
    "exercise some more" in {
      stressNormalActor(None)
    }
  }

  "A normal actor" should {
    "have some reasonable throughput" in {
      stressNormalActor(None)
    }
  }

  "A Normal event sourced persistent actor" should {
    "have some reasonable throughput" in {
      stressEventSourcedPersistentActor(None, "normal", 1)
    }
  }

  "An Async event sourced persistent actor" should {
    "have some reasonable throughput" in {
      stressEventSourcedPersistentActor(None, "async", 1)
    }
  }

  "An Batch event sourced persistent actor" should {
    "have some reasonable throughput" in {
      stressEventSourcedPersistentActor(None, "batch", 1)
    }
  }


}

//import scala.concurrent.duration._
//
//import com.typesafe.config.ConfigFactory
//
//import akka.actor._
//import akka.testkit._
//
//object PersistancePreformanceSpec {
//  val config =
//    """
//      akka.persistence.performance.cycles.load = 100
//      # more accurate throughput measurements
//      #akka.persistence.performance.cycles.load = 200000
//    """
//
//  case object StopMeasure
//  final case class FailAt(sequenceNr: Long)
//
//  class Measure(numberOfMessages: Int) {
//    private val NanoToSecond = 1000.0 * 1000 * 1000
//
//    private var startTime: Long = 0L
//    private var stopTime: Long = 0L
//
//    def startMeasure(): Unit = {
//      startTime = System.nanoTime
//    }
//
//    def stopMeasure(): Double = {
//      stopTime = System.nanoTime
//      (NanoToSecond * numberOfMessages / (stopTime - startTime))
//    }
//  }
//
//}
//
//class PersistancePreformance with ImplicitSender {
//  import PerformanceSpec._
//
//  val loadCycles = system.settings.config.getInt("akka.persistence.performance.cycles.load")
//
//  def stressPersistentActor(persistentActor: ActorRef, failAt: Option[Long], description: String): Unit = {
//    failAt.foreach { persistentActor ! FailAt(_) }
//    val m = new Measure(loadCycles)
//    m.startMeasure()
//    (1 to loadCycles).foreach { i =>
//      persistentActor ! s"msg${i}"
//    }
//    persistentActor ! StopMeasure
//    expectMsg(100.seconds, StopMeasure)
//    println(f"\nthroughput = ${m.stopMeasure()}%.2f $description per second")
//  }
//
//  def stressEventSourcedPersistentActor(failAt: Option[Long]): Unit = {
//    val persistentActor = namedPersistentActor[EventsourcedTestPersistentActor]
//    stressPersistentActor(persistentActor, failAt, "persistent events")
//  }
//
//  "Warmup persistent actor" should {
//    "exercise" in {
//      stressEventSourcedPersistentActor(None)
//    }
//    "exercise some more" in {
//      stressEventSourcedPersistentActor(None)
//    }
//  }
//
//  "An event sourced persistent actor" should {
//    "have some reasonable throughput" in {
//      stressEventSourcedPersistentActor(None)
//    }
//    "have some reasonable throughput under failure conditions" in {
//      stressEventSourcedPersistentActor(Some(loadCycles / 10))
//    }
//  }
//
//}
