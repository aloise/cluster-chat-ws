package actors

import akka.actor.{ActorRef, Actor}

/**
  * User: aloise
  * Date: 14.05.16
  * Time: 19:10
  */
abstract class BaseChatActor ( companyMaster:ActorRef ) extends Actor {

  val p = getClass.getPackage
  val appName = p.getImplementationTitle
  val appVersion = p.getImplementationVersion


}
