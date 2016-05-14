package global

import actors.Company.BusinessCatalystTokenRefreshTick
import actors.CompanyMaster
import actors.CompanyMaster.CompanyMessage
import akka.actor.{ActorRef, Props, ActorSystem}
import com.google.inject.{ImplementedBy, AbstractModule}
import javax.inject._
import play.api.inject.{Binding, Module, ApplicationLifecycle}
import play.api.libs.json.{JsNull, Json}
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoApi}
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._

import scala.concurrent.ExecutionContext

/**
  * User: aloise
  * Date: 13.05.16
  * Time: 23:20
  */


@ImplementedBy(classOf[ApplicationLifecycleMonitorImpl])
trait ApplicationLifecycleMonitor {

  def companyMasterActorSystem:ActorSystem

  def companyMasterActor:ActorRef

  def onStartup(): Unit

}

@Singleton
class ApplicationLifecycleMonitorImpl @Inject() ( implicit ec:ExecutionContext, configuration:Configuration ) extends ApplicationLifecycleMonitor {

  import models.Companies.{ jsonFormat => companiesJsonFormat }


  lazy val companyMasterActorSystem =  ActorSystem( configuration.getString("chat.actorSystemName").getOrElse("default") )

  lazy val companyMasterActor = companyMasterActorSystem.actorOf( Props( classOf[CompanyMaster] ) )


  override def onStartup(): Unit = {

    Logger.debug( "ApplicationLifecycleMonitorImpl started " + this )

  }


  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
/*
  appLifecycle.addStopHook { () =>

    Future.successful(())
  }
*/

  // Called when this singleton is constructed (could be replaced by `hello()`)
  onStartup()
}
