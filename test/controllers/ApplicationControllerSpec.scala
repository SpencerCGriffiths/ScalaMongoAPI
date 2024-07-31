package controllers

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
    controllerComponents = component, dataRepository = repository
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

    // Testing the index() route of ApplicationController
    // FakeRequest() is needed to imitate an inbound HTTP request
    val result = TestApplicationController.index()(FakeRequest())


    "return 200 OK" in {
      beforeEach()
      status(result) shouldBe OK
      afterEach()
      //status(result) shouldBe 501
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

  "test name" should {
    "do something" in {
      beforeEach()
      afterEach()
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
