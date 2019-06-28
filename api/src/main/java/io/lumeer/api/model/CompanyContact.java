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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CompanyContact {

   public static final String ID = "id";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String COMPANY = "company";
   public static final String FIRST_NAME = "firstName";
   public static final String LAST_NAME = "lastName";
   public static final String ADDRESS1 = "address1";
   public static final String ADDRESS2 = "address2";
   public static final String CITY = "city";
   public static final String ZIP = "zip";
   public static final String STATE = "state";
   public static final String COUNTRY = "country";
   public static final String EMAIL = "email";
   public static final String PHONE = "phone";
   public static final String IC = "ic";
   public static final String DIC = "dic";

   private String id;
   private String organizationId;
   private String company;
   private String firstName;
   private String lastName;
   private String address1;
   private String address2;
   private String city;
   private String zip;
   private String state;
   private String country;
   private String email;
   private String phone;
   private String ic;
   private String dic;
   private long version;

   @JsonCreator
   public CompanyContact(@JsonProperty(ID) final String id,
         @JsonProperty(ORGANIZATION_ID) final String organizationId, @JsonProperty(COMPANY) final String company,
         @JsonProperty(FIRST_NAME) final String firstName, @JsonProperty(LAST_NAME) final String lastName,
         @JsonProperty(ADDRESS1) final String address1, @JsonProperty(ADDRESS2) final String address2,
         @JsonProperty(CITY) final String city, @JsonProperty(ZIP) final String zip, @JsonProperty(STATE) final String state,
         @JsonProperty(COUNTRY) final String country, @JsonProperty(EMAIL) final String email,
         @JsonProperty(PHONE) final String phone, @JsonProperty(IC) final String ic,
         @JsonProperty(DIC) final String dic) {
      this.id = id;
      this.organizationId = organizationId;
      this.company = company;
      this.firstName = firstName;
      this.lastName = lastName;
      this.address1 = address1;
      this.address2 = address2;
      this.city = city;
      this.zip = zip;
      this.state = state;
      this.country = country;
      this.email = email;
      this.phone = phone;
      this.ic = ic;
      this.dic = dic;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public String getCompany() {
      return company;
   }

   public void setCompany(final String company) {
      this.company = company;
   }

   public String getFirstName() {
      return firstName;
   }

   public void setFirstName(final String firstName) {
      this.firstName = firstName;
   }

   public String getLastName() {
      return lastName;
   }

   public void setLastName(final String lastName) {
      this.lastName = lastName;
   }

   public String getAddress1() {
      return address1;
   }

   public void setAddress1(final String address1) {
      this.address1 = address1;
   }

   public String getAddress2() {
      return address2;
   }

   public void setAddress2(final String address2) {
      this.address2 = address2;
   }

   public String getCity() {
      return city;
   }

   public void setCity(final String city) {
      this.city = city;
   }

   public String getZip() {
      return zip;
   }

   public void setZip(final String zip) {
      this.zip = zip;
   }

   public String getState() {
      return state;
   }

   public void setState(final String state) {
      this.state = state;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(final String country) {
      this.country = country;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(final String email) {
      this.email = email;
   }

   public String getPhone() {
      return phone;
   }

   public void setPhone(final String phone) {
      this.phone = phone;
   }

   public String getIc() {
      return ic;
   }

   public void setIc(final String ic) {
      this.ic = ic;
   }

   public String getDic() {
      return dic;
   }

   public void setDic(final String dic) {
      this.dic = dic;
   }

   public long getVersion() {
      return version;
   }

   public void setVersion(final long version) {
      this.version = version;
   }

   @Override
   public String toString() {
      return "CompanyContact{" +
            "id='" + id + '\'' +
            ", organizationId='" + organizationId + '\'' +
            ", company='" + company + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", address1='" + address1 + '\'' +
            ", address2='" + address2 + '\'' +
            ", city='" + city + '\'' +
            ", zip='" + zip + '\'' +
            ", state='" + state + '\'' +
            ", country='" + country + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", ic='" + ic + '\'' +
            ", dic='" + dic + '\'' +
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
      final CompanyContact that = (CompanyContact) o;
      return Objects.equals(id, that.id) &&
            Objects.equals(organizationId, that.organizationId) &&
            Objects.equals(company, that.company) &&
            Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName) &&
            Objects.equals(address1, that.address1) &&
            Objects.equals(address2, that.address2) &&
            Objects.equals(city, that.city) &&
            Objects.equals(zip, that.zip) &&
            Objects.equals(state, that.state) &&
            Objects.equals(country, that.country) &&
            Objects.equals(email, that.email) &&
            Objects.equals(phone, that.phone) &&
            Objects.equals(ic, that.ic) &&
            Objects.equals(dic, that.dic);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, organizationId, company, firstName, lastName, address1, address2, city, zip, state, country, email, phone, ic, dic);
   }
}
