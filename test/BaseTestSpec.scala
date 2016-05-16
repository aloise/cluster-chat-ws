import org.scalatestplus.play._
import org.scalatest._

import scala.collection.mutable

class BaseTestSpec extends PlaySpec {

    "Application" must {
      "work as expected" in {
        1 must be ( 1 )
      }

  }


}