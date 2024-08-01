package controllers

import Services.ApplicationService
import baseSpec.BaseSpecWithApplication
import models.DataModel
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Accepted
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication{

  val TestApplicationController = new ApplicationController(
    controllerComponents = component,
    dataRepository = repository,
    service = service
  )

  private val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

  private val dataModelUpdate: DataModel = DataModel(
    "abcd",
    "test name2",
    "test description2",
    1002
  )
  "ApplicationController .index()" should {


    "return 200 OK when books are found" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      // Testing the index() route of ApplicationController
      // FakeRequest() is needed to imitate an inbound HTTP request
      val result = TestApplicationController.index()(FakeRequest())

      status(result) shouldBe OK
      afterEach()
    }

    "return the Seq of books when books are found" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      // Testing the index() route of ApplicationController
      // FakeRequest() is needed to imitate an inbound HTTP request
      val result = TestApplicationController.index()(FakeRequest())

      whenReady(result) { res =>
        res.body.contentType shouldBe Some("application/json")
        // Manually convert ByteString to JSON
        val byteString = res.body.consumeData.futureValue
        val jsonString = byteString.utf8String
        val json = Json.parse(jsonString)
        val books = json.as[Seq[DataModel]]

        // Print or assert the parsed books
        books shouldBe Seq(dataModel)
      }
      afterEach()
    }

    "return 204 no content found with a valid request but no content" in {
      beforeEach()

      // Testing the index() route of ApplicationController
      // FakeRequest() is needed to imitate an inbound HTTP request
      val result = TestApplicationController.index()(FakeRequest())

      status(result) shouldBe NO_CONTENT
      afterEach()
    }

    "return None when no books are present" in {
      beforeEach()

      // Testing the index() route of ApplicationController
      // FakeRequest() is needed to imitate an inbound HTTP request
      val result = TestApplicationController.index()(FakeRequest())

      whenReady(result) { res =>
        res.body.contentType shouldBe None
      }
      afterEach()
    }

    "return 400 when no invalid request has been made on this path" in {
      beforeEach()
      // to cause an error in the index() you have to simulate a scenario where an exception is thrown
      // This would an error with the database but in order not to replicate this consistently this can be "mocked"


      val request: FakeRequest[JsValue] = buildGet("/api").withBody[JsValue](Json.toJson("Bad Request- Invalid JSON Data Model"))
      val result = TestApplicationController.index()(request)

      whenReady(result) { res =>
        res.body.contentType shouldBe None
      }
      afterEach()
    }
  }

  "ApplicationController .create" should {

    "create a book in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED
      afterEach()
    }

    "return a bad request if body to create cannot be validated" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson("Bad Request- Invalid JSON Data Model"))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.BAD_REQUEST
      afterEach()
    }
  }

  "ApplicationController .read" should {

    "find a book in the database by id" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())

      status(readResult) shouldBe OK
      contentAsJson(readResult).as[DataModel] shouldBe dataModel
      afterEach()
    }

    "return a not found 404 if the book does not exist" in {
      beforeEach()
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      status(readResult) shouldBe NOT_FOUND
      afterEach()
    }
  }

  "ApplicationController .update()" should {

    "Update a database entry when the entry exists" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED


      val requestUpdate: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdate))
      val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


      status(updateResult) shouldBe ACCEPTED
      afterEach()
    }

    "Return an error of NotFound when the item does not exist to be updated" in {
      beforeEach()
      // Entry with ID abcd has not been created yet to update
      val requestUpdate: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdate))
      val updateResult: Future[Result] = TestApplicationController.update("1")(requestUpdate)


      status(updateResult) shouldBe NOT_FOUND
      afterEach()
    }

    "Return an error of BadRequest when the data to update is not valid Json" in {
      beforeEach()
      // Entry with ID abcd has been created but the data being sent to update isn't appropriate

      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val requestUpdate: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson("This is not valid Json for dataModel"))
      val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


      status(updateResult) shouldBe BAD_REQUEST
      afterEach()
    }
  }


  "ApplicationController .delete()" should {

    "delete an individual entry in the data base with 202 Accepted response" in {

      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val updateResult: Future[Result] = TestApplicationController.delete("abcd")(FakeRequest())


      status(updateResult) shouldBe ACCEPTED
      afterEach()
    }

    "Return a 404 not found if the ID to delete does not exist in the database" in {

      beforeEach()
      val updateResult: Future[Result] = TestApplicationController.delete("abcd")(FakeRequest())


      status(updateResult) shouldBe NOT_FOUND
      afterEach()
    }
  }

  "ApplicationController .getGoogleBook()" should {

    "delete an individual entry in the data base with 202 Accepted response" in {

      beforeEach()

      val result: Future[Result] = TestApplicationController.getGoogleBook("flowers","inauthor")(FakeRequest())


      status(result) shouldBe ACCEPTED
      afterEach()
    }
  }

  "test name" should {
    "do something" in {
      beforeEach()
      afterEach()
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
