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
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.core.task.executor.operation.ViewPermissionsOperation
import io.lumeer.core.util.Tuple
import io.lumeer.core.util.Utils

class ViewsUpdatingStage(executor: OperationExecutor) : Stage(executor) {

   override fun call(): ChangesTracker {
      operations.takeIf { it.isNotEmpty() }?.let {

         val viewUpdates : Map<String, List<ViewPermissionsOperation>> = Utils.categorize(
               it.filter { operation -> operation is ViewPermissionsOperation && operation.isComplete }
                     .map { operation -> (operation as ViewPermissionsOperation) }
                     .stream()
         ) { op -> op.entity.id }

         viewUpdates.keys.forEach { key ->
            viewUpdates[key]?.takeIf { list -> list.isNotEmpty() }?.let { list ->
               Tuple(list[0].originalView, list[0].entity)
            }.takeIf { viewTuple -> viewTuple?.first != null && viewTuple?.second != null }?.let { viewTuple ->
               viewUpdates[key]?.forEach { update ->
                  viewTuple.second.permissions.updateUserPermissions(setOf(Permission.buildWithRoles(update.userId, update.roles)))
               }

               val updatedView = task.daoContextSnapshot.viewDao.updateView(viewTuple.first.id, viewTuple.second)
               changesTracker.addUpdatedViews(mutableSetOf(Tuple(viewTuple.first, updatedView)))
            }
         }

         return changesTracker
      }

      return ChangesTracker()
   }
}
