package utg.domain.args.users

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

import utg.domain.RoleId
import utg.domain.UserId

case class UserFilters(
    id: Option[UserId] = None,
    roleId: Option[RoleId] = None,
    name: Option[NonEmptyString] = None,
    limit: Option[PosInt] = None,
    offset: Option[PosInt] = None,
  )
