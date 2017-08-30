/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.core.facade;

import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ViewFacade extends AbstractFacade {

   @Inject
   private ViewDao viewDao;

   public View createView(View view) {
      // TODO check collection permissions

      // TODO generate view code if not provided

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUsername(), View.ROLES);
      view.getPermissions().updateUserPermissions(defaultUserPermission);

      return viewDao.createView(view);
   }

   public View updateView(final String code, final View view) {
      View storedView = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(storedView, Role.MANAGE);

      keepStoredPermissions(view, storedView.getPermissions());
      View updatedView = viewDao.updateView(storedView.getId(), view);

      return keepOnlyActualUserRoles(updatedView);
   }

   public void deleteView(final String code) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      viewDao.deleteView(view.getId());
   }

   public View getViewByCode(final String code) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.READ);

      return keepOnlyActualUserRoles(view);
   }

   public List<View> getViews(Pagination pagination) {
      SearchQuery searchQuery = createPaginationQuery(pagination);

      return viewDao.getViews(searchQuery).stream()
                    .map(this::keepOnlyActualUserRoles)
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

      view.getPermissions().updateUserPermissions(userPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String code, final String user) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      view.getPermissions().removeUserPermission(user);
      viewDao.updateView(view.getId(), view);
   }

   public Set<Permission> updateGroupPermissions(final String code, final Permission... groupPermissions) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      view.getPermissions().updateGroupPermissions(groupPermissions);
      viewDao.updateView(view.getId(), view);

      return view.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String code, final String group) {
      View view = viewDao.getViewByCode(code);
      permissionsChecker.checkRole(view, Role.MANAGE);

      view.getPermissions().removeGroupPermission(group);
      viewDao.updateView(view.getId(), view);
   }
}
