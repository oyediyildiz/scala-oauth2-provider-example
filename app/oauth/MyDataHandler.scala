package oauth

import scalaoauth2.provider.{AuthInfo, DataHandler}
import java.util.Date
import java.sql.Timestamp
import util.Crypto
import models._

class MyDataHandler extends DataHandler[models.User] {
  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean = {
    Clients.validate(clientId, clientSecret, grantType)
  }

  def findUser(username: String, password: String): Option[User] = {
    Users.findUser(username, password)
  }

  def createAccessToken(authInfo: AuthInfo[User]): scalaoauth2.provider.AccessToken = {
    val accessTokenExpiresIn = 60 * 60 // 1 hour
    val now = new Date()
    val createdAt = new Timestamp(now.getTime)
    val refreshToken = Some(Crypto.generateToken())
    val accessToken = Crypto.generateToken()

    val tokenObject = models.AccessToken(accessToken, refreshToken, authInfo.user.id.toInt, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId)
    AccessTokens.deleteExistingAndCreate(tokenObject, authInfo.user.id.toInt, authInfo.clientId)
    scalaoauth2.provider.AccessToken(accessToken, refreshToken, authInfo.scope, Some(accessTokenExpiresIn.toLong), now)
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[scalaoauth2.provider.AccessToken] = {
    AccessTokens.findToken(authInfo.user.id, authInfo.clientId) map { a =>
      scalaoauth2.provider.AccessToken(a.accessToken, a.refreshToken, a.scope, Some(a.expiresIn.toLong), a.createdAt)
    }
  }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): scalaoauth2.provider.AccessToken = {
    createAccessToken(authInfo)
  }

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = {
    None // Not implemented yet
  }

  def findAccessToken(token: String): Option[scalaoauth2.provider.AccessToken] = {
    AccessTokens.findAccessToken(token) map { a =>
      scalaoauth2.provider.AccessToken(a.accessToken, a.refreshToken, a.scope, Some(a.expiresIn.toLong), a.createdAt)
    }
  }

  def findAuthInfoByAccessToken(accessToken: scalaoauth2.provider.AccessToken): Option[AuthInfo[User]] = {
    AccessTokens.findAccessToken(accessToken.token) map { a =>
      val user = Users.getById(a.userId).get
      AuthInfo(user, a.clientId, a.scope, Some(""))
    }
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = {
    AccessTokens.findRefreshToken(refreshToken) map { a =>
      val user = Users.getById(a.userId).get
      AuthInfo(user, a.clientId, a.scope, Some(""))
    }
  }

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] = {
    AuthCodes.find(code) map { a =>
      val user = Users.getById(a.userId).get
      AuthInfo(user, a.clientId, a.scope, a.redirectUri)
    }
  }
}
