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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.SelectionList;

import java.util.List;

public interface SelectionListDao extends OrganizationScopedDao {

   void ensureIndexes(Organization organization);

   SelectionList createList(SelectionList list);

   void createLists(List<SelectionList> lists, String projectId);

   SelectionList updateList(String id, SelectionList list);

   void deleteList(SelectionList list);

   List<SelectionList> getAllLists();

   List<SelectionList> getAllLists(List<String> projectIds);

   SelectionList getList(String id);
}
