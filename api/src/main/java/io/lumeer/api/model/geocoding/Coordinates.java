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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Coordinates implements Serializable {

   @JsonProperty("lat")
   private final float latitude;

   @JsonProperty("lng")
   private final float longitude;

   public Coordinates(final String coordinates) {
      var coordinatesArray = coordinates.split(",");
      if (coordinatesArray.length != 2) {
         throw new IllegalArgumentException("Invalid coordinates format");
      }

      this.latitude = Float.parseFloat(coordinatesArray[0].trim());
      this.longitude = Float.parseFloat(coordinatesArray[1].trim());
   }

   public Coordinates(final float latitude, final float longitude) {
      this.latitude = latitude;
      this.longitude = longitude;
   }

   public float getLatitude() {
      return latitude;
   }

   public float getLongitude() {
      return longitude;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Coordinates that = (Coordinates) o;
      return Float.compare(that.latitude, latitude) == 0 &&
            Float.compare(that.longitude, longitude) == 0;
   }

   @Override
   public int hashCode() {
      return Objects.hash(latitude, longitude);
   }

   @Override
   public String toString() {
      return latitude + "," + longitude;
   }
}
