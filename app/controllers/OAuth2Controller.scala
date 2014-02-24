package controllers

import play.api.mvc.{Action, Controller}
import scalaoauth2.provider.OAuth2Provider
import oauth.MyDataHandler

object OAuth2Controller extends Controller with OAuth2Provider {
  def accessToken = Action { implicit request =>
    issueAccessToken(new MyDataHandler)
  }
}
