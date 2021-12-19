/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.Permission
import io.lumeer.api.model.Role
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.core.task.executor.operation.ViewPermissionsOperation
import io.lumeer.core.util.Tuple

class ViewsUpdatingStage(executor: OperationExecutor) : Stage(executor) {

   override fun call(): ChangesTracker {
      if (operations.isEmpty()) {
         return ChangesTracker()
      }

      val viewUpdates = operations.orEmpty().filter { operation -> operation is ViewPermissionsOperation && operation.isComplete }
            .map { operation -> (operation as ViewPermissionsOperation) }
            .groupBy { operation -> operation.entity.id }

      viewUpdates.entries.forEach { (_, operations) ->
         val originalView = operations.firstOrNull()?.getOriginalView()
         val view = operations.firstOrNull()?.entity

         if (originalView != null && view != null) {
            operations.forEach { operation ->
               val roles = operation.roles.map { Role(it) }.toSet()
               view.permissions.updateUserPermissions(setOf(Permission.buildWithRoles(operation.userId, roles)))
            }

            val updatedView = task.daoContextSnapshot.viewDao.updateView(originalView.id, view)
            changesTracker.addUpdatedViews(mutableSetOf(Tuple(originalView, updatedView)))
         }
      }

      return changesTracker
   }
}
