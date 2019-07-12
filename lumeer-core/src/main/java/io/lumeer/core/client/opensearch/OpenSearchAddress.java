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

import io.lumeer.api.model.geocoding.Address;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchAddress {

   private final String city;
   private final String cityDistrict;
   private final String continent;
   private final String country;
   private final String countryCode;
   private final String county;
   private final String houseNumber;
   private final String neighbourhood;
   private final String postcode;
   private final String road;
   private final String state;
   private final String suburb;
   private final String town;

   @JsonCreator
   public OpenSearchAddress(
         @JsonProperty("city") final String city,
         @JsonProperty("city_district") final String cityDistrict,
         @JsonProperty("continent") final String continent,
         @JsonProperty("country") final String country,
         @JsonProperty("country_code") final String countryCode,
         @JsonProperty("county") final String county,
         @JsonProperty("house_number") final String houseNumber,
         @JsonProperty("neighbourhood") final String neighbourhood,
         @JsonProperty("postcode") final String postcode,
         @JsonProperty("road") final String road,
         @JsonProperty("state") final String state,
         @JsonProperty("suburb") final String suburb,
         @JsonProperty("town") final String town) {
      this.city = city;
      this.cityDistrict = cityDistrict;
      this.continent = continent;
      this.country = country;
      this.countryCode = countryCode;
      this.county = county;
      this.houseNumber = houseNumber;
      this.neighbourhood = neighbourhood;
      this.postcode = postcode;
      this.road = road;
      this.state = state;
      this.suburb = suburb;
      this.town = town;
   }

   public String getCity() {
      return city;
   }

   public String getCityDistrict() {
      return cityDistrict;
   }

   public String getContinent() {
      return continent;
   }

   public String getCountry() {
      return country;
   }

   public String getCountryCode() {
      return countryCode;
   }

   public String getCounty() {
      return county;
   }

   public String getHouseNumber() {
      return houseNumber;
   }

   public String getNeighbourhood() {
      return neighbourhood;
   }

   public String getPostcode() {
      return postcode;
   }

   public String getRoad() {
      return road;
   }

   public String getState() {
      return state;
   }

   public String getSuburb() {
      return suburb;
   }

   public String getTown() {
      return town;
   }

   public Address toAddress() {
      String city = this.city != null ? this.city : this.town;
      return new Address(city, country, county, houseNumber, postcode, state, road);
   }
}
