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

import static java.util.stream.Collectors.*;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.User;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentUtils {

   private DocumentUtils() {
   }

   public static Map<String, List<Document>> getDocumentsByCollection(final List<Document> documents) {
      return documents.stream().collect(Collectors.groupingBy(Document::getCollectionId, mapping(d -> d, toList())));
   }

   // gets encoded documents
   public static List<Document> getDocuments(final DaoContextSnapshot dao, final Query query, final User user, final Language language, final AllowedPermissions permissions, final String timeZone) {
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
               timeZone != null ? timeZone : TimeZone.getDefault().getID()
         );

         final Tuple<List<Document>, List<LinkInstance>> result = DataFilter.filterDocumentsAndLinksByQueryDecodingFromJson(
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

   public static List<Document> loadDocumentsData(final DaoContextSnapshot dao, final Collection collection, final List<Document> documents) {
      final Map<String, Document> documentMap = documents.stream().collect(toMap(Document::getId, Function.identity()));
      final List<DataDocument> data = dao.getDataDao().getData(collection.getId(), documentMap.keySet());
      data.forEach(dd -> {
         if (documentMap.containsKey(dd.getId())) {
            documentMap.get(dd.getId()).setData(dd);
         }
      });

      return documents;
   }

   public static boolean isDocumentOwner(final Collection collection, final Document document, String userId) {
      return document.getCreatedBy().equals(userId);
   }

   public static boolean isDocumentOwnerByPurpose(final Collection collection, final Document document, final User user) {
      return DocumentUtils.isTaskAssignedByUser(collection, document, user.getEmail());
   }

   public static boolean isTaskAssignedByUser(final Collection collection, final Document document, String userEmail) {
      return isTaskAssignedByUser(collection, document.getData(), userEmail);
   }

   public static boolean isTaskAssignedByUser(final Collection collection, final DataDocument data, String userEmail) {
      return getUsersAssigneeEmails(collection, data).stream().anyMatch(s -> StringUtils.compareIgnoreCase(s, userEmail) == 0);
   }

   public static Set<String> getUsersAssigneeEmails(final Collection collection, final Document document) {
      return getUsersAssigneeEmails(collection, document.getData());
   }

   public static Set<String> getUsersAssigneeEmails(final Collection collection, final DataDocument data) {
      if (collection.getPurposeType() == CollectionPurposeType.Tasks) {
         final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();
         final Attribute assigneeAttribute = ResourceUtils.findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            return getUsersList(data, assigneeAttribute.getId());
         }
      }
      return Collections.emptySet();
   }

   @SuppressWarnings("unchecked")
   public static Set<String> getUsersList(final Document document, final String attributeId) {
      return getUsersList(document.getData(), attributeId);
   }

   @SuppressWarnings("unchecked")
   public static Set<String> getUsersList(final DataDocument data, final String attributeId) {
      final Object usersObject = data != null ? data.getObject(attributeId) : null;
      if (usersObject != null) {
         if (usersObject instanceof String) {
            return Set.of((String) usersObject);
         } else if (usersObject instanceof java.util.Collection) {
            return Set.copyOf((java.util.Collection<String>) usersObject);
         }
      }

      return Set.of();
   }

   public static Document loadDocumentWithData(final DocumentDao documentDao, final DataDao dataDao, final String documentId) {
      final Document document = documentDao.getDocumentById(documentId);
      document.setData(dataDao.getData(document.getCollectionId(), documentId));

      return document;
   }

   public static Document loadDocumentWithData(final DocumentDao documentDao, final DataDao dataDao, final Collection collection, final String documentId) {
      final Document document = documentDao.getDocumentById(documentId);
      document.setData(dataDao.getData(collection.getId(), documentId));

      return document;
   }

   public static List<Document> loadDocumentsData(final DaoContextSnapshot dao, final Collection collection, final List<Document> documents, final ConstraintManager constraintManager, final boolean encodeForFce) {
      final List<Document> documentsWithData = loadDocumentsData(dao, collection, documents);
      encodeDocumentDataForFce(collection, documentsWithData, constraintManager, encodeForFce);

      return documentsWithData;
   }

   private static void encodeDocumentDataForFce(final Collection collection, final List<Document> documents, final ConstraintManager constraintManager, final boolean encodeForFce) {
      documents.forEach(d -> {
         if (encodeForFce) {
            d.setData(constraintManager.encodeDataTypesForFce(collection, d.getData()));
         } else {
            d.setData(constraintManager.decodeDataTypes(collection, d.getData()));
         }
      });
   }

   public static List<Document> loadDocumentsWithData(final DaoContextSnapshot dao, final Collection collection, final Set<String> documentIds) {
      return loadDocumentsData(dao, collection, dao.getDocumentDao().getDocumentsByIds(documentIds));
   }

   public static List<Document> loadDocumentsWithData(final DaoContextSnapshot dao, final Collection collection, final Set<String> documentIds, final ConstraintManager constraintManager, final boolean encodeForFce) {
      final List<Document> documents = loadDocumentsWithData(dao, collection, documentIds);
      encodeDocumentDataForFce(collection, documents, constraintManager, encodeForFce);

      return documents;
   }

}
