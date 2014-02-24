package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import play.api.db.slick.DB
import play.api.Play.current
import java.util.Date

case class Client(id: String, secret: Option[String], redirectUri: Option[String], scope: Option[String])
class Clients(tag: Tag) extends Table[Client](tag, "client") {
  def id = column[String]("id", O.PrimaryKey)
  def secret = column[Option[String]]("secret")
  def redirectUri = column[Option[String]]("redirect_uri")
  def scope = column[Option[String]]("scope")
  def * = (id, secret, redirectUri, scope) <> (Client.tupled, Client.unapply _)
}

case class GrantType(id: Int, grantType: String)
class GrantTypes(tag: Tag) extends Table[GrantType](tag, "grant_type") {
  def id = column[Int]("id", O.PrimaryKey)
  def grantType = column[String]("grant_type")
  def * = (id, grantType) <> (GrantType.tupled, GrantType.unapply _)
}

case class ClientGrantType(clientId: String, grantTypeId: Int)
class ClientGrantTypes(tag: Tag) extends Table[ClientGrantType](tag, "client_grant_type") {
  def clientId = column[String]("client_id")
  def grantTypeId = column[Int]("grant_type_id")
  def * = (clientId, grantTypeId) <> (ClientGrantType.tupled, ClientGrantType.unapply _)
  val pk = primaryKey("pk_client_grant_type", (clientId, grantTypeId))
}

case class AccessToken(accessToken: String, refreshToken: Option[String], userId: Long, scope: Option[String], expiresIn: Int, createdAt: java.sql.Timestamp, clientId: String)
class AccessTokens(tag: Tag) extends Table[AccessToken](tag, "access_token") {
  def accessToken = column[String]("access_token", O.PrimaryKey)
  def refreshToken = column[Option[String]]("refresh_token")
  def userId = column[Long]("user_id")
  def scope = column[Option[String]]("scope")
  def expiresIn = column[Int]("expires_in")
  def createdAt = column[java.sql.Timestamp]("created_at")
  def clientId = column[String]("client_id")
  def * = (accessToken, refreshToken, userId, scope, expiresIn, createdAt, clientId) <> (AccessToken.tupled, AccessToken.unapply _)
}

case class AuthCode(authorizationCode: String, userId: Long, redirectUri: Option[String], createdAt: java.sql.Timestamp, scope: Option[String], clientId: String, expiresIn: Int)
class AuthCodes(tag: Tag) extends Table[AuthCode](tag, "auth_code") {
  def authorizationCode = column[String]("authorization_code", O.PrimaryKey)
  def userId = column[Long]("user_id")
  def redirectUri = column[Option[String]]("redirect_uri")
  def createdAt = column[java.sql.Timestamp]("created_at")
  def scope = column[Option[String]]("scope")
  def clientId = column[String]("client_id")
  def expiresIn = column[Int]("expires_in")
  def * = (authorizationCode, userId, redirectUri, createdAt, scope, clientId, expiresIn) <> (AuthCode.tupled, AuthCode.unapply _)
}

case class User(id: Long, username: String, email: String, password: String)
class Users(tag: Tag) extends Table[User](tag, "user") {
  def id = column[Long]("id", O.PrimaryKey)
  def username = column[String]("username")
  def email = column[String]("email")
  def password = column[String]("password")
  def * = (id, username, email, password) <> (User.tupled, User.unapply _)
}

private[models] trait DAO {
  val Clients = TableQuery[Clients]
  val Users = TableQuery[Users]
  val GrantTypes = TableQuery[GrantTypes]
  val ClientGrantTypes = TableQuery[ClientGrantTypes]
  val AccessTokens = TableQuery[AccessTokens]
  val AuthCodes = TableQuery[AuthCodes]
}


object Clients extends DAO {
  def validate(id: String, secret: String, grantType: String): Boolean = {
    DB.withTransaction { implicit session =>
      val check = for {
        ((c, cgt), gt) <- Clients innerJoin ClientGrantTypes on (_.id === _.clientId) innerJoin GrantTypes on (_._2.grantTypeId === _.id)
        if c.id === id && c.secret === secret && gt.grantType === grantType
      } yield 0
      check.firstOption.isDefined
    }
  }

  def findById(id: String): Option[Client] = {
    DB.withTransaction { implicit session =>
      Clients.filter(c => c.id === id).firstOption
    }
  }
}

object Users extends DAO {
  def findUser(username: String, password: String): Option[User] = {
    DB.withTransaction { implicit session =>
      Users.where(u => u.username === username && u.password === password).firstOption
    }
  }

  def getById(id: Long): Option[User] = {
    DB.withTransaction { implicit session =>
      Users.where(u => u.id === id).firstOption
    }
  }
}

object AccessTokens extends DAO {
  def create(accessToken: AccessToken) = {
    DB.withTransaction { implicit session =>
      AccessTokens.insert(accessToken)
    }
  }

  def deleteExistingAndCreate(accessToken: AccessToken, userId: Long, clientId: String) = {
    DB.withTransaction { implicit session =>
      // these two operations should happen inside a transaction
      AccessTokens.where(a => a.clientId === clientId && a.userId === userId).delete
      AccessTokens.insert(accessToken)
    }
  }

  def findToken(userId: Long, clientId: String): Option[AccessToken] = {
    DB.withTransaction { implicit session =>
      AccessTokens.where(a => a.clientId === clientId && a.userId === userId).firstOption
    }
  }

  def findAccessToken(token: String): Option[AccessToken] = {
    DB.withTransaction { implicit session =>
      AccessTokens.where(a => a.accessToken === token).firstOption
    }
  }

  def findRefreshToken(token: String): Option[AccessToken] = {
    DB.withTransaction { implicit session =>
      AccessTokens.where(a => a.refreshToken === token).firstOption
    }
  }
}


object AuthCodes extends DAO {
  def find(code: String) = {
    DB.withTransaction { implicit session =>
      val authCode = AuthCodes.where(a => a.authorizationCode === code).firstOption

      // filtering out expired authorization codes
      authCode.filter(p => p.createdAt.getTime + (p.expiresIn * 1000) > new Date().getTime)
    }
  }
}