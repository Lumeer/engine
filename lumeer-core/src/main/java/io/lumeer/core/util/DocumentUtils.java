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
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.User;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.detector.Assignee;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
      if (dao.getSelectedWorkspace().getOrganization().isPresent()) {
         return getDocuments(dao.getCollectionDao(), dao.getDocumentDao(), dao.getDataDao(), dao.getUserDao(), dao.getGroupDao(), dao.getSelectionListDao(), dao.getSelectedWorkspace().getOrganization().get(), dao.getSelectedWorkspace().getProject().get(), query, user, language, permissions, timeZone);
      }

      return List.of();
   }

   public static List<Document> getDocuments(final CollectionDao collectionDao, final DocumentDao documentDao, final DataDao dataDao, final UserDao userDao, final GroupDao groupDao, final SelectionListDao selectionListDao, final Organization organization, final Project project, final Query query, final User user, final Language language, final AllowedPermissions permissions, final String timeZone) {
      if (organization != null && query.getCollectionIds().size() > 0) {
         final String collectionId = query.getCollectionIds().iterator().next();
         final Collection collection = collectionDao.getCollectionById(collectionId);
         final List<Document> documents = documentDao.getDocumentsByCollection(collectionId);
         final Map<String, Document> documentsByIds = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
         dataDao.getData(collectionId, documents.stream().map(Document::getId).collect(Collectors.toSet())).forEach(data -> {
            final Document doc = documentsByIds.get(data.getId());
            if (doc != null) {
               doc.setData(data);
            }
         });

         final TranslationManager translationManager = new TranslationManager();
         final ConstraintData constraintData = new ConstraintData(
               userDao.getAllUsers(organization.getId()),
               user,
               translationManager.translateDurationUnitsMap(language),
               new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
               timeZone != null ? timeZone : TimeZone.getDefault().getID(),
               groupDao.getAllGroups(organization.getId()),
               SelectionListUtils.appendPredefinedLists(selectionListDao.getAllLists(Collections.singletonList(project.getId())))
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

   public static List<Document> loadDocumentsData(final DataDao dataDao, final Collection collection, final List<Document> documents) {
      final Map<String, Document> documentMap = documents.stream().collect(toMap(Document::getId, Function.identity()));
      final List<DataDocument> data = dataDao.getData(collection.getId(), documentMap.keySet());
      data.forEach(dd -> {
         if (documentMap.containsKey(dd.getId())) {
            documentMap.get(dd.getId()).setData(dd);
         }
      });

      return documents;
   }

   public static boolean isDocumentOwner(final Collection collection, final Document document, String userId) {
      return document != null && document.getCreatedBy().equals(userId);
   }

   public static boolean isDocumentOwnerByPurpose(final Collection collection, final Document document, final User user, final List<Group> teams, final List<User> users) {
      return DocumentUtils.isTaskAssignedByUser(collection, document, user.getEmail(), teams, users);
   }

   public static boolean isTaskAssignedByUser(final Collection collection, final Document document, String userEmail, final List<Group> teams, final List<User> users) {
      return document != null && isTaskAssignedByUser(collection, document.getData(), userEmail, teams, users);
   }

   public static boolean isTaskAssignedByUser(final Collection collection, final DataDocument data, String userEmail, final List<Group> teams, final List<User> users) {
      return getUsersAssigneeEmails(collection, data, teams, users).stream().anyMatch(s -> StringUtils.compareIgnoreCase(s, userEmail) == 0);
   }

   public static Set<String> getUsersAssigneeEmails(final Collection collection, final Document document, final List<Group> teams, final List<User> users) {
      return document != null ? getUsersAssigneeEmails(collection, document.getData(), teams, users) : Collections.emptySet();
   }

   public static Set<String> getUsersAssigneeEmails(final Collection collection, final DataDocument data, final List<Group> teams, final List<User> users) {
      if (collection.getPurposeType() == CollectionPurposeType.Tasks) {
         final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();
         final Attribute assigneeAttribute = ResourceUtils.findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            return getUsersList(data, assigneeAttribute, teams, users).stream().map(Assignee::getEmail).collect(toSet());
         }
      }
      return Collections.emptySet();
   }

   public static Set<Assignee> getUsersList(final Document document, final Attribute attribute, final List<Group> teams, final List<User> users) {
      return document != null ? getUsersList(document.getData(), attribute, teams, users) : Collections.emptySet();
   }

   public static Set<Assignee> getUsersList(final DataDocument data, final Attribute attribute, final List<Group> teams, final List<User> users) {
      final Set<String> stringList = getStringList(data, attribute);
      if (attribute.getConstraint() != null && attribute.getConstraint().getType() == ConstraintType.User) {
         return stringList.stream().map(value -> {
            if (value.startsWith("@")) {
               final String teamId = value.substring(1);
               final Optional<Group> team = teams.stream().filter(t -> t.getId().equals(teamId)).findFirst();
               final Map<String, User> usersMap = users.stream().collect(toMap(User::getId, user -> user));
               return team.map(group -> group.getUsers().stream()
                                             .map(usersMap::get).filter(Objects::nonNull)
                                             .map(u -> Assignee.Companion.fromUser(u, true)).collect(toList()))
                          .orElseGet(ArrayList::new);
            }
            return Collections.singletonList(lookupAssignee(value, false, users));
         }).flatMap(List::stream).collect(Collectors.toSet());
      }

      return stringList.stream().map(s -> lookupAssignee(s, false, users)).collect(toSet());
   }

   private static Assignee lookupAssignee(final String email, final boolean viaTeam, final List<User> users) {
      return users.stream().filter(u -> u.getEmail().equals(email)).findFirst().map(user ->
            Assignee.Companion.fromUser(user, viaTeam)
      ).orElseGet(() ->
            new Assignee(email, viaTeam)
      );
   }

   public static Set<String> getStringList(final DataDocument data, final Attribute attribute) {
      final Object usersObject = data != null ? data.getObject(attribute.getId()) : null;
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
      final List<Document> documentsWithData = loadDocumentsData(dao.getDataDao(), collection, documents);
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
      return loadDocumentsData(dao.getDataDao(), collection, dao.getDocumentDao().getDocumentsByIds(documentIds));
   }

   public static List<Document> loadDocumentsWithData(final DaoContextSnapshot dao, final Collection collection, final Set<String> documentIds, final ConstraintManager constraintManager, final boolean encodeForFce) {
      final List<Document> documents = loadDocumentsWithData(dao, collection, documentIds);
      encodeDocumentDataForFce(collection, documents, constraintManager, encodeForFce);

      return documents;
   }

}
