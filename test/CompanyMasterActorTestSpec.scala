import actors.CompanyMaster
import actors.CompanyMaster.{CompanyMessage, GetCompany, UpdateCompanyData}
import akka.stream.Materializer
import org.scalatestplus.play._
import org.scalatest._

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.Matchers._
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import play.api.Logger
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import reactivemongo.bson.BSONObjectID
import akka.pattern._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User: aloise
  * Date: 16.05.16
  * Time: 16:39
  */
class CompanyMasterActorTestSpec extends TestKit(ActorSystem("testActorSystem")) with ImplicitSender with WordSpecLike with MustMatchers with OptionValues with WsScalaTestClient with BeforeAndAfterAll {

  play.api.Play.start(FakeApplication())

  implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())

  val companyMasterActor =  TestActorRef(Props[actors.CompanyMaster])

  val testCompany = models.Company( BSONObjectID.generate(), "TestCompany" )

  val testCompanyActor = TestActorRef( Props( classOf[actors.Company], testCompany, companyMasterActor ) )


  companyMasterActor ! CompanyMaster.UpdateCompanyData( testCompanyActor, testCompany )


  override def afterAll {
    TestKit.shutdownActorSystem(system)
    Await.result( system.whenTerminated, 10.seconds )
  }

  "CompanyMasterActor" should {

    "not respond on a garbage msg" in {
      companyMasterActor ! ( BigInt(scala.util.Random.nextInt()), "garbage" )
      expectNoMsg()

    }

    "reply on GetCompany request" in {
      companyMasterActor ! CompanyMaster.GetCompany( BSONObjectID.generate() )
      expectMsgType[CompanyMaster.GetCompanyResponse]
    }

    "not receive a reply with empty company" in {
      companyMasterActor ! CompanyMaster.GetCompany( BSONObjectID.generate() )
      expectMsg( CompanyMaster.GetCompanyResponse( None ) )
    }

    "receive a reply with non-empty company" in {
      companyMasterActor ! CompanyMaster.GetCompany( testCompany._id )
      expectMsg( CompanyMaster.GetCompanyResponse( Some( ( testCompanyActor, testCompany ) ) ) )

    }

  }

}
