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

package io.lumeer.remote.rest;

import io.lumeer.api.model.LinkType;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.List;
import javax.ws.rs.Path;

//@Path("organizations/{organizationCode}/projects/{projectCode}/link-types")
public class LinkTypeService extends AbstractService {

   public LinkType createLinkType(LinkType linkType) {
      throw new UnsupportedOperationException();
   }

   public LinkType updateLinkType(String id, LinkType linkType) {
      throw new UnsupportedOperationException();
   }

   public void deleteLinkType(String id) {
      throw new UnsupportedOperationException();
   }

   public List<LinkType> getLinkTypes(SearchQuery query) {
      throw new UnsupportedOperationException();
   }

}
