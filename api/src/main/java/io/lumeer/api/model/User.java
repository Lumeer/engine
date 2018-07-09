/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.dto.adapter.ZonedDateTimeAdapter;
import io.lumeer.api.view.UserViews;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String EMAIL = "email";
   public static final String GROUPS = "groups";
   public static final String WISHES = "wishes";
   public static final String AGREEMENT = "agreement";
   public static final String AGREEMENT_DATE = "agreementDate";
   public static final String NEWSLETTER = "newsletter";

   @JsonView(UserViews.DefaultView.class)
   private String id;

   @JsonView(UserViews.DefaultView.class)
   private String name;

   @JsonView(UserViews.DefaultView.class)
   private String email;

   @JsonIgnore
   private String authId;

   @JsonView(UserViews.DefaultView.class)
   private Map<String, Set<String>> groups;

   @JsonView(UserViews.FullView.class)
   private DefaultWorkspace defaultWorkspace;

   @JsonView(UserViews.FullView.class)
   private Boolean agreement;

   @JsonView(UserViews.FullView.class)
   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime agreementDate;

   @JsonView(UserViews.FullView.class)
   private Boolean newsletter;

   private List<String> wishes;

   public User(final String email) {
      this.email = email;
      this.groups = new HashMap<>();
   }

   @JsonCreator
   public User(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(EMAIL) final String email,
         @JsonProperty(GROUPS) final Map<String, Set<String>> groups,
         @JsonProperty(WISHES) final List<String> wishes,
         @JsonProperty(AGREEMENT) final Boolean agreement,
         @JsonProperty(AGREEMENT_DATE) final ZonedDateTime agreementDate,
         @JsonProperty(NEWSLETTER) final Boolean newsletter) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.groups = groups;
      this.wishes = wishes;
      this.agreement = agreement;
      this.agreementDate = agreementDate;
      this.newsletter = newsletter;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(final String email) {
      this.email = email;
   }

   public String getAuthId() {
      return authId;
   }

   public void setAuthId(final String authId) {
      this.authId = authId;
   }

   public Map<String, Set<String>> getGroups() {
      return groups;
   }

   public void setGroups(final Map<String, Set<String>> groups) {
      this.groups = groups;
   }

   public DefaultWorkspace getDefaultWorkspace() {
      return defaultWorkspace;
   }

   public void setDefaultWorkspace(final DefaultWorkspace defaultWorkspace) {
      this.defaultWorkspace = defaultWorkspace;
   }

   public List<String> getWishes() {
      return wishes;
   }

   public void setWishes(final List<String> wishes) {
      this.wishes = wishes;
   }

   public Boolean hasAgreement() {
      return agreement;
   }

   public void setAgreement(final Boolean agreement) {
      this.agreement = agreement;
   }

   public ZonedDateTime getAgreementDate() {
      return agreementDate;
   }

   public void setAgreementDate(final ZonedDateTime agreementDate) {
      this.agreementDate = agreementDate;
   }

   public Boolean hasNewsletter() {
      return newsletter;
   }

   public void setNewsletter(final Boolean newsletter) {
      this.newsletter = newsletter;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final User user = (User) o;
      return Objects.equals(id, user.id);
   }

   @Override
   public int hashCode() {

      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "User{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", email='" + email + '\'' +
            ", authId='" + authId + '\'' +
            ", groups=" + groups +
            ", defaultWorkspace=" + defaultWorkspace +
            ", agreement=" + agreement +
            ", agreementDate=" + agreementDate +
            ", newsletter=" + newsletter +
            ", wishes=" + wishes +
            '}';
   }
}
