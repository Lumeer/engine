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
public class Address implements Serializable {

   private final String city;
   private final String country;
   private final String county;
   private final String houseNumber;
   private final String postalCode;
   private final String state;
   private final String street;
   private final String cityDistrict;
   private final String suburb;

   public Address(
         final String city,
         final String country,
         final String county,
         final String houseNumber,
         final String postalCode,
         final String state,
         final String street,
         final String cityDistrict,
         final String suburb) {
      this.city = city;
      this.country = country;
      this.county = county;
      this.houseNumber = houseNumber;
      this.postalCode = postalCode;
      this.state = state;
      this.street = street;
      this.cityDistrict = cityDistrict;
      this.suburb = suburb;
   }

   public String getCity() {
      return city;
   }

   public String getCountry() {
      return country;
   }

   public String getCounty() {
      return county;
   }

   public String getHouseNumber() {
      return houseNumber;
   }

   public String getPostalCode() {
      return postalCode;
   }

   public String getState() {
      return state;
   }

   public String getStreet() {
      return street;
   }

   public String getCityDistrict() {
      return cityDistrict;
   }

   public String getSuburb() {
      return suburb;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Address address = (Address) o;
      return Objects.equals(city, address.city) &&
            Objects.equals(country, address.country) &&
            Objects.equals(county, address.county) &&
            Objects.equals(houseNumber, address.houseNumber) &&
            Objects.equals(postalCode, address.postalCode) &&
            Objects.equals(state, address.state) &&
            Objects.equals(street, address.street) &&
            Objects.equals(cityDistrict, address.cityDistrict) &&
            Objects.equals(suburb, address.suburb);
   }

   @Override
   public int hashCode() {
      return Objects.hash(city, country, county, houseNumber, postalCode, state, street, cityDistrict, suburb);
   }

   @Override
   public String toString() {
      return "Address{" +
            "city='" + city + '\'' +
            ", country='" + country + '\'' +
            ", county='" + county + '\'' +
            ", houseNumber='" + houseNumber + '\'' +
            ", postalCode='" + postalCode + '\'' +
            ", state='" + state + '\'' +
            ", street='" + street + '\'' +
            ", cityDistrict='" + cityDistrict + '\'' +
            ", suburb='" + suburb + '\'' +
            '}';
   }
}
