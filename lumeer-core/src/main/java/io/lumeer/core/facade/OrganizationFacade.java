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

import io.lumeer.api.model.InitialUserData;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.OrganizationLoginsInfo;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectDescription;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.core.util.Utils;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.InformationStoreDao;
import io.lumeer.storage.api.dao.InitialUserDataDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceVariableDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class OrganizationFacade extends AbstractFacade {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private GroupDao groupDao;

   @Inject
   private UserDao userDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private PaymentDao paymentDao;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private SelectionListDao selectionListDao;

   @Inject
   private ResourceVariableDao resourceVariableDao;

   @Inject
   private InitialUserDataDao initialUserDataDao;

   @Inject
   private InformationStoreDao informationStoreDao;

   @Inject
   private UserLoginDao userLoginDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   public Organization createOrganization(final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());

      List<Organization> organizations = getOrganizations();
      if (organizations.size() > 0) {
         checkCreateOrganization(organizations);
      }

      Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);
      mapResourceCreationValues(organization);
      organization.setCode(checkOrGenerateCode(organization.getCode(), Collections.emptySet()));

      Organization storedOrganization = organizationDao.createOrganization(organization);

      createOrganizationInUser(storedOrganization.getId());
      createOrganizationScopedRepositories(storedOrganization);

      return storedOrganization;
   }

   private String checkOrGenerateCode(String code, java.util.Collection<String> excludeCodes) {
      Set<String> existingCodes = organizationDao.getOrganizationsCodes();
      existingCodes.removeAll(excludeCodes);
      return CodeGenerator.checkCode(existingCodes, Objects.requireNonNullElse(code, "EMPTY").toUpperCase(), 2, 6);
   }

   public boolean checkCode(String code) {
      Utils.checkCodeSafe(code.toUpperCase());
      return !organizationDao.getOrganizationsCodes().contains(code.toUpperCase());
   }

   private void checkCreateOrganization(List<Organization> organizations) {
      var hasManagedOrganization = organizations.stream().anyMatch(organization -> permissionsChecker.hasRole(organization, RoleType.Manage));
      if (hasManagedOrganization) {
         permissionsChecker.checkSystemPermission();
      }
   }

   public Organization updateOrganization(final String organizationId, final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());
      Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.Read);

      Organization updatingOrganization = storedOrganization.copy();
      updatingOrganization.patch(organization, permissionsChecker.getActualRoles(storedOrganization));
      mapResourceUpdateValues(updatingOrganization);
      updatingOrganization.setCode(checkOrGenerateCode(updatingOrganization.getCode(), Collections.singleton(storedOrganization.getCode())));

      Organization updatedOrganization = organizationDao.updateOrganization(organizationId, updatingOrganization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, updatedOrganization);

      return mapResource(updatedOrganization);
   }

   public void deleteOrganization(final String organizationId) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      if (!permissionsChecker.hasSystemPermission()) {
         permissionsChecker.checkCanDelete(organization);
      }

      deleteOrganizationScopedRepositories(organization);

      organizationDao.deleteOrganization(organization.getId());
      workspaceCache.removeOrganization(organizationId);
   }

   public Organization getOrganizationByCode(final String code) {
      final Organization organization = organizationDao.getOrganizationByCode(code);
      permissionsChecker.checkRole(organization, RoleType.Read);

      return mapResource(organization);
   }

   public Organization getOrganizationById(final String id) {
      return mapResource(checkRoleAndGetOrganization(id, RoleType.Read));
   }

   public List<Organization> getOrganizations() {
      User currentUser = authenticatedUser.getCurrentUser();
      Set<String> userOrganizations = currentUser.getOrganizations() != null ? currentUser.getOrganizations() : new HashSet<>();
      if (userOrganizations.isEmpty()) {
         return Collections.emptyList();
      }

      return organizationDao.getOrganizationsByIds(userOrganizations).stream()
                            .filter(organization -> permissionsChecker.hasRole(organization, RoleType.Read))
                            .map(this::mapResource)
                            .collect(Collectors.toList());
   }

   public Map<String, Set<String>> getUsersWithoutReadableOrganizations() {
      permissionsChecker.checkSystemPermission();

      List<Organization> organizations = organizationDao.getAllOrganizations();
      List<User> users = userDao.getAllUsers();
      Map<String, Set<String>> usersWithoutPermissions = new HashMap<>();
      for (User user : users) {
         Set<String> userOrganizations = user.getOrganizations() != null ? user.getOrganizations() : Collections.emptySet();
         List<Organization> organizationsWithoutPermissions = organizations.stream()
                                                                           .filter(organization -> !userOrganizations.contains(organization.getId()) && permissionsChecker.hasRole(organization, RoleType.Read, user.getId()))
                                                                           .collect(Collectors.toList());

         if (organizationsWithoutPermissions.size() > 0) {
            usersWithoutPermissions.put(user.getEmail(), organizationsWithoutPermissions.stream().map(Resource::getCode).collect(Collectors.toSet()));
         }
      }

      return usersWithoutPermissions;
   }

   public void repairUsersWithoutReadableOrganizations() {
      permissionsChecker.checkSystemPermission();

      List<Organization> organizations = organizationDao.getAllOrganizations();
      List<User> users = userDao.getAllUsers();
      for (User user : users) {
         Set<String> userOrganizations = user.getOrganizations() != null ? new HashSet<>(user.getOrganizations()) : new HashSet<>();
         Set<String> organizationsWithoutPermissions = organizations.stream()
                                                                    .filter(organization -> !userOrganizations.contains(organization.getId()) && permissionsChecker.hasRole(organization, RoleType.Read, user.getId()))
                                                                    .map(Organization::getId)
                                                                    .collect(Collectors.toSet());

         if (organizationsWithoutPermissions.size() > 0) {
            userOrganizations.addAll(organizationsWithoutPermissions);
            user.setOrganizations(userOrganizations);
            userDao.updateUser(user.getId(), user);
         }
      }
   }

   private Organization checkRoleAndGetOrganization(final String organizationId, final RoleType role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   public Permissions getOrganizationPermissions(final String organizationId) {
      return mapResource(checkRoleAndGetOrganization(organizationId, RoleType.UserConfig)).getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String organizationId, final Set<Permission> userPermissions) {
      return updateUserPermissions(organizationId, userPermissions, true);
   }

   public Set<Permission> addUserPermissions(final String organizationId, final Set<Permission> userPermissions) {
      return updateUserPermissions(organizationId, userPermissions, false);
   }

   public Set<Permission> updateUserPermissions(final String organizationId, final Set<Permission> userPermissions, boolean update) {
      Organization organization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);

      final Organization originalOrganization = organization.copy();
      if (update) {
         organization.getPermissions().updateUserPermissions(userPermissions);
      } else {
         organization.getPermissions().addUserPermissions(userPermissions);
      }
      mapResourceUpdateValues(organization);

      organizationDao.updateOrganization(organization.getId(), organization, originalOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String organizationId, final String userId) {
      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      projectDao.getAllProjects().forEach(project -> {
         final Project originalProject = project.copy();
         project.getPermissions().removeUserPermission(userId);
         final Project updatedProject = projectDao.updateProject(project.getId(), project, originalProject);
         workspaceCache.updateProject(project.getId(), updatedProject);
      });

      organization.getPermissions().removeUserPermission(userId);
      mapResourceUpdateValues(organization);

      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   public Set<Permission> updateGroupPermissions(final String organizationId, final Set<Permission> groupPermissions) {
      permissionsChecker.checkGroupsHandle();

      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      organization.getPermissions().updateGroupPermissions(groupPermissions);
      mapResourceUpdateValues(organization);

      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String organizationId, final String groupId) {
      permissionsChecker.checkGroupsHandle();

      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      projectDao.getAllProjects().forEach(project -> {
         final Project originalProject = project.copy();
         project.getPermissions().removeGroupPermission(groupId);
         final Project updatedProject = projectDao.updateProject(project.getId(), project, originalProject);
         workspaceCache.updateProject(project.getId(), updatedProject);
      });

      organization.getPermissions().removeGroupPermission(groupId);
      mapResourceUpdateValues(organization);

      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   public List<Organization> getOrganizationsCapableForProject(final ProjectDescription projectDescription) {
      return getOrganizations().stream().filter(org -> permissionsChecker.hasRole(org, RoleType.ProjectContribute)
      ).filter(org -> {
         final ServiceLimits serviceLimits = paymentFacade.getCurrentServiceLimits(org);
         final long projects = projectDao.getProjectsCount(org);

         if (projects >= serviceLimits.getProjects()) {
            return false;
         }

         if (projectDescription == null) {
            return true;
         }

         return serviceLimits.fitsLimits(projectDescription);
      }).collect(Collectors.toList());
   }

   public InitialUserData getInitialUserData() {
      permissionsChecker.checkSystemPermission();

      return initialUserDataDao.get().stream().filter(datum -> datum.getProjectId() == null).findFirst().orElse(null);
   }

   public InitialUserData setInitialUserData(InitialUserData data) {
      permissionsChecker.checkSystemPermission();

      return initialUserDataDao.upsert(data);
   }

   public int deleteOldOrganizations(int months) {
      permissionsChecker.checkSystemPermission();

      Map<String, Organization> organizationsMap = organizationDao.getAllOrganizations().stream()
                                                                  .collect(Collectors.toMap(Organization::getId, o -> o));

      List<OrganizationLoginsInfo> loginsInfos = getOrganizationsLoginsInfoToDelete(false, months);
      loginsInfos.forEach(info -> {
         Organization organization = organizationsMap.get(info.getOrganizationId());
         if (organization != null) {
            deleteOrganizationScopedRepositories(organization);

            organizationDao.deleteOrganization(organization.getId());
            workspaceCache.removeOrganization(organization.getId());
         }
      });
      return loginsInfos.size();
   }

   public List<OrganizationLoginsInfo> getOrganizationsLoginsInfoToDelete(boolean descending, int months) {
      permissionsChecker.checkSystemPermission();

      int safeMonths = Math.max(months, 9);
      List<String> whitelistedDomains = configurationProducer.getArray(DefaultConfigurationProducer.WHITELIST_USER_DOMAINS);
      List<String> whitelistedEmails = configurationProducer.getArray(DefaultConfigurationProducer.WHITELIST_USER_EMAILS);
      return getOrganizationsLoginsInfo(descending)
            .stream()
            .filter(info -> isLastLoginOlderThanMonths(info, safeMonths) && !containsWhitelistedUsers(info, whitelistedDomains, whitelistedEmails))
            .collect(Collectors.toList());
   }

   public boolean isLastLoginOlderThanMonths(OrganizationLoginsInfo info, int months) {
      if (info.getLastLoginDate() == null) {
         return true;
      }
      return info.getLastLoginDate().plusMonths(months).isBefore(ZonedDateTime.now());
   }

   public boolean containsWhitelistedUsers(OrganizationLoginsInfo info, List<String> whitelistedDomains, List<String> whitelistedEmails) {
      return info.getUserEmails().stream().anyMatch(email -> {
         if (whitelistedEmails.contains(email)) {
            return true;
         }
         var emailDomain = email.substring(email.indexOf("@") + 1);
         return !emailDomain.isEmpty() && whitelistedDomains.contains(emailDomain);
      });
   }

   public List<OrganizationLoginsInfo> getOrganizationsLoginsInfo(boolean descending) {
      permissionsChecker.checkSystemPermission();

      List<User> users = userDao.getAllUsers();
      Map<String, ZonedDateTime> usersLastLogins = userLoginDao.getUsersLastLogins();

      Comparator<OrganizationLoginsInfo> comparator = Comparator.comparing(OrganizationLoginsInfo::getLastLoginDate, Comparator.nullsFirst(Comparator.naturalOrder()));

      return organizationDao.getAllOrganizations().stream().map(organization -> {
                               List<User> usersByOrganization = users.stream()
                                                                     .filter(user -> permissionsChecker.hasRole(organization, RoleType.Read, user.getId()))
                                                                     .collect(Collectors.toList());

                               Set<String> usersIds = usersByOrganization.stream().map(User::getId).collect(Collectors.toSet());
                               Set<String> usersEmails = usersByOrganization.stream().map(User::getEmail).collect(Collectors.toSet());

                               List<ZonedDateTime> lastLogins = usersLastLogins.entrySet().stream()
                                                                               .filter(entry -> usersIds.contains(entry.getKey()))
                                                                               .map(Map.Entry::getValue)
                                                                               .filter(Objects::nonNull)
                                                                               .sorted()
                                                                               .collect(Collectors.toList());
                               ZonedDateTime lastLogin = lastLogins.size() > 0 ? lastLogins.get(lastLogins.size() - 1) : null;

                               workspaceKeeper.setOrganization(organization);
                               projectDao.setOrganization(organization);

                               Set<String> projectsCodes = projectDao.getProjectsCodes();

                               return new OrganizationLoginsInfo(organization.getId(), organization.getCode(), projectsCodes, usersEmails, lastLogin);
                            })
                            .sorted(descending ? comparator.reversed() : comparator)
                            .collect(Collectors.toList());
   }

   private void createOrganizationInUser(final String organizationId) {
      User currentUser = authenticatedUser.getCurrentUser();

      Set<String> groups = currentUser.getOrganizations() != null ? new HashSet<>(currentUser.getOrganizations()) : new HashSet<>();
      groups.add(organizationId);
      currentUser.setOrganizations(groups);

      userDao.updateUser(currentUser.getId(), currentUser);
   }

   private void createOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.createRepository(organization);
      groupDao.createRepository(organization);
      paymentDao.createRepository(organization);
      favoriteItemDao.createRepository(organization);
      selectionListDao.createRepository(organization);
      resourceVariableDao.createRepository(organization);
      initialUserDataDao.createRepository(organization);
      informationStoreDao.createRepository(organization);
   }

   private void deleteOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);

      workspaceKeeper.setOrganization(organization);
      projectDao.getAllProjects().forEach(project -> projectFacade.deleteProjectScopedRepositories(project));

      projectDao.deleteRepository(organization);
      groupDao.deleteRepository(organization);
      paymentDao.deleteRepository(organization);
      favoriteItemDao.deleteRepository(organization);
      selectionListDao.deleteRepository(organization);
      resourceVariableDao.deleteRepository(organization);
      initialUserDataDao.deleteRepository(organization);
      informationStoreDao.deleteRepository(organization);

      userCache.clear();

      delayedActionDao.deleteAllScheduledActions(organization.getId());
   }
}
