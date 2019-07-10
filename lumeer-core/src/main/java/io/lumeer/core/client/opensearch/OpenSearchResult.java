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
package io.lumeer.core.client.opensearch;

import io.lumeer.api.model.geocoding.Coordinates;
import io.lumeer.api.model.geocoding.Location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchResult {

   private final OpenSearchAddress address;
   private final float lat;
   private final float lon;
   private final String osmType;

   @JsonCreator
   public OpenSearchResult(
         @JsonProperty("address") final OpenSearchAddress address,
         @JsonProperty("lat") final float lat,
         @JsonProperty("lon") final float lon,
         @JsonProperty("osm_type") final String osmType) {
      this.address = address;
      this.lat = lat;
      this.lon = lon;
      this.osmType = osmType;
   }

   public OpenSearchAddress getAddress() {
      return address;
   }

   public float getLat() {
      return lat;
   }

   public float getLon() {
      return lon;
   }

   public String getOsmType() {
      return osmType;
   }

   public Location toLocation() {
      return new Location(address.toAddress(), new Coordinates(lat, lon));
   }
}
