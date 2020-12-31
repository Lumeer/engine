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

import io.lumeer.api.model.ResourceType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionRuleJsParser {

   public static List<ResourceReference> parseRuleFunctionJs(final String js, final Set<String> collectionIds, final Set<String> linkTypeIds) {
      final List<ResourceReference> result = new ArrayList<>();

      if (StringUtils.isNotEmpty(js)) {
         final Set<String> ids = new HashSet<>();
         final Matcher matcher = Pattern.compile("'([a-f0-9]{24})'").matcher(js);

         while (matcher.find()) {
            ids.add(matcher.group(1));
         }

         System.out.println(ids);

         ids.forEach(id -> {
            if (collectionIds.contains(id)) {
               result.add(new ResourceReference(ResourceType.COLLECTION, id));
            } else if (linkTypeIds.contains(id)) {
               result.add(new ResourceReference(ResourceType.LINK, id));
            }
         });
      }

      System.out.println(result);

      return result;
   }

   public static class ResourceReference {
      private ResourceType resourceType;
      private String id;

      public ResourceReference(final ResourceType resourceType, final String id) {
         this.resourceType = resourceType;
         this.id = id;
      }

      public ResourceType getResourceType() {
         return resourceType;
      }

      public String getId() {
         return id;
      }

      @Override
      public String toString() {
         return "ResourceReference{" +
               "resourceType=" + resourceType +
               ", id='" + id + '\'' +
               '}';
      }

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         final ResourceReference that = (ResourceReference) o;
         return resourceType == that.resourceType && Objects.equals(id, that.id);
      }

      @Override
      public int hashCode() {
         return Objects.hash(resourceType, id);
      }
   }
}
