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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.adapter.ViewAdapter;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DefaultViewConfigDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DefaultViewConfigDao defaultViewConfigDao;

   private ViewAdapter adapter;
   private LinkTypeAdapter linkTypeAdapter;
   private CollectionAdapter collectionAdapter;

   @PostConstruct
   public void init() {
      adapter = new ViewAdapter(viewDao, linkTypeDao, favoriteItemDao);
      linkTypeAdapter = new LinkTypeAdapter(linkInstanceDao);
      collectionAdapter = new CollectionAdapter(favoriteItemDao, documentDao);
   }

   public ViewAdapter getAdapter() {
      return adapter;
   }

   public View createView(View view) {
      if (view.getQuery().getCollectionIds() != null) {
         collectionDao.getCollectionsByIds(view.getQuery().getCollectionIds()).forEach(collection ->
               permissionsChecker.checkRole(collection, Role.READ));
      }
      view.setAuthorId(authenticatedUser.getCurrentUserId());
      view.setLastTimeUsed(ZonedDateTime.now());
      view.setId(null);

      if (view.getCode() == null || view.getCode().isEmpty()) {
         view.setCode(this.generateViewCode(view.getName()));
      }

      Permission defaultUserPermission = Permission.buildWithRoles(authenticatedUser.getCurrentUserId(), View.ROLES);
      view.getPermissions().updateUserPermissions(defaultUserPermission);
      view.setAuthorRights(getViewAuthorRights(view));

      return viewDao.createView(view);
   }

   public View updateView(final String id, final View view) {
      View storedView = viewDao.getViewById(id);
      permissionsChecker.checkRole(storedView, Role.MANAGE);

      keepStoredPermissions(view, storedView.getPermissions());
      view.setAuthorId(storedView.getAuthorId());
      view.setLastTimeUsed(ZonedDateTime.now());

      View updatedView = viewDao.updateView(storedView.getId(), view);
      updatedView.setAuthorRights(getViewAuthorRights(updatedView));

      return mapView(updatedView);
   }

   private View mapView(View view) {
      return adapter.mapViewData(mapResource(view), authenticatedUser.getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public void deleteView(final String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);

      viewDao.deleteView(view.getId());

      favoriteItemDao.removeFavoriteViewFromUsers(getCurrentProject().getId(), id);
   }

   public View getViewById(final String id) {
      final View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.READ);

      checkAuthorId(view);
      view.setAuthorRights(getViewAuthorRights(view));

      return mapView(view);
   }

   private View checkAuthorId(final View view) {
      if (view.getAuthorId() == null || "".equals(view.getAuthorId())) {
         if (permissionsChecker.hasRole(view, Role.MANAGE)) {
            view.setAuthorId(authenticatedUser.getCurrentUserId());
            return viewDao.updateView(view.getId(), view);
         }
      }

      return view;
   }

   public List<View> getViewsPublic() {
      if (permissionsChecker.isPublic()) {
         return viewDao.getAllViews();
      }

      return List.of();
   }

   private List<View> mapViews(List<View> views) {
      return adapter.mapViewsData(views, authenticatedUser.getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public List<View> getViews() {
      if (permissionsChecker.isManager()) {
         return mapViews(viewDao.getAllViews());
      }

      checkProjectRole(Role.READ);
      return getViews(createSimpleQuery());
   }

   private List<View> getViews(DatabaseQuery databaseQuery) {
      return mapViews(viewDao.getViews(databaseQuery).stream()
                    .filter(view -> permissionsChecker.hasRole(view, Role.READ))
                    .map(this::checkAuthorId)
                    .peek(view -> view.setAuthorRights(getViewAuthorRights(view)))
                    .map(this::mapResource)
                    .collect(Collectors.toList()));
   }

   public Permissions getViewPermissions(final String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);

      return view.getPermissions();
   }

   public void addFavoriteView(String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.READ);

      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();
      favoriteItemDao.addFavoriteView(userId, projectId, id);
   }

   public void removeFavoriteView(String id) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.READ);

      String userId = getCurrentUser().getId();
      favoriteItemDao.removeFavoriteView(userId, id);
   }

   public boolean isFavorite(String id) {
      return isFavorite(id, getCurrentUser().getId());
   }

   public boolean isFavorite(String id, String userId) {
      return getFavoriteViewsIds(userId).contains(id);
   }

   public Set<String> getFavoriteViewsIds() {
      return getFavoriteViewsIds(getCurrentUser().getId());
   }

   public Set<String> getFavoriteViewsIds(String userId) {
      String projectId = getCurrentProject().getId();

      return adapter.getFavoriteViewIds(userId, projectId);
   }

   public Set<Permission> updateUserPermissions(final String id, final Set<Permission> userPermissions) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      final View originalView = view.copy();
      view.getPermissions().clearUserPermissions();
      view.getPermissions().updateUserPermissions(userPermissions);
      viewDao.updateView(view.getId(), view, originalView);

      return view.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String id, final String userId) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().removeUserPermission(userId);
      viewDao.updateView(view.getId(), view);
   }

   public Set<Permission> updateGroupPermissions(final String id, final Set<Permission> groupPermissions) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().clearGroupPermissions();
      view.getPermissions().updateGroupPermissions(groupPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String id, final String groupId) {
      View view = viewDao.getViewById(id);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().removeGroupPermission(groupId);
      viewDao.updateView(view.getId(), view);
   }

   private String generateViewCode(String viewName) {
      Set<String> existingCodes = viewDao.getAllViewCodes();
      return CodeGenerator.generate(existingCodes, viewName);
   }

   public List<Collection> getViewsCollections() {
      return getViewsCollections(getViews(), true);
   }

   private List<Collection> getViewsCollections(List<View> views, boolean keepActualRoles) {
      Set<String> collectionIds = QueryUtils.getViewsCollectionIds(views, getViewsLinkTypes(views));

      return collectionAdapter.mapCollectionsData(collectionDao.getCollectionsByIds(collectionIds).stream()
                          .map(collection -> keepActualRoles ? mapResource(collection) : collection)
                          .collect(Collectors.toList()), authenticatedUser.getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public List<LinkType> getViewsLinkTypes() {
      return getViewsLinkTypes(getViews());
   }

   private List<LinkType> getViewsLinkTypes(List<View> views) {
      Set<String> linkTypesIds = views.stream().map(view -> view.getQuery().getLinkTypeIds())
                                      .flatMap(java.util.Collection::stream)
                                      .collect(Collectors.toSet());
      return linkTypeAdapter.mapLinkTypesData(linkTypeDao.getLinkTypesByIds(linkTypesIds));
   }

   public Map<String, Set<Role>> getViewAuthorRights(final View view) {
      return getCollectionsByView(view).stream()
                                       .collect(Collectors.toMap(Resource::getId, c -> permissionsChecker.getActualRoles(c, view.getAuthorId())));
   }

   public List<DefaultViewConfig> getDefaultConfigs() {
      return defaultViewConfigDao.getConfigs(authenticatedUser.getCurrentUserId());
   }

   public DefaultViewConfig updateDefaultConfig(DefaultViewConfig config) {
      var userId = authenticatedUser.getCurrentUserId();
      config.setUserId(userId);

      return defaultViewConfigDao.updateConfig(config);
   }

   private List<Collection> getCollectionsByView(final View view) {
      return getViewsCollections(Collections.singletonList(view), false);
   }

   private void checkProjectRole(Role role) {
      Project project = getCurrentProject();
      permissionsChecker.checkRole(project, role);
   }

   private Project getCurrentProject() {
      if (workspaceKeeper.getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return workspaceKeeper.getProject().get();
   }

   private User getCurrentUser() {
      return authenticatedUser.getCurrentUser();
   }

   public View mapViewData(final View view, final String userId) {
      view.setFavorite(isFavorite(view.getId(), userId));
      return view;
   }
}
