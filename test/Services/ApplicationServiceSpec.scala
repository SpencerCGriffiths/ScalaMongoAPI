package Services

import Connectors.ApplicationConnector
import baseSpec._
import cats.data.EitherT
import models.{APIError, DataModel}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.Results.Status

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

  val gameOfThrones: DataModel = DataModel (
    _id = "1",
    name = "A Game of Thrones",
    description = "A description",
    pageCount = 694
  )

  val gameOfThronesJson: JsValue = Json.parse(
    """
      |{
      |  "totalItems": 1,
      |  "items": [
      |    {
      |      "id": "1",
      |      "volumeInfo": {
      |        "title": "A Game of Thrones",
      |        "description": "A description",
      |        "pageCount": 694
      |      }
      |    }
      |  ]
      |}
        """.stripMargin
  )

  "getGoogleBook" should {
    val url: String = "testUrl"

    "return a book" in {
      (mockConnector.get[JsValue](_: String)(_: OFormat[JsValue], _: ExecutionContext))
        //^ sets up a mock expectation or the get method of the mockConnector, The method takes a String (URL PARAM), 0Format, and a Execution context
        .expects(url, *, *)
        //^ specifies arguments for method call - url is specific, * is a wild card
        .returning(EitherT(Future.successful(Right(gameOfThronesJson)): Future[Either[APIError, JsValue]]))
        //^ Sets up the mock return- A Future, containing a Right, with gameOfThrone > Wrapping in Either T indicates it could return an error
        // ^ We explicitly state the type :
        .once()
        // ^ This specifies that the method is called once

      // Transforming EitherT to a Future that only contains the Right value or fails
      val futureResult = testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value.flatMap {
        // ^ call the method on testService with the provided params
        // ^ This returns an EitherT[Future, APIError, DataModel]
        // ^ .value converts to a Future[Either[APIError, DataModel]
        // ^ .flatMap transforms this to a single outcome flattening to Future[Right] or Future[Left] to pattern match
        case Right(value) => Future.successful(value)
          //^ Pattern matching for a successful value or for a failed future due to the exception
      }

      // Using whenReady to wait for the future to complete and verify the result
      whenReady(futureResult) { result =>
        result shouldBe gameOfThrones
      }
      }

    "return an error" in {
      val url: String = "testUrl"

      (mockConnector.get[DataModel](_: String)(_: OFormat[DataModel], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT(Future.successful(Left(APIError.BadAPIResponse(500, "Internal Server Error"))): Future[Either[APIError, DataModel]]))
        // ^ This creates a future that has failed. It gives this error
        .once()

      // Using whenReady to wait for the future to complete and verify the result
      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) {
        case Left(error) => error shouldBe APIError.BadAPIResponse(500, "Internal Server Error")
          // TODO 01/08 14:11 --- Why does this work??
      }
    }

    }
  }