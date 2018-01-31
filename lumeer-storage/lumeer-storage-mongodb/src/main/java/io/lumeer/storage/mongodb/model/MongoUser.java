package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class MongoUser {

   private String id;
   private String name;
   private String email;
   private String keycloakId;
   private Map<String, Set<String>> groups;

   public MongoUser(final User user, final String organizationId, final String keycloakId){
      this.id = user.getId();
      this.name = user.getName();
      this.email = user.getEmail();
      this.keycloakId = keycloakId;
      this.groups = Collections.singletonMap(organizationId, user.getGroups());
   }

   public MongoUser(final String id, final String name, final String email, final String keycloakId, final Map<String, Set<String>> groups) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.keycloakId = keycloakId;
      this.groups = groups;
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

   public String getKeycloakId() {
      return keycloakId;
   }

   public void setKeycloakId(final String keycloakId) {
      this.keycloakId = keycloakId;
   }

   public Map<String, Set<String>> getGroups() {
      return groups;
   }

   public void setGroups(final Map<String, Set<String>> groups) {
      this.groups = groups;
   }

   public User toUser(final String organizationId){
      Set<String> groups = getGroups().getOrDefault(organizationId, Collections.emptySet());
      return new User(getId(), getName(), getEmail(), groups);
   }
}
