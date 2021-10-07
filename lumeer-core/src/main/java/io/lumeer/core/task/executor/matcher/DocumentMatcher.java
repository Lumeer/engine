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
package io.lumeer.core.task.executor.matcher;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.api.util.PermissionUtils;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.util.SelectionListUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentMatcher {
   private final DaoContextSnapshot dao;
   private final RuleTask ruleTask;

   private final Organization organization;
   private final Project project;

   private LinkType linkType;

   private Document thisDocument; // either old or new, based on which one is present, to refer to document ID and properties that do not change
   private Document oldDocument;
   private Document newDocument;

   private Collection thisCollection;
   private Collection thatCollection;

   private Map<String, AllowedPermissions> collectionPermissions;
   private Map<String, AllowedPermissions> linkTypePermissions;

   private Attribute thisAttribute;
   private Attribute thatAttribute;

   private Object oldValue;
   private Object newValue;

   private Language language;
   private ConstraintData constraintData;

   private MatchQueryProvider queryProvider;

   public DocumentMatcher(final DaoContextSnapshot dao, final RuleTask ruleTask) {
      this.dao = dao;
      this.ruleTask = ruleTask;
      this.organization = dao.getOrganization();
      this.project = dao.getProject();
   }

   private void initializeLocalizationData() {
      language = Language.fromString(ruleTask.getCurrentLocale());
      final String timeZone = ruleTask.getTimeZone();
      final TranslationManager translationManager = new TranslationManager();
      constraintData = new ConstraintData(
            dao.getUserDao().getAllUsers(this.organization.getId()),
            ruleTask.getInitiator(),
            translationManager.translateDurationUnitsMap(language),
            new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
            timeZone != null ? timeZone : TimeZone.getDefault().getID(),
            dao.getGroupDao().getAllGroups(this.organization.getId()),
            SelectionListUtils.appendPredefinedLists(dao.getSelectionListDao().getAllLists(Collections.singletonList(project.getId())))
      );
   }

   private void initializePermissions() {
      Set<RoleType> thisCollectionRoles = PermissionUtils.getUserRolesInResource(this.organization, this.project, this.thisCollection, this.ruleTask.getInitiator(), this.ruleTask.getGroups());
      Set<RoleType> thatCollectionRoles = PermissionUtils.getUserRolesInResource(this.organization, this.project, this.thatCollection, this.ruleTask.getInitiator(), this.ruleTask.getGroups());

      AllowedPermissions thisCollectionPermissions = new AllowedPermissions(thisCollectionRoles);
      AllowedPermissions thatCollectionPermissions = new AllowedPermissions(thatCollectionRoles);
      AllowedPermissions linkTypePermission = AllowedPermissions.merge(thisCollectionPermissions, thatCollectionPermissions);

      collectionPermissions = Map.of(thisCollection.getId(), thisCollectionPermissions, thatCollection.getId(), thatCollectionPermissions);
      linkTypePermissions = Map.of(linkType.getId(), linkTypePermission);
   }

   private void initializeQueryProvider() {
      final boolean thisMulti = AttributeUtil.isMultiselect(thisAttribute);
      final boolean thatMulti = AttributeUtil.isMultiselect(thatAttribute);

      if (!thisMulti) {
         if (!thatMulti) {
            queryProvider = new SimpleToSimpleMatch(this);
         } else {
            queryProvider = new SimpleToMultiselectMatch(this);
         }
      } else {
         if (!thatMulti) {
            queryProvider = new MultiselectToSimpleMatch(this);
         } else {
            queryProvider = new MultiselectToMultiselectMatch(this);
         }
      }
   }

   public boolean initialize(final AutoLinkRule rule) {
      linkType = dao.getLinkTypeDao().getLinkType(rule.getLinkType());

      if (linkType != null) {
         initializeLocalizationData();

         thisDocument = ruleTask.getOldDocument() != null ? ruleTask.getOldDocument() : ruleTask.getNewDocument();
         final String thisCollectionId = thisDocument.getCollectionId();
         final String thatCollectionId = linkType.getCollectionIds().get(0).equals(thisCollectionId) ? linkType.getCollectionIds().get(1) : linkType.getCollectionIds().get(0);
         final String thisAttributeId, thatAttributeId;
         thisCollection = dao.getCollectionDao().getCollectionById(thisCollectionId);
         thatCollection = dao.getCollectionDao().getCollectionById(thatCollectionId);
         initializePermissions();

         if (rule.getCollection1().equals(thisCollectionId)) {
            thisAttributeId = rule.getAttribute1();
            thatAttributeId = rule.getAttribute2();
         } else {
            thisAttributeId = rule.getAttribute2();
            thatAttributeId = rule.getAttribute1();
         }

         thisAttribute = CollectionUtil.getAttribute(thisCollection, thisAttributeId);
         thatAttribute = CollectionUtil.getAttribute(thatCollection, thatAttributeId);
         initializeQueryProvider();

         // check whether the document hasn't been deleted by previous rules
         if (ruleTask.getNewDocument() != null) {
            if (ruleTask.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(ruleTask.getNewDocument().getId()).size() <= 0) {
               return false;
            }
         }
         if (ruleTask.getOldDocument() != null) {
            if (ruleTask.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(ruleTask.getOldDocument().getId()).size() <= 0) {
               return false;
            }
         }

         if (ruleTask.getOldDocument() != null) {
            oldDocument = new Document(ruleTask.getOldDocument());
            oldDocument.setData(ruleTask.getConstraintManager().decodeDataTypes(thisCollection, oldDocument.getData()));
            oldValue = oldDocument.getData().get(thisAttributeId);
         }
         if (ruleTask.getNewDocument() != null) {
            newDocument = new Document(ruleTask.getNewDocument());
            newDocument.setData(ruleTask.getConstraintManager().decodeDataTypes(thisCollection, newDocument.getData()));
            newValue = newDocument.getData().get(thisAttributeId);
         }

         return true;
      }

      return false;
   }

   public LinkType getLinkType() {
      return linkType;
   }

   public Document getThisDocument() {
      return thisDocument;
   }

   public Document getOldDocument() {
      return oldDocument;
   }

   public Document getNewDocument() {
      return newDocument;
   }

   public Collection getThisCollection() {
      return thisCollection;
   }

   public Collection getThatCollection() {
      return thatCollection;
   }

   public Attribute getThisAttribute() {
      return thisAttribute;
   }

   public Attribute getThatAttribute() {
      return thatAttribute;
   }

   public Object getOldValue() {
      return oldValue;
   }

   public Object getNewValue() {
      return newValue;
   }

   public List<LinkInstance> getAllLinkInstances() {
      final SearchQuery query = getLinkInstanceQuery(Set.of(thisDocument.getId()));
      return dao.getLinkInstanceDao().searchLinkInstances(query);
   }

   public SearchQuery getLinkInstanceQuery(final Set<String> documentIds) {
      return SearchQuery
            .createBuilder()
            .stems(Collections.singletonList(
                  SearchQueryStem
                        .createBuilder("")
                        .linkTypeIds(Collections.singletonList(linkType.getId()))
                        .documentIds(documentIds)
                        .build()))
            .build();
   }

   private List<Document> getLinkedDocuments(final List<LinkInstance> links) {
      final Set<String> ids = new HashSet<>();
      links.forEach(link -> {
         link.getDocumentIds().forEach(id -> {
            if (!id.equals(thisDocument.getId())) {
               ids.add(id);
            }
         });
      });

      return loadDataAndDecode(ids);
   }

   private List<Document> loadDataAndDecode(final Set<String> ids) {
      final List<Document> docs = dao.getDocumentDao().getDocumentsByIds(ids);
      return loadDataAndDecode(docs, ids);
   }

   private List<Document> loadDataAndDecode(final List<Document> documents) {
      final Set<String> ids = documents.stream().map(Document::getId).collect(Collectors.toSet());
      return loadDataAndDecode(documents, ids);
   }

   private List<Document> loadDataAndDecode(final List<Document> documents, final Set<String> ids) {
      final Map<String, DataDocument> data = dao.getDataDao().getData(thatCollection.getId(), ids).stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      documents.forEach(doc -> {
         doc.setData(ruleTask.getConstraintManager().decodeDataTypes(thatCollection, data.get(doc.getId())));
      });

      return documents;
   }

   private Tuple<List<Document>, List<LinkInstance>> filterDocuments(final Query query, final List<Document> documents, final List<LinkInstance> links) {
      return DataFilter.filterDocumentsAndLinksByQueryDecodingFromJson(
            documents, List.of(thisCollection, thatCollection), List.of(linkType), links, query,
            collectionPermissions, linkTypePermissions,
            constraintData,
            true,
            language
      );
   }

   public Tuple<List<Document>, List<LinkInstance>> filterForRemoval() {
      final List<Document> documents = new ArrayList<>();
      documents.add(getOldDocument());

      final List<LinkInstance> links = getAllLinkInstances();
      documents.addAll(getLinkedDocuments(links));

      final Query query = queryProvider.getMatchQueryForRemoval(oldValue, newValue);

      return filterDocuments(query, documents, links);
   }

   public Tuple<List<Document>, List<LinkInstance>> filterForCreation() {
      final List<Document> documents = loadDataAndDecode(dao.getDocumentDao().getDocumentsByCollection(thatCollection.getId()));

      final Query query = queryProvider.getMatchQueryForCreation(oldValue, newValue);
      return filterDocuments(query, documents, List.of());
   }
}
