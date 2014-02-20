
import akka.actor._
import akka.routing.RoundRobinRouter
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.math._
  
  sealed trait SMessage
  case object Calculate extends SMessage
  case class Work(start: Long, nrOfElements: Long) extends SMessage
  case class Result(rs: Long) extends SMessage
  case class PerfectSquare(value: Long)
 
  class Worker extends Actor {
    
    def calsum(start: Long,nrofElement:Long):Double = {
      var acc =0.0
      for (i <- start until (start + nrofElement) )
        acc += i*i
      math.sqrt(acc)
    }
    
    def isPerfectNum(x: Double):Boolean=
      if(x == x.toLong ) true else false 
      
    def index(start: Long,nrofElement:Long):Long ={
      if(isPerfectNum(calsum(start,nrofElement))) 
        start
      else
        0
    }
    def receive = {
      case Work(start, nrOfElements) =>
        sender ! Result(index(start, nrOfElements)) // perform the work
    }
  }
 
  class Master(nrOfWorkers: Int, nrOfMessages: Long, nrOfElements: Long, listener: ActorRef)
    extends Actor {
 
    var nrOfResults: Long = _
  //  val start: Long = System.currentTimeMillis
 
    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

 
    def receive = {
      case Calculate =>
        for (i <- 1L to nrOfMessages ) workerRouter ! Work(i , nrOfElements)
      case Result(rs) =>
        nrOfResults += 1
        if (nrOfResults <= nrOfMessages && rs !=0 ) {
          // Send the result to the listener
          listener ! PerfectSquare (rs)
          if(nrOfResults == nrOfMessages)
        	  context.stop(self)
         // Stops this actor and all its supervised children
        }
        if(nrOfResults == nrOfMessages ){
          context.stop(self)
          println("All Results has been shown !")
          context.system.shutdown()
        }
    }
 
  }
  class Listener extends Actor {
    def receive = {
      case PerfectSquare(rs) =>
        println("\n\tStart Index: \t\t%s"
          .format(rs))
        //context.system.shutdown()
    }
  }
 
 

  object Project1 {
    def main(args: Array[String]) {
    val  nrOfElements = args(1).toLong
    
    val nrOfMessages = args(0).toLong
      
     calculate(nrOfWorkers = 20, nrOfElements  , nrOfMessages )
    }
    def calculate(nrOfWorkers: Int, nrOfElements: Long, nrOfMessages: Long) {
    // Create an Akka system
    val system = ActorSystem("SquareSystem")
 
    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")
 
    // create the master
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)),
      name = "master")
 
    // start the calculation
    master ! Calculate
    }
  }
