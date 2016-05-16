package controllers.helpers

/**
 * Created by pc3 on 03.08.15.
 */
object HeaderHelpers {

  def crossOriginHeadersAllowOrigin( allowOrigin:Option[String] ) = Seq[(String,String)](
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Origin" -> allowOrigin.getOrElse("*"),
    "Allow" -> "*",
    "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, Cookie, X-Json, X-Prototype-Version",
    "Access-Control-Allow-Credentials" -> "true"

  )

  def crossOriginHeaders()( implicit config:play.api.Configuration ) = {
    crossOriginHeadersAllowOrigin( config.getString("assistants.domain") )

  }

}
