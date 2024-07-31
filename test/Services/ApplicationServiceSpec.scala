package Services

import Connectors.ApplicationConnector
import baseSpec._
import models.DataModel
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.tools.nsc.interactive.Response

class ApplicationServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
  val mockConnector: ApplicationConnector = mock[ApplicationConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new ApplicationService(mockConnector)

  //^ TODO 31/7 15:04 Information RE- ApplicationConnector and ExecutionContext for test
  //^ We explicitly call methods on the ApplicationConnector class in the ApplicationService. These methods can return different responses based off what you call it with
  //^ We DO NOT want to test our ApplicationConnector as a part of this spec, only ApplicationService

  // TODO 31/7 15:40 Information RE- Mocking / Unit Testing
  //^ where you EXPLICITLY tell the method in ApplicationConnector what to return so you can test how the service responds independently of the connector
  //^ Instead of making a call to the Google Books API we can pretend to have received the gameOfThrones JSON from the API to test the functionality

  val gameOfThrones: JsValue = Json.obj(
    "_id" -> "someId",
    "name" -> "A Game of Thrones",
    "description" -> "The best book!!!",
    "pageCount" -> 100
  )

  "getGoogleBook" should {
    val url: String = "testUrl"

    "return a book" in {
      (mockConnector.get[DataModel](_: String)(_: OFormat[DataModel], _: ExecutionContext))
        .expects(url, *, *)
        // ^ .expects can take. this shows that the connector can expect any request in place of the parameter, this can also be any()
        .returning(Future.successful(gameOfThrones.as[DataModel]))
        //^ returning explicitly states what the connector returns
        .once()
        // ^ .once shows how many times we can expect this response

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "")) {
        //^ When ready handles the future allowing us to wait for the future to complete
        result => result shouldBe gameOfThrones.as[DataModel]
      }
      }
    "return an error" in {
      val url: String = "testUrl"

      (mockConnector.get[DataModel](_: String)(_: OFormat[DataModel], _: ExecutionContext))
        .expects(url, *, *)
        .returning(Future.failed(new Exception("error")))
        // ^ This creates a future that has failed. It gives this error a new Exception
        .once()

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").failed) { failedResult =>
        failedResult shouldBe a[Exception]
        // TODO Information 31/7 20:26- RE a[Exception]
        // ^ Alternatives for other cases could be more specific i.e. a[IllegalArgumentException], a[Throwable]
        failedResult.getMessage shouldBe "error"
      }
    }
    }
  }
