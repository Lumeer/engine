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
package io.lumeer.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Role;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class LumeerAssertions {

   public static void assertPermissions(Set<Permission> actualPermissions, Permission expectedPermission) {
      assertThat(actualPermissions).containsOnly(expectedPermission);
      assertThat(actualPermissions.stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(expectedPermission.getRoles());
   }

   public static void assertPermissions(Set<Permission> actualPermissions, Permission... expectedPermission) {
      assertThat(actualPermissions).containsOnly(expectedPermission);

      Arrays.stream(expectedPermission).forEach(p -> {
         var actual = actualPermissions.stream().filter(a -> a.equals(p)).findFirst();

         assertThat(actual.isPresent()).isTrue();

         assertThat(actual.get().getRoles()).containsExactly(p.getRoles().toArray(new Role[0]));
      });
   }

}
