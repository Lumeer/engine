/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SearchQuery;

@RequestScoped
public class ViewFacade extends AbstractFacade {

   @Inject
   private ViewDao viewDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private LinkTypeDao linkTypeDao;

   public View createView(View view) {
      if (view.getQuery().getCollectionIds() != null) {
         collectionDao.getCollectionsByIds(view.getQuery().getCollectionIds()).forEach(collection ->
               permissionsChecker.checkRole(collection, Role.READ));
      }
      view.setAuthorId(authenticatedUser.getCurrentUserId());

      if (view.getCode() == null || view.getCode().isEmpty()) {
         view.setCode(this.generateViewCode(view.getName()));
      }

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUserId(), View.ROLES);
      view.getPermissions().updateUserPermissions(defaultUserPermission);

      return viewDao.createView(view);
   }

   public View updateView(final String code, final View view) {
      View storedView = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(storedView, Role.MANAGE);

      keepStoredPermissions(view, storedView.getPermissions());
      View updatedView = viewDao.updateView(storedView.getId(), view);

      return mapResource(updatedView);
   }

   public void deleteView(final String code) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      viewDao.deleteView(view.getId());
   }

   public View getViewByCode(final String code) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.READ);

      return mapResource(view);
   }

   public List<View> getViews() {
      return getViews(createQuery());
   }

   public List<View> getViews(Pagination pagination) {
      return getViews(createPaginationQuery(pagination));
   }

   private List<View> getViews(SearchQuery searchQuery) {
      return viewDao.getViews(searchQuery).stream()
                    .filter(view -> permissionsChecker.hasRole(view, Role.READ))
                    .map(this::mapResource)
                    .collect(Collectors.toList());
   }

   public Permissions getViewPermissions(final String code) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      return view.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String code, final Permission... userPermissions) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().clearUserPermissions();
      view.getPermissions().updateUserPermissions(userPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String code, final String userId) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().removeUserPermission(userId);
      viewDao.updateView(view.getId(), view);
   }

   public Set<Permission> updateGroupPermissions(final String code, final Permission... groupPermissions) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);
      permissionsChecker.invalidateCache(view);

      view.getPermissions().clearGroupPermissions();
      view.getPermissions().updateGroupPermissions(groupPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String code, final String groupId) {
      View view = viewDao.getViewByCode(code);
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
      final Set<Collection> collections = new HashSet<>();

      getViews().forEach(view -> {
         collections.addAll(searchFacade.searchCollectionsByView(view));
      });

      return new ArrayList<>(collections);
   }

   public List<LinkType> getViewsLinkTypes() {
      final Set<LinkType> linkTypes = new HashSet<>();

      getViews().forEach(view ->
            view.getQuery().getLinkTypeIds().stream()
                .map(linkTypeDao::getLinkType)
                .filter(linkType -> linkType.getCollectionIds().stream()
                     .allMatch(collectionId ->
                           permissionsChecker.hasRoleWithView(
                                 collectionDao.getCollectionById(collectionId), Role.READ, Role.READ, view.getCode()))
                ).forEach(linkTypes::add));

      return new ArrayList<>(linkTypes);
   }
}