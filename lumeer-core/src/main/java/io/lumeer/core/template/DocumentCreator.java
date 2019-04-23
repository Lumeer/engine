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

import io.lumeer.api.model.Document;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.engine.api.data.DataDocument;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Optional;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DocumentCreator extends WithIdCreator {

   final private DocumentFacade documentFacade;

   private DocumentCreator(final TemplateParser templateParser, final DocumentFacade documentFacade) {
      super(templateParser);
      this.documentFacade = documentFacade;
   }

   public static void createDocuments(final TemplateParser templateParser, final DocumentFacade documentFacade) {
      final DocumentCreator creator = new DocumentCreator(templateParser, documentFacade);
      creator.createDocuments();
   }

   private void createDocuments() {
      JSONArray a = (JSONArray) templateParser.getTemplate().get("documents");
      a.forEach(doc -> {
         var docObj = (JSONObject) doc;
         var documentTemplateId = TemplateParserUtils.getId(docObj);
         var collectionTemplateId = (String) docObj.get("collectionId");
         var data = new DataDocument();
         var document = documentFacade.createDocument(templateParser.getDict().getCollectionId(collectionTemplateId), new Document(data));
         templateParser.getDict().addDocument(documentTemplateId, document);

         getDocumentData(document, documentTemplateId, collectionTemplateId);
         document = documentFacade.updateDocumentData(document.getCollectionId(), document.getId(), document.getData());
      });

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
               documentFacade.updateDocumentMetaData(document.getCollectionId(), document.getId(), document.getMetaData());
            }
         }
      });
   }

   @SuppressWarnings("unchecked")
   private Document getDocumentData(final Document document, final String documentTemplateId, final String collectionTemplateId) {
      final Optional<JSONObject> data = ((JSONArray) ((JSONObject) templateParser.getTemplate().get("data")).get(collectionTemplateId)).stream().filter(d -> documentTemplateId.equals(TemplateParserUtils.getId((JSONObject) d))).findFirst();

      if (data.isPresent() && data.get().size() > 1) {
         document.setData(translateDataDocument(templateParser.getDict().getCollection(collectionTemplateId), data.get()));
      }

      return document;
   }
}
