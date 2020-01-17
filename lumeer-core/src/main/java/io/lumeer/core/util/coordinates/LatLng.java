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
package io.lumeer.core.util.coordinates;

import java.util.Objects;

public class LatLng {
   private final Double lat;
   private final Double lng;

   public LatLng(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
   }

   public LatLng() {
      lat = null;
      lng = null;
   }

   public Double getLat() {
      return lat;
   }

   public Double getLng() {
      return lng;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final LatLng latLng = (LatLng) o;
      return Objects.equals(lat, latLng.lat) &&
            Objects.equals(lng, latLng.lng);
   }

   @Override
   public int hashCode() {
      return Objects.hash(lat, lng);
   }

   @Override
   public String toString() {
      return "LatLng{" +
            "lat=" + lat +
            ", lng=" + lng +
            '}';
   }
}
