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
package io.lumeer.core.util;

import static java.util.stream.Collectors.toMap;

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LinkInstanceUtils {

   public static LinkInstance loadLinkInstanceWithData(final LinkInstanceDao linkInstanceDao, final LinkDataDao linkDataDao, final String linkInstanceId) {
      final LinkInstance linkInstance = linkInstanceDao.getLinkInstance(linkInstanceId);
      linkInstance.setData(linkDataDao.getData(linkInstance.getLinkTypeId(), linkInstanceId));

      return linkInstance;
   }

   public static List<LinkInstance> loadLinkInstancesData(final LinkDataDao dataDao, final LinkType linkType, final List<LinkInstance> linkInstances) {
      final Map<String, LinkInstance> linkInstanceMap = linkInstances.stream().collect(toMap(LinkInstance::getId, Function.identity()));
      final List<DataDocument> data = dataDao.getData(linkType.getId(), linkInstanceMap.keySet());
      data.forEach(dd -> {
         if (linkInstanceMap.containsKey(dd.getId())) {
            linkInstanceMap.get(dd.getId()).setData(dd);
         }
      });

      return linkInstances;
   }

   public static boolean isLinkInstanceOwner(final LinkType linkType, final LinkInstance linkInstance, String userId) {
      return userId.equals(linkInstance.getCreatedBy());
   }

}
