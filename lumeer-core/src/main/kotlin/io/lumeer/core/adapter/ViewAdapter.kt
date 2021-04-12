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
package io.lumeer.core.adapter

import io.lumeer.api.model.Permission
import io.lumeer.api.model.View
import io.lumeer.api.util.ResourceUtils
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.FavoriteItemDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.ViewDao

class ViewAdapter(private val viewDao: ViewDao, private val linkTypeDao: LinkTypeDao, private val favoriteItemDao: FavoriteItemDao) {

   fun getFavoriteViewIds(userId: String, projectId: String): Set<String> = favoriteItemDao.getFavoriteViewIds(userId, projectId)

   fun isFavorite(viewId: String, userId: String, projectId: String): Boolean = getFavoriteViewIds(userId, projectId).contains(viewId)

   fun mapViewsData(views: List<View>, userId: String, projectId: String): List<View>  {
      val favoriteIds = getFavoriteViewIds(userId, projectId);
      return views.onEach {
        it.isFavorite = favoriteIds.contains(it.id)
      }
   }

   fun mapViewData(view: View, userId: String, projectId: String): View = view.apply {
      isFavorite = isFavorite(view.id, userId, projectId)
   }

   fun getUsersIdsInViewByCollection(collectionId: String, check: (Permission) -> Boolean ): Set<String> {
      val views = viewDao.allViews
      val linkTypes = linkTypeDao.allLinkTypes

       return views.filter { QueryUtils.getQueryCollectionIds(it.query, linkTypes).contains(collectionId) }
               .map { view -> view.permissions?.userPermissions.orEmpty().filter { permission -> check(permission) } }
               .flatMap { list -> list.map { permission -> permission.id } }
               .toSet()
   }

}
