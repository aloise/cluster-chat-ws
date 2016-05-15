package controllers

import java.net.URLDecoder
import javax.inject.Inject

import actors.{UserConnection, AssistantConnection}
import actors.messages.SocksMessages.{AssistantRequest, Message}
import akka.stream.Materializer
import global.ApplicationLifecycleMonitor
import global.crypto.{CryptoConfigParser, Crypto}
import play.api.libs.json.{JsString, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{RequestHeader, Controller}
import play.sockjs.api._
import reactivemongo.play.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection._
import scala.concurrent.Future
import scala.util.Try

import actors.messages.SocksMessages._
import AssistantConnection.assistantRequestMessageFormatter
import models.Widgets.{ jsonFormat => widgetJsonFormat }

/**
 * User: aloise
 * Date: 02.11.14
 * Time: 9:42
 */
class AssistantGateway @Inject() ( app: ApplicationLifecycleMonitor, mat:Materializer ) extends SockJSRouter with BaseController  {

  override protected def settings = SockJSSettings()



  def sockjs = SockJS.accept [AssistantRequest, Message] { request =>

    // connect the websocket. All processing and error reporting is done inside the UserConnection actor
    ActorFlow.actorRef[AssistantRequest, Message]( AssistantConnection.getActorProps( request, app.companyMasterActor, app.getCryptoProvider ) )( app.companyMasterActorSystem, mat )

  }


  protected def getRequestCookies( request:RequestHeader ) = {
    request.cookies.map{ c =>
      val str = URLDecoder.decode( c.value, "UTF-8")

      c.name -> str // Try( Json.parse( str ) ).getOrElse( JsString(str) )

    }.toMap
  }



}
