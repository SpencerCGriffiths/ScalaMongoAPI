package Services

import models.{APIError, DataModel}
import org.mongodb.scala.result
import repositories.{DataRepository, DataRepositoryTrait}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RepositoryService @Inject()(val dataRepository: DataRepositoryTrait)(implicit ec: ExecutionContext) {

  def index(): Future[Either[APIError, Seq[DataModel]]] = {
    dataRepository.index().map {
      case Right(data) => Right(data)
      case Left(error) => Left(error)
    }.recover {
      case _ => Left(APIError.BadAPIResponse(500, "Error: error in RepoService"))
    }
  }

  def create(book: DataModel): Future[Either[APIError, DataModel]] = {
    dataRepository.create(book).map{
      case Right(data) => Right(data)
      case Left(error) => Left(error)
    }. recover {
      case _ => Left(APIError.BadAPIResponse(500, "Error: error in RepoService"))
    }
  }

  def read(id: Option[String], name: Option[String]): Future[Either[APIError, DataModel]] = {
      dataRepository.read(id, name).map {
        case Right(data) => Right(data)
        case Left(error) => Left(error)
      }.recover {
        case _ => Left(APIError.BadAPIResponse(500, "Error: error in RepoService"))
      }
    }

  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    dataRepository.update(id, book).map {
      case Right(data) => Right(data)
      case Left(error) => Left(error)
    }.recover {
      case _  => Left(APIError.BadAPIResponse(500, "Error: error in RepoService"))
    }
  }

  def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    dataRepository.delete(id).map {
      case Right(data) => Right(data)
      case Left(error) => Left(error)
    }.recover{
      case _  => Left(APIError.BadAPIResponse(500, "Error: error in RepoService"))
    }
  }


}

