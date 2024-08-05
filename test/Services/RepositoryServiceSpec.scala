package services

import Connectors.ApplicationConnector
import Services.{ApplicationService, RepositoryService}
import baseSpec.{BaseSpec, BaseSpecWithApplication}
import models.{APIError, DataModel}
import org.scalamock.clazz.MockImpl.mock
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import repositories.DataRepository

import scala.Option.when
import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  val mockDataRepository: DataRepository = mock[DataRepository]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new RepositoryService(mockDataRepository)

  private val testDataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

}
