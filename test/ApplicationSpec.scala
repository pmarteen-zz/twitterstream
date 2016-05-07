import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

/**
  *
  * Smoke tests for TwitterListenerController
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "TwitterListenerController" should {

    "display welcome page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
      contentAsString(home) must include ("Hello")
    }

    "display twitter feed" in {
      val home = route(app, FakeRequest(GET, "/twitter")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
    }

    "display hashtags feed" in {
      val home = route(app, FakeRequest(GET, "/hashtags")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
    }

  }

}
