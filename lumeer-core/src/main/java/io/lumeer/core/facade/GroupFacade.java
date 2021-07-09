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
package io.lumeer.core.facade;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.InvitationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.RoleUtils;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class GroupFacade extends AbstractFacade {

   @Inject
   private GroupDao groupDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   public Group createGroup(Group group) {
      checkPermissions(RoleType.UserConfig);
      checkGroupName(group.getName());

      group.setId(null);
      if (group.getUsers() == null) {
         group.setUsers(new ArrayList<>());
      }
      group.setUsers(new ArrayList<>(new HashSet<>(group.getUsers())));
      return mapGroupData(groupDao.createGroup(group));
   }

   public Group updateGroup(String groupId, Group group) {
      checkPermissions(RoleType.UserConfig);
      Group storedGroup = groupDao.getGroup(groupId);
      if (!storedGroup.getName().equals(group.getName())) {
         checkGroupName(group.getName());
      }

      group.setId(groupId);
      if (group.getUsers() == null) {
         group.setUsers(new ArrayList<>());
      }
      group.setUsers(new ArrayList<>(new HashSet<>(group.getUsers())));

      permissionsChecker.getPermissionAdapter().invalidateUserCache();
      return mapGroupData(groupDao.updateGroup(groupId, group));
   }

   public List<Group> addGroupsToWorkspace(final String organizationId, final String projectId, final List<Group> groups, final InvitationType invitationType) {
      // we need at least project management rights
      checkProjectPermissions(organizationId, projectId);

      Organization organization = organizationFacade.getOrganizationById(organizationId);

      addGroupsToOrganization(organization, groups);
      addGroupsToProject(organization, projectId, groups, invitationType);

      return groups;
   }

   private void addGroupsToOrganization(Organization organization, List<Group> groups) {
      var newPermissions = buildGroupPermission(organization, groups, InvitationType.JOIN_ONLY);
      organizationFacade.updateUserPermissions(organization.getId(), newPermissions);
   }

   private void addGroupsToProject(Organization organization, final String projectId, final List<Group> groups, final InvitationType invitationType) {
      workspaceKeeper.setOrganizationId(organization.getId());
      var project = projectDao.getProjectById(projectId);
      var newPermissions = buildGroupPermission(project, groups, invitationType);
      projectFacade.updateUserPermissions(projectId, newPermissions);
   }

   private Set<Permission> buildGroupPermission(final Resource resource, final List<Group> groups, final InvitationType invitationType) {
      return groups.stream()
                   .map(group -> {
                      var existingPermissions = resource.getPermissions().getGroupPermissions().stream().filter(permission -> permission.getId().equals(group.getId())).findFirst();
                      var minimalSet = new HashSet<>(Set.of(new Role(RoleType.Read)));
                      existingPermissions.ifPresent(permission -> minimalSet.addAll(permission.getRoles()));
                      return Permission.buildWithRoles(group.getId(), RoleUtils.getInvitationRoles(invitationType, resource.getType(), minimalSet));
                   })
                   .collect(Collectors.toSet());
   }

   private void checkGroupName(String name) {
      if (groupDao.getGroupByName(name) != null) {
         throw new IllegalArgumentException("Group with name " + name + " already exists");
      }
   }

   public void deleteGroup(String groupId) {
      checkPermissions(RoleType.UserConfig);

      groupDao.deleteGroup(groupId);
   }

   public List<Group> getGroups() {
      checkPermissions(RoleType.Read);

      return groupDao.getAllGroups().stream().peek(this::mapGroupData).collect(Collectors.toList());
   }

   private Group mapGroupData(Group group) {
      group.setOrganizationId(workspaceKeeper.getOrganizationId());
      return group;
   }

   private Organization checkPermissions(RoleType roleType) {
      permissionsChecker.checkGroupsHandle();

      if (workspaceKeeper.getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, roleType);
      return organization;
   }

   private Project checkProjectPermissions(final String organizationId, final String projectId) {
      permissionsChecker.checkGroupsHandle();

      workspaceKeeper.setOrganizationId(organizationId);
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, RoleType.UserConfig);

      return project;
   }

}
