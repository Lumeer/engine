package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.Group;

public class MongoGroup {

   private String id;
   private String name;
   private String organizationId;

   public MongoGroup(final Group group, final String organizationId) {
      this.id = group.getId();
      this.name = group.getName();
      this.organizationId = organizationId;
   }

   public MongoGroup(final String id, final String name, final String organizationId) {
      this.id = id;
      this.name = name;
      this.organizationId = organizationId;
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

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public Group toGroup() {
      return new Group(getId(), getName());
   }
}
