package utg

import cats.effect.Async
import cats.effect.Resource
import skunk.Session

import utg.repos._

case class Repositories[F[_]](
    users: UsersRepository[F],
    assets: AssetsRepository[F],
  )
object Repositories {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): Repositories[F] =
    Repositories(
      users = UsersRepository.make[F],
      assets = AssetsRepository.make[F],
    )
}
