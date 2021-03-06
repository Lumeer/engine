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
package io.lumeer.core.util;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.User;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentUtils {

   private DocumentUtils() {
   }

   public static DataDocument checkDocumentKeysValidity(DataDocument dataDocument) throws InvalidDocumentKeyException {
      DataDocument ndd = new DataDocument();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = entry.getKey().trim();
         if (!isAttributeNameValid(attributeName)) {
            throw new InvalidDocumentKeyException(attributeName);
         }
         Object value = entry.getValue();
         if (isDataDocument(value)) {
            ndd.put(attributeName, checkDocumentKeysValidity((DataDocument) value));
         } else if (isList(value)) {
            List l = (List) entry.getValue();
            if (!l.isEmpty() && isDataDocument(l.get(0))) {
               ArrayList<DataDocument> docs = new ArrayList<>();
               ndd.put(attributeName, docs);
               for (Object o : l) {
                  docs.add(checkDocumentKeysValidity((DataDocument) o));
               }
            } else {
               ndd.put(attributeName, l);
            }
         } else {
            ndd.put(attributeName, value);
         }
      }
      return ndd;
   }

   public static DataDocument cleanInvalidAttributes(final DataDocument dataDocument) {
      final DataDocument ndd = new DataDocument();

      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         final String attributeName = entry.getKey().trim();
         if (isAttributeNameValid(attributeName)) {
            final Object value = entry.getValue();

            if (isDataDocument(value)) {
               ndd.put(attributeName, cleanInvalidAttributes((DataDocument) value));
            } else if (isList(value)) {
               List l = (List) entry.getValue();
               if (!l.isEmpty() && isDataDocument(l.get(0))) {
                  ArrayList<DataDocument> docs = new ArrayList<>();
                  ndd.put(attributeName, docs);
                  for (Object o : l) {
                     docs.add(cleanInvalidAttributes((DataDocument) o));
                  }
               } else {
                  ndd.put(attributeName, l);
               }
            } else {
               ndd.put(attributeName, value);
            }
         }
      }
      return ndd;
   }

   public static boolean isAttributeNameValid(String attributeName) {
      return attributeName.equals("_id") || !(attributeName.startsWith("$") || attributeName.startsWith("_") || attributeName.contains("."));
   }

   public static Map<String, List<Document>> getDocumentsByCollection(final List<Document> documents) {
      return documents.stream().collect(Collectors.groupingBy(Document::getCollectionId, mapping(d -> d, toList())));
   }

   public static Map<String, Collection> getCollectionsMap(final CollectionDao collectionDao, final List<Document> documents) {
      Map<String, List<Document>> documentsByCollection = getDocumentsByCollection(documents);
      return collectionDao.getCollectionsByIds(documentsByCollection.keySet())
                     .stream().collect(Collectors.toMap(Collection::getId, coll -> coll));
   }

   public static Map<String, Collection> getCollectionsMap(final CollectionDao collectionDao, final Map<String, List<Document>> documentsByCollection) {
      return collectionDao.getCollectionsByIds(documentsByCollection.keySet())
                     .stream().collect(Collectors.toMap(Collection::getId, coll -> coll));
   }

   public static Set<String> getDocumentAttributes(DataDocument dataDocument, String prefix) {
      Set<String> attrs = new HashSet<>();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = prefix + entry.getKey().trim();
         attrs.add(attributeName);
         if (isDataDocument(entry.getValue())) {
            attrs.addAll(getDocumentAttributes((DataDocument) entry.getValue(), attributeName + "."));
         }
      }
      return attrs;
   }

   public static List<Document> getDocuments(final DaoContextSnapshot dao, final Query query, final User user, final Language language, final AllowedPermissions permissions) {
      if (dao.getSelectedWorkspace().getOrganization().isPresent() && query.getCollectionIds().size() > 0) {
         final String collectionId = query.getCollectionIds().iterator().next();
         final Collection collection = dao.getCollectionDao().getCollectionById(collectionId);
         final List<Document> documents = dao.getDocumentDao().getDocumentsByCollection(collectionId);
         final Map<String, Document> documentsByIds = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
         dao.getDataDao().getData(collectionId, documents.stream().map(Document::getId).collect(Collectors.toSet())).forEach(data -> {
            final Document doc = documentsByIds.get(data.getId());
            if (doc != null) {
               doc.setData(data);
            }
         });

         final TranslationManager translationManager = new TranslationManager();
         final ConstraintData constraintData = new ConstraintData(
               dao.getUserDao().getAllUsers(dao.getSelectedWorkspace().getOrganization().get().getId()),
               user,
               translationManager.translateDurationUnitsMap(language),
               new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
               null
         );

         final Tuple<List<Document>, List<LinkInstance>> result = DataFilter.filterDocumentsAndLinksByQuery(
               documents, List.of(collection), List.of(), List.of(), query,
               Map.of(collectionId, permissions),
               Map.of(),
               constraintData,
               true,
               language
         );

         return result.getFirst();
      }

      return List.of();
   }


   private static boolean isDataDocument(Object obj) {
      return obj != null && obj instanceof DataDocument;
   }

   private static boolean isList(Object obj) {
      return obj != null && obj instanceof List;
   }

}
