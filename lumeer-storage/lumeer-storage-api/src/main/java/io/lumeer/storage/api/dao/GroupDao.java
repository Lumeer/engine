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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;

import java.util.List;
import java.util.Set;

public interface GroupDao extends OrganizationScopedDao {

   Group createGroup(Group group);

   Group updateGroup(String id, Group group);

   void deleteGroup(String id);

   void deleteUserFromGroups(String userId);

   void addUserToGroups(String userId, Set<String> groups);

   List<Group> getAllGroups();

   List<Group> getAllGroups(String organizationId);

   Group getGroup(String id);

   Group getGroupByName(String name);
}
