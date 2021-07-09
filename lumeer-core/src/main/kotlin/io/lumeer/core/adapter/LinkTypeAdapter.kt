package io.lumeer.core.adapter

import io.lumeer.api.model.LinkType
import io.lumeer.storage.api.dao.LinkInstanceDao

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
class LinkTypeAdapter(val linkInstanceDao: LinkInstanceDao) {

   fun getLinkInstancesCounts(): Map<String, Long> = linkInstanceDao.linkInstancesCounts

   fun getLinkInstancesCountByLinkType(linkTypeId: String): Long = linkInstanceDao.getLinkInstancesCountByLinkType(linkTypeId)

   fun mapLinkTypesComputedProperties(linkTypes: List<LinkType>): List<LinkType> {
      val counts = getLinkInstancesCounts();
      return linkTypes.onEach {
         it.linksCount = counts[it.id]?.or(0)
      }
   }

   fun mapLinkTypeComputedProperties(linkType: LinkType): LinkType = linkType.apply { linksCount = getLinkInstancesCountByLinkType(id) }
}
