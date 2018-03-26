import java.util.UUID

import akka.actor._
import akka.event.{EventBus, LookupClassification}
import akka.event.ActorEventBus

case class Channel1(id: UUID = UUID.randomUUID(), name: String, phoneNumber: Option[String])

class LookupBus(phoneNumber: String) extends EventBus
  with LookupClassification
  with ActorEventBus {

  type Event = Channel1
  type Classifier = Boolean

  override def mapSize = 128

  override def classify(channel: Channel1) = channel.phoneNumber.contains(phoneNumber)

  override protected def publish(channel: Channel1, subscriber: Subscriber)= subscriber ! channel
}

class BroadcastChannel(bus: LookupBus) extends Actor {
  def receive = {
    case channel: Channel1 => {
      println(s"${channel} Broadcasting a new message from ${channel.phoneNumber}")
      bus.publish(channel)
    }
  }
}

class ChannelSubscriber extends Actor {
  def receive = {
    case channel: Channel1 => println(s"I am ${self.path.name} and I have received a message: $channel")
  }
}

object Main2 extends App {

  implicit val system = ActorSystem("evernymMessageBroadcast")

  val newsChannel = Channel1(name="Mobile News Channel", phoneNumber = Some("2341234234"))
  val cricketchannel = Channel1(name="Mobile Cricket Channel", phoneNumber = Some("9988664455"))

  val lookupBus = new LookupBus(newsChannel.phoneNumber.get)
  val lookupBus1 = new LookupBus(cricketchannel.phoneNumber.get)

  val broadcastChannel = system.actorOf(Props(new BroadcastChannel(lookupBus)), name = "newsMessage-Broadcast")
  val cricketBroadcastChannel = system.actorOf(Props(new BroadcastChannel(lookupBus1)), name = "CricketMessage-Broadcast")

  val subscriber1 = system.actorOf(Props[ChannelSubscriber], name = "subscriber-1")
  val subscriber2 = system.actorOf(Props[ChannelSubscriber], name = "subscriber-2")

  lookupBus.subscribe(subscriber1, false)  // false if phonenumber is not same
  //lookupBus.subscribe(subscriber2, false)
  lookupBus1.subscribe(subscriber1, false)
  //lookupBus1.subscribe(subscriber2, false)

  broadcastChannel ! newsChannel
  broadcastChannel ! cricketchannel
  cricketBroadcastChannel ! newsChannel
  cricketBroadcastChannel ! cricketchannel
}