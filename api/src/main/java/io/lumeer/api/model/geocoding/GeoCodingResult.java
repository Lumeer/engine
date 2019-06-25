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
package io.lumeer.api.model.geocoding;

import java.util.Objects;

public class GeoCodingResult {

   private final GeoCodingProvider provider;
   private final String query;
   private final Object results;

   public GeoCodingResult(final GeoCodingProvider provider, final String query, final Object results) {
      this.provider = provider;
      this.query = query;
      this.results = results;
   }

   public GeoCodingProvider getProvider() {
      return provider;
   }

   public String getQuery() {
      return query;
   }

   public Object getResults() {
      return results;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final GeoCodingResult that = (GeoCodingResult) o;
      return provider == that.provider &&
            Objects.equals(query, that.query);
   }

   @Override
   public int hashCode() {
      return Objects.hash(provider, query);
   }

   @Override
   public String toString() {
      return "GeoCodingResult{" +
            "provider=" + provider +
            ", query='" + query + '\'' +
            ", results=" + results +
            '}';
   }
}
