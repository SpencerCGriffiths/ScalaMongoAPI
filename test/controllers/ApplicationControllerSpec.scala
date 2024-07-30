package controllers

import baseSpec.BaseSpecWithApplication
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._

class ApplicationControllerSpec extends BaseSpecWithApplication{

  val TestApplicationController = new ApplicationController(
    component
  )

  "ApplicationController .index()" should {

    // Testing the index() route of ApplicationController
    // FakeRequest() is needed to imitate an inbound HTTP request
    val result = TestApplicationController.index()(FakeRequest())


    "return 200 OK" in {

      status(result) shouldBe OK

      //status(result) shouldBe 501
    }

  }

  "ApplicationController .create()" should {

  }

  "ApplicationController .read()" should {

  }
  "ApplicationController .update()" should {

  }
  "ApplicationController .delete()" should {

  }

}
