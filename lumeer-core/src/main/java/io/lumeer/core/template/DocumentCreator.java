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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.engine.api.data.DataDocument;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DocumentCreator extends WithIdCreator {

   final private DocumentFacade documentFacade;
   final private AuthenticatedUser authenticatedUser;
   final private TemplateMetadata templateMetadata;
   final private int maxDocuments;

   private DocumentCreator(final TemplateParser templateParser, final DocumentFacade documentFacade, final AuthenticatedUser authenticatedUser, final TemplateMetadata templateMetadata, final int maxDocuments) {
      super(templateParser);
      this.documentFacade = documentFacade;
      this.authenticatedUser = authenticatedUser;
      this.templateMetadata = templateMetadata;
      this.maxDocuments = maxDocuments;
   }

   public static void createDocuments(final TemplateParser templateParser, final DocumentFacade documentFacade, final AuthenticatedUser authenticatedUser, final TemplateMetadata templateMetadata, final int maxDocuments) {
      final DocumentCreator creator = new DocumentCreator(templateParser, documentFacade, authenticatedUser, templateMetadata, maxDocuments);
      creator.createDocuments();
   }

   private void createDocuments() {
      final JSONArray collections = (JSONArray) templateParser.template.get("collections");
      final int maxDocumentsPerCollection = (maxDocuments < 0) ? -1 : (maxDocuments / collections.size() - 20); // 20 is a reserve so that users can create some more documents

      JSONArray a = (JSONArray) templateParser.getTemplate().get("documents");
      final Map<String, List<Document>> documents = new HashMap<>();
      a.forEach(doc -> {
         var docObj = (JSONObject) doc;
         var documentTemplateId = TemplateParserUtils.getId(docObj);
         var collectionTemplateId = (String) docObj.get("collectionId");

         var docu = new Document(new DataDocument());
         getDocumentData(docu, documentTemplateId, collectionTemplateId);
         docu.setMetaData(new DataDocument("templateId", documentTemplateId));

         var groupedDocuments = documents.computeIfAbsent(collectionTemplateId, cId -> new ArrayList<>());

         if (maxDocumentsPerCollection < 0 || groupedDocuments.size() < maxDocumentsPerCollection || a.size() < maxDocuments) {
            groupedDocuments.add(docu);
         }
      });

      documents.forEach((collectionTemplateId, collectionDocuments) -> {
         var storedDocuments = documentFacade.createDocuments(templateParser.getDict().getCollectionId(collectionTemplateId), collectionDocuments, true);
         storedDocuments.forEach(doc -> templateParser.getDict().addDocument(doc.getMetaData().getString("templateId"), doc));
      });

      final Map<String, List<Document>> updates = new HashMap<>();
      a.forEach(doc -> {
         var docObj = (JSONObject) doc;
         var metaData = docObj.get("metaData");

         if (metaData != null) {
            var templateParentId = ((JSONObject) metaData).get("parentId");

            if (templateParentId != null) {
               var parentId = templateParser.getDict().getDocumentId((String) templateParentId);
               var documentTemplateId = TemplateParserUtils.getId(docObj);
               var document = templateParser.getDict().getDocument(documentTemplateId);

               if (document.getMetaData() == null) {
                  document.setMetaData(new DataDocument());
               }

               document.setMetaData(document.getMetaData().append("parentId", parentId));

               updates.computeIfAbsent(document.getCollectionId(), cId -> new ArrayList<>()).add(document);
            }
         }
      });

      updates.forEach(documentFacade::updateDocumentsMetaData);
   }

   @SuppressWarnings("unchecked")
   private Document getDocumentData(final Document document, final String documentTemplateId, final String collectionTemplateId) {
      final Optional<JSONObject> data = ((JSONArray) ((JSONObject) templateParser.getTemplate().get("data")).get(collectionTemplateId)).stream().filter(d -> documentTemplateId.equals(TemplateParserUtils.getId((JSONObject) d))).findFirst();

      if (data.isPresent() && data.get().size() > 1) {
         document.setData(translateDataDocument(data.get(), authenticatedUser, templateMetadata.getDateAddition()));
      }

      return document;
   }
}
