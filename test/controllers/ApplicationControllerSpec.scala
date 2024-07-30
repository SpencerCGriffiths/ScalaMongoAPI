package controllers

import baseSpec.BaseSpecWithApplication
import models.DataModel
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
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
  }

  "ApplicationController .read()" should {

  }
  "ApplicationController .update()" should {

  }
  "ApplicationController .delete()" should {

  }

  "test name" should {
    "do something" in {

    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
