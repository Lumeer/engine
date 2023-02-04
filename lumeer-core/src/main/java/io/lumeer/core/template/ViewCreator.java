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
package io.lumeer.core.template;

import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.LinkAttributeFilter;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.ResourcesPermissions;
import io.lumeer.api.model.View;
import io.lumeer.api.model.ViewSettings;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.exception.TemplateNotAvailableException;
import io.lumeer.core.facade.ViewFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewCreator extends WithIdCreator {

   private final ViewFacade viewFacade;
   private final ObjectMapper mapper;
   private final ConstraintManager constraintManager;

   private ViewCreator(final TemplateParser templateParser, final ViewFacade viewFacade, final DefaultConfigurationProducer defaultConfigurationProducer) {
      super(templateParser);
      this.viewFacade = viewFacade;
      this.constraintManager = ConstraintManager.getInstance(defaultConfigurationProducer);

      this.mapper = createObjectMapper();
   }

   public static void createViews(final TemplateParser templateParser, final ViewFacade viewFacade, final DefaultConfigurationProducer defaultConfigurationProducer) {
      final ViewCreator creator = new ViewCreator(templateParser, viewFacade, defaultConfigurationProducer);
      creator.createViews();
   }

   private void createViews() {
      ((JSONArray) templateParser.getTemplate().get("views")).forEach(viewObj -> {
         try {
            var viewJson = (JSONObject) viewObj;
            var templateId = TemplateParserUtils.getId(viewJson);
            viewJson.remove("_id");
            var view = mapper.readValue(viewJson.toJSONString(), View.class);
            viewJson.put("_id", templateId);
            view.setCode(null);
            view.setQuery(translateQuery(view.getQuery()));
            view.setConfig(templateParser.translateConfig(view.getConfig(), constraintManager));
            view.setSettings(translateViewSettings(view.getSettings()));
            view.setPermissions(new Permissions());
            view = viewFacade.createView(view);
            templateParser.getDict().addView(templateId, view);
         } catch (IOException e) {
            throw new TemplateNotAvailableException(e);
         }
      });
   }

   private Query translateQuery(final Query query) {
      var newStems = new ArrayList<QueryStem>();

      query.getStems().forEach(stem -> {
         var collectionId = stem.getCollectionId() != null ? templateParser.getDict().getCollectionId(stem.getCollectionId()) : null;

         List<String> linkTypeIds = new ArrayList<>();
         var linkTypeIdsUsed = false;
         if (stem.getLinkTypeIds() != null) {
            linkTypeIdsUsed = true;
            stem.getLinkTypeIds().forEach(linkTypeId -> linkTypeIds.add(templateParser.getDict().getLinkTypeId(linkTypeId)));
         }

         Set<String> documentIds = new HashSet<>();
         var documentIdsUsed = false;
         if (stem.getDocumentIds() != null) {
            documentIdsUsed = true;
            stem.getDocumentIds().forEach(documentId -> documentIds.add(templateParser.getDict().getDocumentId(documentId)));
         }

         List<CollectionAttributeFilter> collectionAttributeFilters = new ArrayList<>();
         var filtersUsed = false;
         if (stem.getFilters() != null) {
            filtersUsed = true;
            stem.getFilters().forEach(filter -> collectionAttributeFilters.add(new CollectionAttributeFilter(
                  templateParser.getDict().getCollectionId(filter.getCollectionId()),
                  filter.getAttributeId(),
                  filter.getCondition(),
                  filter.getConditionValues()
            )));
         }

         List<LinkAttributeFilter> linkAttributeFilters = new ArrayList<>();
         var linkFiltersUsed = false;
         if (stem.getCollectionId() != null) {
            linkFiltersUsed = true;
            stem.getLinkFilters().forEach(filter -> linkAttributeFilters.add(new LinkAttributeFilter(
                  templateParser.getDict().getLinkTypeId(filter.getLinkTypeId()),
                  filter.getAttributeId(),
                  filter.getCondition(),
                  filter.getConditionValues()
            )));
         }

         newStems.add(new QueryStem(
               null,
               collectionId,
               linkTypeIdsUsed ? linkTypeIds : null,
               documentIdsUsed ? documentIds : null,
               filtersUsed ? collectionAttributeFilters : null,
               linkFiltersUsed ? linkAttributeFilters : null
         ));
      });

      return new Query(newStems, query.getFulltexts(), query.getPage(), query.getPageSize());
   }

   private ViewSettings translateViewSettings(final ViewSettings viewSettings) {
      if (viewSettings == null) {
         return new ViewSettings();
      }
      Object attributes = templateParser.translateConfig(viewSettings.getAttributes(), constraintManager);
      Object data = templateParser.translateConfig(viewSettings.getData(), constraintManager);
      Object modals = templateParser.translateConfig(viewSettings.getModals(), constraintManager);
      ResourcesPermissions resourcesPermissions = new ResourcesPermissions();

      return new ViewSettings(attributes, data, modals, resourcesPermissions);
   }

}
