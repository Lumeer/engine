package io.lumeer.api.util;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleOld;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleUtils {

   private RoleUtils() {
   }

   public static Set<Role> getAllResourceRoles(final Resource resource) {
      if (resource instanceof Organization) {
         return organizationResourceRoles();
      } else if (resource instanceof Project) {
         return projectResourceRoles();
      } else if (resource instanceof Collection) {
         return collectionResourceRoles();
      } else if (resource instanceof View) {
         return viewResourceRoles();
      }
      return Collections.emptySet();
   }

   public static Set<Role> resourceRoles(RoleOld role, ResourceType resourceType) {
      switch (resourceType) {
         case ORGANIZATION:
            return organizationResourceRoles(role);
         case PROJECT:
            return projectResourceRoles(role);
         case COLLECTION:
            return collectionResourceRoles(role);
         case VIEW:
            return viewResourceRoles(role);
         default:
            return Collections.emptySet();
      }
   }

   public static Set<Role> organizationResourceRoles() {
      return Arrays.stream(RoleOld.values()).map(RoleUtils::organizationResourceRoles).flatMap(Set::stream).collect(Collectors.toSet());
   }

   public static Set<Role> organizationResourceRoles(RoleOld role) {
      switch (role) {
         case READ:
            return Set.of(new Role(RoleType.Read));
         case WRITE:
            return Set.of(new Role(RoleType.Contribute));
         case MANAGE:
            return Arrays.stream(RoleType.values()).map(roleType -> new Role(roleType, true)).collect(Collectors.toSet());
         default:
            return Collections.emptySet();
      }
   }

   public static Set<Role> projectResourceRoles() {
      return Arrays.stream(RoleOld.values()).map(RoleUtils::projectResourceRoles).flatMap(Set::stream).collect(Collectors.toSet());
   }

   public static Set<Role> projectResourceRoles(RoleOld role) {
      switch (role) {
         case READ:
            return Set.of(new Role(RoleType.Read));
         case WRITE:
            return Set.of(new Role(RoleType.CollectionContribute), new Role(RoleType.ViewContribute), new Role(RoleType.LinkContribute));
         case MANAGE:
            return Arrays.stream(RoleType.values()).map(roleType -> new Role(roleType, true)).collect(Collectors.toSet());
         default:
            return Collections.emptySet();
      }
   }

   public static Set<Role> collectionResourceRoles() {
      return Arrays.stream(RoleOld.values()).map(RoleUtils::collectionResourceRoles).flatMap(Set::stream).collect(Collectors.toSet());
   }

   public static Set<Role> collectionResourceRoles(RoleOld role) {
      switch (role) {
         case READ:
            return Set.of(new Role(RoleType.Read, true), new Role(RoleType.CommentContribute));
         case WRITE:
            return Set.of(new Role(RoleType.Write), new Role(RoleType.Contribute), new Role(RoleType.Delete));
         case MANAGE:
            return Set.of(new Role(RoleType.Read, true), new Role(RoleType.CommentContribute), new Role(RoleType.Write), new Role(RoleType.Contribute), new Role(RoleType.Delete), new Role(RoleType.Config), new Role(RoleType.AttributeEdit), new Role(RoleType.UserConfig), new Role(RoleType.TechConfig));
         default:
            return Collections.emptySet();
      }
   }

   public static Set<Role> viewResourceRoles() {
      return Arrays.stream(RoleOld.values()).map(RoleUtils::viewResourceRoles).flatMap(Set::stream).collect(Collectors.toSet());
   }

   public static Set<Role> viewResourceRoles(RoleOld role) {
      switch (role) {
         case READ:
         case WRITE:
            return collectionResourceRoles(role);
         case MANAGE:
            return Set.of(new Role(RoleType.Read, true), new Role(RoleType.CommentContribute), new Role(RoleType.Write), new Role(RoleType.Contribute), new Role(RoleType.Delete), new Role(RoleType.Config), new Role(RoleType.AttributeEdit), new Role(RoleType.UserConfig), new Role(RoleType.QueryConfig), new Role(RoleType.PerspectiveConfig));
         default:
            return Collections.emptySet();
      }
   }
}
