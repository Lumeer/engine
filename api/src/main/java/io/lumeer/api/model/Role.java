package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Role {

   private static final String TYPE = "type";
   private static final String TRANSITIVE = "transitive";

   private final RoleType roleType;
   private final boolean transitive;

   @JsonCreator
   public Role(@JsonProperty(TYPE) final RoleType roleType,
         @JsonProperty(TRANSITIVE) final Boolean transitive) {
      this.roleType = roleType;
      this.transitive = transitive;
   }

   public Role(final RoleType roleType) {
      this.roleType = roleType;
      this.transitive = false;
   }

   public RoleType getRoleType() {
      return roleType;
   }

   public boolean isTransitive() {
      return transitive;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Role role = (Role) o;
      return transitive == role.transitive && roleType == role.roleType;
   }

   @Override
   public int hashCode() {
      return Objects.hash(roleType, transitive);
   }

   @Override
   public String toString() {
      return "Role{" +
            "roleType=" + roleType +
            ", transitive=" + transitive +
            '}';
   }
}
