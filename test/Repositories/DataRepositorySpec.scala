package Repositories

import Connectors.ApplicationConnector
import Services.{ApplicationService, RepositoryService}
import baseSpec.BaseSpec
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import models.{APIError, DataModel}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import repositories.{DataRepository, DataRepositoryTrait}
import views.html.defaultpages.error

import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.result.UpdateResult
import org.bson.BsonValue

import scala.Option.when

class DataRepositorySpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
  val mockRepository: DataRepositoryTrait = mock[DataRepositoryTrait]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new RepositoryService(mockRepository)

  private val testDataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )


  private val dataModels = Seq(testDataModel)

  "RepositoryService" when {
    "Calling Index" should {
      "return a list of data models" in {

        // Step 2: Mock the index method to return the expected data
        (mockRepository.index _)
          .expects()
          .returning(Future.successful(Right(dataModels)))
        //^ Underscore is needed to convert method in to a function

        // Step 3: Call the index method on testService and verify the result
        val result = testService.index()

        // Step 4: Check that the result is what we expected
        result.futureValue shouldEqual Right(dataModels)
      }

      "handle an error when the database returns a bad response" in {
        // Step 1: Mock the index method to return an error
        (mockRepository.index _).expects().returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))

        // Step 2: Call the index method on testService and verify the result
        val result = testService.index()

        // Step 3: Check that the result is the expected error
        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }
    }

    "Calling Create" should {

      "Create a book and return the created book" in {

        (mockRepository.create _).expects(testDataModel).returning(Future.successful(Right(testDataModel)))

        val result = testService.create(testDataModel)

        result.futureValue shouldEqual Right(testDataModel)
      }

      "Return a Left error when the book could not be created" in {

        (mockRepository.create _).expects(testDataModel).returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))

        val result = testService.create(testDataModel)

        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }
    }

    "Calling Read" should {

      "return the test data when searching by ID" in {
        // Step 2: Mock the index method to return the expected data
        (mockRepository.read _).expects(Some("abcd"), None).returning(Future.successful(Right(testDataModel)))
        //^ Underscore is needed to convert method in to a function

        // Step 3: Call the index method on testService and verify the result
        val result = testService.read(Some("abcd"), None)

        // Step 4: Check that the result is what we expected
        result.futureValue shouldEqual Right(testDataModel)
      }

      "return the test data when searching by Name" in {
        // Step 2: Mock the index method to return the expected data
        (mockRepository.read _).expects(None, Some("test name")).returning(Future.successful(Right(testDataModel)))
        //^ Underscore is needed to convert method in to a function

        // Step 3: Call the index method on testService and verify the result
        val result = testService.read(None, Some("test name"))

        // Step 4: Check that the result is what we expected
        result.futureValue shouldEqual Right(testDataModel)
      }

      "return a database error when searching by ID and there is a database error" in {
        // Step 2: Mock the index method to return the expected data
        (mockRepository.read _).expects(Some("abcd"), None).returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))
        //^ Underscore is needed to convert method in to a function

        // Step 3: Call the index method on testService and verify the result
        val result = testService.read(Some("abcd"), None)

        // Step 4: Check that the result is what we expected
        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }

      "return a database error when searching by name and there is a database error" in {
        // Step 2: Mock the index method to return the expected data
        (mockRepository.read _).expects(None, Some("test name")).returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))
        //^ Underscore is needed to convert method in to a function

        // Step 3: Call the index method on testService and verify the result
        val result = testService.read(None, Some("test name"))

        // Step 4: Check that the result is what we expected
        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }

    }

    "Calling Update" should {

      "Return the an updated result object" in {
        // Mock the UpdateResult
        val updateResult = mock[UpdateResult]
        // this test differs because it returns a match count, modified count and upsert id
        // Set up the repository mock
        (mockRepository.update _).expects("abcd", testDataModel).returning(Future.successful(Right(updateResult)))

        val result = testService.update("abcd", testDataModel)

        result.futureValue shouldEqual Right(updateResult)
      }
      "Return the Left error with a database error" in {
        // Mock the UpdateResult
        val updateResult = mock[UpdateResult]
        // this test differs because it returns a match count, modified count and upsert id
        // Set up the repository mock
        (mockRepository.update _).expects("abcd", testDataModel).returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))

        val result = testService.update("abcd", testDataModel)

        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }
    }

    "Calling delete" should {

      "Return a deleted result response" in {
        // Mock the DeleteResult
        val deleteResult = mock[DeleteResult]

        // Set up the repository mock
        (mockRepository.delete _).expects("abcd").returning(Future.successful(Right(deleteResult)))

        val result = testService.delete("abcd")

        result.futureValue shouldEqual Right(deleteResult)
      }

      "Return the Left error with a database error" in {
        // Set up the repository mock for error case
        (mockRepository.delete _).expects("abcd").returning(Future.successful(Left(APIError.DatabaseError("Error: bad response from database"))))

        val result = testService.delete("abcd")

        result.futureValue shouldEqual Left(APIError.DatabaseError("Error: bad response from database"))
      }
    }
  }
}