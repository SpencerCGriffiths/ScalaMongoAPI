package controllers

import Services.ApplicationService
import baseSpec.BaseSpecWithApplication
import jdk.net.SocketFlow
import models.{APIError, DataModel, PartialDataModel}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.mvc.Results.Accepted
import play.api.test.Helpers.{status, _}
import repositories.DataRepository
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {

  val TestApplicationController = new ApplicationController(
    controllerComponents = component,
    dataRepository = repository,
    service = service,
    repoService = repoService
  )


  private val testDataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

  private val dataModelUpdateFull: PartialDataModel = PartialDataModel (
    Some("Update test full"),
    Some("Update test description full"),
    Some(1004)
  )
  private val dataModelUpdateName: PartialDataModel = PartialDataModel (
    Some("Update test name"),
    None,
    None
  )
  private val dataModelUpdateDesc: PartialDataModel = PartialDataModel (
    None,
    Some("Update test description"),
    None
  )
  private val dataModelUpdatePage: PartialDataModel = PartialDataModel (
    None,
    None,
    Some(1114)
  )


  "ApplicationController .index()" should {


    "return 200 OK when books are found" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(testDataModel))
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

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(testDataModel))
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
        books shouldBe Seq(testDataModel)
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

    /** TODO - 02/08 09:49 - trying to mock the database in order to return an error from database... TBC */
    //    "return 400 when no invalid request has been made on this path" in {
    //      beforeEach()
    //          val mockDataRepository = new MockDataRepository
    //          val controller = new ApplicationController(mockDataRepository, stubControllerComponents())
    //
    //          val result: Future[Result] = controller.index().apply(FakeRequest())
    //
    //          status(result) mustBe 400
    //          contentType(result) mustBe Some("application/json")
    //          (contentAsJson(result) \ "reason").as[String] mustBe "Error: Bad response from API along path"
    //          afterEach()
    //        }
  }


  "ApplicationController .create" when {

    "Successful" should {

      "return code 201 for created " in {
        beforeEach()

        val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED
        afterEach()
      }

      "return the book that was created" in {
        beforeEach()

        val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        val result = createdResult.futureValue

        // Manually convert ByteString to JSON
        val byteString = result.body.consumeData.futureValue
        val jsonString = byteString.utf8String
        val json = Json.parse(jsonString)
        val createdBook = json.as[DataModel]

        createdBook shouldBe testDataModel // Validate the created book

        afterEach()
      }
    }

    "not successful" should {

      "return a bad request status 400 when the dataModel is invalid" in {
        beforeEach()

        val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson("Bad Request- Invalid JSON Data Model"))
        val createdResult: Future[Result] = TestApplicationController.create()(request)


        status(createdResult) shouldBe Status.BAD_REQUEST
        afterEach()
      }
    }

    "return a Json error message of create failed when the dataModel Json is invalid" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson("Bad Request- Invalid JSON Data Model"))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val result = createdResult.futureValue
      // Manually convert ByteString to JSON
      val byteString = result.body.consumeData.futureValue
      //^ Pulls the ByteString: ByteString(34, 68, 97, 116, 97, 32, 110, 111, 116, 32, 102, 111, 117, 110, 100, 34)
      val jsonString = byteString.utf8String
      //^ Converts to JsonString:"Data not found"
      val errorMessage = Json.parse(jsonString)
      //^ Converts to Json: "Data not found"


      errorMessage shouldBe Json.toJson("Error in create: ")
      afterEach()
    }
  }



  "ApplicationController .read" when {

    "finding a book by id or name" should {

      "Return a 200 Ok Response when using ID" in {
        beforeEach()

        // Create the book in the database (mocking this step as it is not the focus of the test)
        val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
        val createResult: Future[Result] = TestApplicationController.create()(createRequest)

        status(createResult) mustBe CREATED

        // Read the book by name
        val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?id=${testDataModel._id}")
        val readResult: Future[Result] = TestApplicationController.read()(readRequest)

        status(readResult) mustBe OK
        contentAsJson(readResult).as[DataModel] mustBe testDataModel

        afterEach()
      }

      "Return the model that has been called from the database when using ID" in {
        beforeEach()

        // Create the book in the database (mocking this step as it is not the focus of the test)
        val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
        val createResult: Future[Result] = TestApplicationController.create()(createRequest)

        status(createResult) mustBe CREATED

        // Read the book by name
        val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?id=${testDataModel._id}")
        val readResult: Future[Result] = TestApplicationController.read()(readRequest)

        contentAsJson(readResult).as[DataModel]._id mustBe "abcd"
        contentAsJson(readResult).as[DataModel] mustBe testDataModel
        afterEach()
      }

      "Return a 200 Ok Response when using Name" in {
                beforeEach()

                // Create the book in the database (mocking this step as it is not the focus of the test)
                val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
                val createResult: Future[Result] = TestApplicationController.create()(createRequest)

                status(createResult) mustBe CREATED

                // Read the book by name
                val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?name=${testDataModel.name}")
                val readResult: Future[Result] = TestApplicationController.read()(readRequest)

                status(readResult) mustBe OK
                afterEach()
              }

      "Return the model that has been called when using Name" in {
                beforeEach()

                // Create the book in the database (mocking this step as it is not the focus of the test)
                val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
                val createResult: Future[Result] = TestApplicationController.create()(createRequest)

                status(createResult) mustBe CREATED

                // Read the book by name
                val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?name=${testDataModel.name}")
                val readResult: Future[Result] = TestApplicationController.read()(readRequest)

                contentAsJson(readResult).as[DataModel]._id mustBe "abcd"
                contentAsJson(readResult).as[DataModel] mustBe testDataModel
                afterEach()
              }

      "Return 400 code Bad Request when missing the parameter for id or name" in {
                beforeEach()

                // Create the book in the database (mocking this step as it is not the focus of the test)
                val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
                val createResult: Future[Result] = TestApplicationController.create()(createRequest)

                status(createResult) mustBe CREATED

                // Read the book by name
                val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read")
                val readResult: Future[Result] = TestApplicationController.read()(readRequest)


                status(readResult) mustBe BAD_REQUEST
                afterEach()
              }

      "Return error message: Either id or name must be provided <- when missing the parameter for id or name" in {
                beforeEach()

                // Create the book in the database (mocking this step as it is not the focus of the test)
                val createRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testDataModel))
                val createResult: Future[Result] = TestApplicationController.create()(createRequest)

                status(createResult) mustBe CREATED

                // Read the book by name
                val readRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read")
                val readResult: Future[Result] = TestApplicationController.read()(readRequest)

                contentAsJson(readResult).as[String] shouldBe "Either id or name must be provided"
                afterEach()
              }

      "Return 404 not found when book is not found in the database using ID or Name" in {
        beforeEach()

        // Read the book by name
        val readRequestName: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?name=${testDataModel.name}")
        val readResultName: Future[Result] = TestApplicationController.read()(readRequestName)

        val readRequestId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?id=${testDataModel._id}")
        val readResultId: Future[Result] = TestApplicationController.read()(readRequestId)


        status(readResultName) mustBe NOT_FOUND
        status(readResultId) mustBe NOT_FOUND
        afterEach()
      }

      "Return error message: -> Data not found <- when book is not found in the database using ID or Name" in {
        beforeEach()

        // Read the book by name
        val readRequestName: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?name=${testDataModel.name}")
        val readResultName: Future[Result] = TestApplicationController.read()(readRequestName)

        val readRequestId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"/api/read?id=${testDataModel._id}")
        val readResultId: Future[Result] = TestApplicationController.read()(readRequestId)

        contentAsJson(readResultName).as[String] shouldBe "Data not found"
        contentAsJson(readResultId).as[String] shouldBe "Data not found"
        afterEach()
      }

      "Return a database error if there is a bad response from the database" in {
        println("Wire mocking required for this- tbc at a later date")
        // TODO 02/08 15:31 - WireMocking for database error
      }
    }
  }

  "ApplicationController .update()" should {

    "Update an existing database entry" when {

      "All fields have value" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED


        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdateFull))
        val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)

        val json = contentAsJson(updateResult)
        // TODO - Convert to JSON:

        status(updateResult) shouldBe ACCEPTED
        (json \ "_id").as[String] mustBe "abcd"
        (json \ "name").as[String] mustBe "Update test full"
        (json \ "description").as[String] mustBe "Update test description full"
        (json \ "pageCount").as[Int] mustBe 1004

        afterEach()
      }

      "Only the name field is present" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED


        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdateName))
        val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


        val json = contentAsJson(updateResult)
        // TODO - Convert to JSON:

        status(updateResult) shouldBe ACCEPTED
        (json \ "_id").as[String] mustBe "abcd"
        (json \ "name").as[String] mustBe "Update test name"
        (json \ "description").as[String] mustBe "test description"
        (json \ "pageCount").as[Int] mustBe 100

        afterEach()
      }

      "Only the description field is present" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED


        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdateDesc))
        val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


        val json = contentAsJson(updateResult)
        // TODO - Convert to JSON:

        status(updateResult) shouldBe ACCEPTED
        (json \ "_id").as[String] mustBe "abcd"
        (json \ "name").as[String] mustBe "test name"
        (json \ "description").as[String] mustBe "Update test description"
        (json \ "pageCount").as[Int] mustBe 100

        afterEach()
      }

      "Only the pageCount field is present" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED


        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdatePage))
        val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


        val json = contentAsJson(updateResult)
        // TODO - Convert to JSON:

        status(updateResult) shouldBe ACCEPTED
        (json \ "_id").as[String] mustBe "abcd"
        (json \ "name").as[String] mustBe "test name"
        (json \ "description").as[String] mustBe "test description"
        (json \ "pageCount").as[Int] mustBe 1114

        afterEach()
      }
    }

    "Return a specific error" when {

      "(NotFound) when the item does not exist to be updated" in {
        beforeEach()
        // Entry with ID abcd has not been created yet to update
        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(dataModelUpdateFull))
        val updateResult: Future[Result] = TestApplicationController.update("1")(requestUpdate)


        status(updateResult) shouldBe NOT_FOUND
        afterEach()
      }

      "Return an error of BadRequest when the data to update is not valid Json" in {
        beforeEach()
        // Entry with ID abcd has been created but the data being sent to update isn't appropriate

        val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        status(createdResult) shouldBe Status.CREATED

        val requestUpdate: FakeRequest[JsValue] = buildPatch("/api/${testDataModel._id}").withBody[JsValue](Json.toJson("THIS IS NOT A VALID MODEL"))
        val updateResult: Future[Result] = TestApplicationController.update("abcd")(requestUpdate)


        status(updateResult) shouldBe BAD_REQUEST
        afterEach()
      }
    }
  }


  "ApplicationController .delete()" should {

    "delete an individual entry in the data base with 202 Accepted response" in {

      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${testDataModel._id}").withBody[JsValue](Json.toJson(testDataModel))
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
