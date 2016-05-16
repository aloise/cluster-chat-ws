import akka.stream.Materializer
import org.scalatestplus.play._
import org.scalatest._

import scala.collection.mutable
import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.Matchers._

/**
  * User: aloise
  * Date: 16.05.16
  * Time: 16:11
  */
class ControllerTestSpec extends PlaySpec with OneAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer

  "Application Controller" must {

    val appController = new controllers.Application()(app.configuration)

    "return default index page" in {

      val result: Future[Result] = appController.index().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText should include ( "Chat App" )
    }

    "return cross origin headers" in {
      val result = appController.crossOriginOptions("/").apply( FakeRequest() )

      header( "Access-Control-Allow-Origin", result ) should not be empty

    }

  }


}
