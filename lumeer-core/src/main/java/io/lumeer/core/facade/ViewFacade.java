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

import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.View;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.adapter.ViewAdapter;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DefaultViewConfigDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ViewFacade extends AbstractFacade {

   @Inject
   private ViewDao viewDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private UserDao userDao;

   @Inject
   private DefaultViewConfigDao defaultViewConfigDao;

   private ViewAdapter adapter;
   private ResourceAdapter resourceAdapter;

   @PostConstruct
   public void init() {
      resourceAdapter = new ResourceAdapter(permissionsChecker.getPermissionAdapter(), collectionDao, linkTypeDao, viewDao, userDao);
      adapter = new ViewAdapter(resourceAdapter, favoriteItemDao);
   }

   public ViewAdapter getAdapter() {
      return adapter;
   }

   public View createView(View view) {
      permissionsChecker.checkRole(getProject(), RoleType.ViewContribute);
      checkViewCollectionIds(view);
      view.setAuthorId(getCurrentUserId());
      view.setLastTimeUsed(ZonedDateTime.now());
      view.setId(null);

      if (view.getCode() == null || view.getCode().isEmpty()) {
         view.setCode(this.generateViewCode(view.getName()));
      }

      Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), View.ROLES);
      view.getPermissions().updateUserPermissions(defaultUserPermission);

      return mapView(viewDao.createView(view));
   }

   private void checkViewCollectionIds(View view) {
      List<LinkType> linkTypes = Collections.emptyList();
      if (!view.getQuery().getLinkTypeIds().isEmpty()) {
         linkTypes = linkTypeDao.getLinkTypesByIds(view.getQuery().getLinkTypeIds());
         linkTypes.forEach(linkType -> permissionsChecker.checkRoleInLinkType(linkType, RoleType.Read, getCurrentUserId()));
      }

      final Set<String> queryCollectionIds = QueryUtils.getViewCollectionIds(view, linkTypes);
      if (!queryCollectionIds.isEmpty()) {
         collectionDao.getCollectionsByIds(queryCollectionIds).forEach(collection ->
               permissionsChecker.checkRole(collection, RoleType.Read));
      }
   }

   public View updateView(final String id, final View view) {
      View storedView = viewDao.getViewById(id);
      permissionsChecker.checkRole(storedView, RoleType.Read);
      if (permissionsChecker.hasRole(storedView, RoleType.QueryConfig)) {
         checkViewCollectionIds(view);
      }

      View updatingView = storedView.copy();
      updatingView.patch(view, permissionsChecker.getActualRoles(storedView));
      updatingView.setAuthorId(storedView.getAuthorId());
      updatingView.setLastTimeUsed(ZonedDateTime.now());

      return mapView(viewDao.updateView(id, updatingView));
   }

   private View mapView(View view) {
      return adapter.mapViewData(getOrganization(), getProject(), mapResource(view), getCurrentUserId(), selectedWorkspace.getProjectId());
   }

   public void deleteView(final String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkCanDelete(view);

      viewDao.deleteView(view.getId());

      favoriteItemDao.removeFavoriteViewFromUsers(getProject().getId(), id);
   }

   public View getViewById(final String id) {
      final View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.Read);

      return mapView(view);
   }

   public List<View> getViewsPublic() {
      if (permissionsChecker.isPublic()) {
         return viewDao.getAllViews();
      }

      return List.of();
   }

   private List<View> mapViews(List<View> views) {
      return adapter.mapViewsData(getOrganization(), getProject(), views, getCurrentUserId(), selectedWorkspace.getProjectId());
   }

   public List<View> getViews() {
      return mapViews(resourceAdapter.getViews(getOrganization(), getProject(), getCurrentUserId())
                                     .stream()
                                     .map(this::mapResource)
                                     .collect(Collectors.toList())
      );
   }

   public Permissions getViewPermissions(final String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      return view.getPermissions();
   }

   public void addFavoriteView(String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.Read);

      String projectId = getProject().getId();
      String userId = getCurrentUserId();
      favoriteItemDao.addFavoriteView(userId, projectId, id);
   }

   public void removeFavoriteView(String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.Read);

      String userId = getCurrentUserId();
      favoriteItemDao.removeFavoriteView(userId, id);
   }

   public Permissions updatePermissions(final String id, final Permissions permissions) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      final View originalView = view.copy();
      view.getPermissions().clearUserPermissions();
      view.getPermissions().clearGroupPermissions();
      view.getPermissions().updateUserPermissions(permissions.getUserPermissions());
      view.getPermissions().updateGroupPermissions(permissions.getGroupPermissions());
      viewDao.updateView(view.getId(), view, originalView);

      return view.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String id, final Set<Permission> userPermissions) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      final View originalView = view.copy();
      view.getPermissions().clearUserPermissions();
      view.getPermissions().updateUserPermissions(userPermissions);
      viewDao.updateView(view.getId(), view, originalView);

      return view.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String id, final String userId) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      view.getPermissions().removeUserPermission(userId);
      viewDao.updateView(view.getId(), view);
   }

   public Set<Permission> updateGroupPermissions(final String id, final Set<Permission> groupPermissions) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      view.getPermissions().clearGroupPermissions();
      view.getPermissions().updateGroupPermissions(groupPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String id, final String groupId) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, RoleType.UserConfig);

      view.getPermissions().removeGroupPermission(groupId);
      viewDao.updateView(view.getId(), view);
   }

   private String generateViewCode(String viewName) {
      Set<String> existingCodes = viewDao.getAllViewCodes();
      return CodeGenerator.generate(existingCodes, viewName);
   }

   public List<DefaultViewConfig> getDefaultConfigs() {
      return defaultViewConfigDao.getConfigs(getCurrentUserId());
   }

   public DefaultViewConfig updateDefaultConfig(DefaultViewConfig config) {
      config.setUserId(getCurrentUserId());

      return defaultViewConfigDao.updateConfig(config);
   }
}
