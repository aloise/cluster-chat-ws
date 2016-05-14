package global

import actors.Company.BusinessCatalystTokenRefreshTick
import actors.CompanyMaster
import actors.CompanyMaster.CompanyMessage
import akka.actor.{ActorRef, Props, ActorSystem}
import com.google.inject.AbstractModule
import javax.inject._
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsNull, Json}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._

/**
  * User: aloise
  * Date: 13.05.16
  * Time: 23:20
  */

trait ApplicationLifecycleMonitor {

  def companyMasterActorSystem:ActorSystem

  def companyMasterActor:ActorRef

  def onStartup(): Unit

}

@Singleton
class ApplicationLifecycleMonitorImpl @Inject() (appLifecycle: ApplicationLifecycle, configuration: play.api.Configuration) extends ApplicationLifecycleMonitor {

  import models.Companies.{ jsonFormat => companiesJsonFormat }

  lazy val companyMasterActorSystem = ActorSystem( configuration.getString("chat.actorSystemName").getOrElse("default") )

  lazy val companyMasterActor = companyMasterActorSystem.actorOf( Props( classOf[CompanyMaster] ) )


  override def onStartup(): Unit = {

    Logger.debug( "ApplicationLifecycleMonitorImpl started" )

    models.ChatRooms.closeOutdatedChatRooms()

    refreshOAuthTokens( companyMasterActor )( companyMasterActorSystem.dispatcher )
  }

  protected def refreshOAuthTokens( companyMaster:ActorRef )( implicit ec:ExecutionContext ) = {

    val q = Json.obj( "businessCatalystOAuthResponse.refresh_token" -> Json.obj( "$ne" -> JsNull ) )

    models.Companies.collection.find(q).cursor[models.Company]().collect[Seq]().foreach { companies =>

      companies.foreach { company =>
        companyMaster ! CompanyMessage( company._id, BusinessCatalystTokenRefreshTick )
      }

    }

  }


  // You can do this, or just explicitly call `hello()` at the end
  def start(): Unit = {
    onStartup()
  }

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>

    Future.successful(())
  }

  // Called when this singleton is constructed (could be replaced by `hello()`)
  start()
}


class StartupModule extends AbstractModule {

  override def configure() = {
    // We bind the implementation to the interface (trait) as an eager singleton,
    // which means it is bound immediately when the application starts.
    bind(classOf[ApplicationLifecycleMonitor]).to(classOf[ApplicationLifecycleMonitorImpl]).asEagerSingleton()
  }

}
