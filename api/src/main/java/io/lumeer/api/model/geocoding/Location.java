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

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Location implements Serializable {

   private final Address address;
   private final Coordinates coordinates;

   public Location(final Address address, final Coordinates coordinates) {
      this.address = address;
      this.coordinates = coordinates;
   }

   public Address getAddress() {
      return address;
   }

   public Coordinates getCoordinates() {
      return coordinates;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Location location = (Location) o;
      return Objects.equals(address, location.address) &&
            Objects.equals(coordinates, location.coordinates);
   }

   @Override
   public int hashCode() {
      return Objects.hash(address, coordinates);
   }

   @Override
   public String toString() {
      return "Location{" +
            "address=" + address +
            ", coordinates=" + coordinates +
            '}';
   }
}
