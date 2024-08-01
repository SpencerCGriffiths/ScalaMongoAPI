package Services

import com.google.inject.Singleton
import Connectors.ApplicationConnector
import cats.data.EitherT
import models.{APIError, DataModel}

import java.awt.print.Book
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(connector: ApplicationConnector) {

  def getGoogleBook(urlOverride: Option[String] = None, search: String, term: String)(implicit ec: ExecutionContext): EitherT[Future, APIError ,DataModel] = {
    connector.get[DataModel](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search%$term"))
    // Map it in to a book
  }
}

// ^ TODO Information: 31/7  12:50
// ^ We are using google books api, hence the url as it is a free resource
// ^ urlOverride is a way to provide full url
// ^ term is the special keyword that can allow searching in particular fields with search

