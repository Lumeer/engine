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
package io.lumeer.core.template;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.WithId;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks objects created based on a JSON template file. It maps the IDs stored in
 * the template file to the real ID as they are created in the database.
 */
public class TemplateObjectsDictionary {

   private final Map<String, Collection> collections = new HashMap<>();
   private final Map<String, LinkType> linkTypes = new HashMap<>();
   private final Map<String, View> views = new HashMap<>();
   private final Map<String, Document> documents = new HashMap<>();
   private final Map<String, LinkInstance> linkInstances = new HashMap<>();
   private final Map<String, Map<String, Attribute>> attributes = new HashMap<>();

   public void addCollection(final String templateId, final Collection collection) {
      collections.put(templateId, collection);
   }

   public String getCollectionId(final String templateId) {
      return getSafeId(collections.get(templateId));
   }

   public Collection getCollection(final String templateId) {
      return collections.get(templateId);
   }

   public void addLinkType(final String templateId, final LinkType linkType) {
      linkTypes.put(templateId, linkType);
   }

   public String getLinkTypeId(final String templateId) {
      return getSafeId(linkTypes.get(templateId));
   }

   public LinkType getLinkType(final String templateId) {
      return linkTypes.get(templateId);
   }

   public void addView(final String templateId, final View view) {
      views.put(templateId, view);
   }

   public String getViewId(final String templateId) {
      return getSafeId(views.get(templateId));
   }

   public View getView(final String templateId) {
      return views.get(templateId);
   }

   public void addLinkInstance(final String templateId, final LinkInstance linkInstance) {
      linkInstances.put(templateId, linkInstance);
   }

   public String getLinkInstanceId(final String templateId) {
      return getSafeId(linkInstances.get(templateId));
   }

   public LinkInstance getLinkInstance(final String templateId) {
      return linkInstances.get(templateId);
   }

   public void addDocument(final String templateId, final Document document) {
      documents.put(templateId, document);
   }

   public String getDocumentId(final String templateId) {
      return getSafeId(documents.get(templateId));
   }

   public Document getDocument(final String templateId) {
      return documents.get(templateId);
   }

   public void addAttribute(final WithId resource, final String templateId, final Attribute attribute) {
      if (attributes.get(resource.getId()) == null) {
         attributes.put(resource.getId(), new HashMap<>());
      }
      attributes.get(resource.getId()).put(templateId, attribute);
   }

   public String getAttributeId(final WithId resource, final String templateId) {
      if (attributes.get(resource.getId()) != null) {
         var attr = attributes.get(resource.getId()).get(templateId);
         return attr != null ? attr.getId() : null;
      }

      return null;
   }

   public Attribute getAttribute(final WithId resource, final String templateId) {
      if (attributes.get(resource.getId()) != null) {
         return attributes.get(resource.getId()).get(templateId);
      }

      return null;
   }

   private String getSafeId(final WithId withId) {
      return withId != null ? withId.getId() : null;
   }
}
