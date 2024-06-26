package utg.repos.sql

import eu.timepit.refined.types.string.NonEmptyString
import shapeless.HNil
import skunk._
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.UserId
import utg.domain.args.users.UserFilters
import utg.domain.auth.AccessCredentials

private[repos] object UsersSql extends Sql[UserId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: nes *: nes *: phone *: RolesSql.id *: AssetsSql.id.opt)
      .to[dto.User]
  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(
          data = user,
          password = hash,
        )
    }

  val findByLogin: Query[NonEmptyString, AccessCredentials[dto.User]] =
    sql"""SELECT id, created_at, firstname, lastname, login, phone, role_id, asset_id, password FROM users
          WHERE login = $nes LIMIT 1""".query(accessCredentialsDecoder)

  val findById: Query[UserId, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, login, phone, role_id, asset_id FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, login, phone, role_id, asset_id FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES ($id, $zonedDateTime, $nes, $nes, $nes, $phone, ${RolesSql.id}, ${AssetsSql.id.opt}, $passwordHash)"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.login *:
          u.data.phone *: u.data.roleId *: u.data.assetId *: u.password *: EmptyTuple
      }

  val update: Command[dto.User] =
    sql"""UPDATE users
       SET firstname = $nes,
       lastname = $nes,
       phone = $phone,
       role_id = ${RolesSql.id},
       asset_id = ${AssetsSql.id.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.phone *: user.roleId *: user.assetId *: user.id *: EmptyTuple

      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"u.id = $id"),
      filters.roleId.map(sql"u.role_id = ${RolesSql.id}"),
      filters.name.map(s => s"%$s%").map(sql"u.firstname + ' ' + u.lastname ILIKE $varchar"),
    )

  def select(filters: UserFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT 
              u.id AS user_id,
              u.created_at AS created_at,
              u.firstname AS firstname,
              u.lastname  AS lastname,
              u.login AS login,
              u.phone AS phone,
              u.role_id AS role_id,
              u.asset_id AS asset_id,
              COUNT(*) OVER() AS total
            FROM users u"""
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }
}
