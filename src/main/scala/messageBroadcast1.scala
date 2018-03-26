import java.util.UUID

import akka.actor._

import scala.collection.mutable
import akka.actor.{Actor, ActorRef}

case class Subscribe(channel: Channel, actor: ActorRef)
case class Publish(channel: Channel, payload: Any, retain: Boolean = false)

case class Channel(id: UUID = UUID.randomUUID(), name: String, phoneNumber: Option[String])
/*case class User(id: UUID, name: String)
case class Following(channelId: UUID, UserId: UUID)
case class PhoneNumber(number: String)*/

class ChannelActor extends Actor {
  val channelsList = mutable.Map.empty[Channel, ActorRef]

  def createBroadcastActor = {
    context.actorOf(Props[BroadcastActor])
  }

  override def receive = {
    case subscribe: Subscribe => channelsList.getOrElseUpdate(subscribe.channel, createBroadcastActor) ! subscribe

    case publish: Publish => {
      val channelsGroup = channelsList.keySet.toList.groupBy(_.phoneNumber)
      val samePhoneNumbers: List[Channel] = channelsGroup.toList.filter(_._2.size >= 2).flatMap(x => x._2) //having same phone number

      samePhoneNumbers.map { channel =>
        val newChannel = channel.copy(phoneNumber = Some("12345")) //any random number .. add logic here for different numbers
        val newChannelBroadcast =  channelsList.get(channel).get
        channelsList.getOrElseUpdate(newChannel, newChannelBroadcast)
        //channelsList.remove(channel)
      }
      //channelsList.map(c => println(s"Channels: ${c._1}"))
      channelsList.get(publish.channel).foreach(_ ! publish)
    }
  }
}

class BroadcastActor extends Actor {
  val recipients = mutable.MutableList.empty[ActorRef]

  override def receive = {
    case Subscribe(channel: Channel, actorRef: ActorRef) =>{
      recipients += actorRef
    }

    case publish : Publish =>{
      recipients.map(_ ! publish)
    }
  }
}

class Subscriber extends Actor {
  override def receive: Receive = {
    case publish: Publish => println(s"I am ${self.path.name}. Received message as ${publish.payload} from ${publish.channel.name}.")
  }
}

object TestApp extends App {
  val system = ActorSystem("Broadcasting")
  val actor = system.actorOf(Props[ChannelActor], "Broadcasting-System")

  val subscriber1 = system.actorOf(Props[Subscriber], name = "subscriber-1")
  val subscriber2 = system.actorOf(Props[Subscriber], name = "subscriber-2")
  val subscriber3 = system.actorOf(Props[Subscriber], name = "subscriber-3")

  val channel = Channel(name="NEWS SMS", phoneNumber = Some("9988776655"))
  val channel1 = Channel(name="CRICKET NEWS SMS", phoneNumber = Some("32321"))
  val channel2 = Channel(name="WEATHER SMS", phoneNumber = Some("9988776655"))

  actor ! Subscribe(channel, subscriber1)
  actor ! Subscribe(channel, subscriber2)
  actor ! Subscribe(channel1, subscriber2)
  actor ! Subscribe(channel2, subscriber1)
  actor ! Subscribe(channel2, subscriber2)
  actor ! Subscribe(channel2, subscriber3)

  actor ! Publish(channel, "Hey How r u?")
  actor ! Publish(channel1, "Good Morning!!")
  actor ! Publish(channel2, "Weather is good today")
}