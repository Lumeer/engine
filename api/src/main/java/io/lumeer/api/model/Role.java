package io.lumeer.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Role {

   private static final String TYPE = "type";
   private static final String TRANSITIVE = "transitive";

   private final RoleType type;
   private final boolean transitive;

   @JsonCreator
   public Role(@JsonProperty(TYPE) final RoleType type,
         @JsonProperty(TRANSITIVE) final boolean transitive) {
      this.type = type;
      this.transitive = transitive;
   }

   public Role(final RoleType type) {
      this.type = type;
      this.transitive = false;
   }

   public RoleType getType() {
      return type;
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
      return transitive == role.transitive && type == role.type;
   }

   @Override
   public int hashCode() {
      return Objects.hash(type, transitive);
   }

   @Override
   public String toString() {
      return "Role{" +
            "type=" + type +
            ", transitive=" + transitive +
            '}';
   }
}
