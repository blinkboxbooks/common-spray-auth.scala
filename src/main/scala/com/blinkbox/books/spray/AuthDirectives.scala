package com.blinkbox.books.spray

import com.blinkbox.books.auth.{User, UserConstraint}
import com.blinkbox.books.spray.BearerTokenAuthenticator.credentialsMissingHeaders
import spray.http.HttpHeaders.Authorization
import spray.http.OAuth2BearerToken
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.Directives.{authenticate, authorize, optionalHeaderValueByType, provide, reject}
import spray.routing._
import spray.routing.directives.AuthMagnet

object AuthDirectives {

  def optionalAuthenticate[T](magnet: AuthMagnet[T]): Directive1[Option[T]] = optionalHeaderValueByType[Authorization](()).flatMap {
    case Some(_) => authenticate(magnet).flatMap(u => provide(Some(u)))
    case None => provide(None)
  }

  val authToken: Directive1[String] = optionalHeaderValueByType[Authorization](()).flatMap {
    case None         => reject(AuthenticationFailedRejection(CredentialsMissing, credentialsMissingHeaders))
    case Some(header) => extractAuthToken(header) match {
      case Right(token)    => provide(token)
      case Left(rejection) => reject(rejection)
    }
  }

  val optionalAuthToken: Directive1[Option[String]] = optionalHeaderValueByType[Authorization](()).flatMap {
    case None         => provide(None)
    case Some(header) => extractAuthToken(header) match {
      case Right(token)    => provide(Some(token))
      case Left(rejection) => reject(rejection)
    }
  }

  private def extractAuthToken(header: Authorization): Either[Rejection, String] = header match {
    case Authorization(OAuth2BearerToken(token)) => Right(token)
    case _ => Left(AuthenticationFailedRejection(CredentialsMissing, credentialsMissingHeaders))
  }

  def authenticateAndAuthorize(magnet: AuthMagnet[User], constraint: UserConstraint): Directive1[User] =
    authenticate(magnet).flatMap { u =>
      authorize(constraint(u)).hflatMap(_ => provide(u))
    }
}

