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

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.lumeer.api.model.LinkInstance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkTypeUtils {

   private LinkTypeUtils() {
   }

   public static Map<String, List<LinkInstance>> getLinksByType(final List<LinkInstance> linkInstances) {
      return linkInstances.stream().collect(Collectors.groupingBy(LinkInstance::getLinkTypeId, mapping(d -> d, toList())));
   }

}
