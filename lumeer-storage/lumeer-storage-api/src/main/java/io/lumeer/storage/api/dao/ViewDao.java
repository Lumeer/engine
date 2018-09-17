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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;

import java.util.List;
import java.util.Set;

public interface ViewDao {

   void createViewsRepository(Project project);

   void deleteViewsRepository(Project project);

   View createView(View view);

   View updateView(String id, View view);

   void deleteView(String id);

   View getViewByCode(String code);

   View getViewById(String id);

   List<View> getViews(SearchQuery query);

   List<View> getViews(SuggestionQuery query);

   void setProject(final Project project);

   Set<String> getAllViewCodes();

}
