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
package io.lumeer.core.task;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.User;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.matcher.MatchQueryFactory;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.js.DataFilter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AutoLinkBatchTask extends AbstractContextualTask {

   private static final Logger log = Logger.getLogger(AutoLinkBatchTask.class.getName());

   private AutoLinkRule rule;
   private LinkType linkType;
   private Collection collection;
   private Attribute attribute;
   private Collection otherCollection;
   private Attribute otherAttribute;
   private List<Document> sourceDocuments;
   private List<Document> targetDocuments;
   private List<LinkInstance> existingLinks;
   private ConstraintData constraintData;
   private Language language;
   private Map<String, AllowedPermissions> permissions;
   private User user;
   private Function<Object, Query> matchQuery;

   public void setupBatch(final AutoLinkRule rule, final LinkType linkType,
         final Collection collection, final Attribute attribute,
         final Collection otherCollection, final Attribute otherAttribute,
         final User user,
         final Map<String, AllowedPermissions> permissions) {
      this.rule = rule;
      this.linkType = linkType;
      this.collection = collection;
      this.attribute = attribute;
      this.otherCollection = otherCollection;
      this.otherAttribute = otherAttribute;
      this.permissions = permissions;
      this.user = user;

      sourceDocuments = daoContextSnapshot.getDocumentDao().getDocumentsByCollection(collection.getId());
      targetDocuments = daoContextSnapshot.getDocumentDao().getDocumentsByCollection(otherCollection.getId());
      existingLinks = daoContextSnapshot.getLinkInstanceDao().getLinkInstancesByLinkType(linkType.getId());

      language = Language.fromString(requestDataKeeper.getUserLocale());
      timeZone = requestDataKeeper.getTimezone();
      final TranslationManager translationManager = new TranslationManager();
      constraintData = new ConstraintData(
            daoContextSnapshot.getUserDao().getAllUsers(daoContextSnapshot.getSelectedWorkspace().getOrganization().get().getId()),
            user,
            translationManager.translateDurationUnitsMap(language),
            new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
            timeZone != null ? timeZone : TimeZone.getDefault().getID()
      );

      matchQuery = MatchQueryFactory.getMatchQuery(attribute, otherCollection, otherAttribute);
   }

   @Override
   public void process(final TaskExecutor executor, final ChangesTracker changesTracker) {
      try {
         List<LinkInstance> linksForCreation = new ArrayList<>();

         final Map<String, Document> sourceDocumentsById = sourceDocuments.stream().collect(Collectors.toMap(Document::getId, Function.identity()));

         // group source documents by matching attribute value
         final Map<Object, Set<String>> source = new HashMap<>();
         daoContextSnapshot.getDataDao().getDataStream(collection.getId()).forEach(dd -> {
            sourceDocumentsById.get(dd.getId()).setData(getConstraintManager().decodeDataTypes(collection, dd));
            final Object o = sourceDocumentsById.get(dd.getId()).getData().getObject(attribute.getId());

            if (o != null) {
               source.computeIfAbsent(o, key -> new HashSet<>()).add(dd.getId());
            }
         });

         if (source.size() > 0) {
            // read data for all target documents
            final Map<String, Document> targetDocumentsById = targetDocuments.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
            daoContextSnapshot.getDataDao().getDataStream(otherCollection.getId()).forEach(dd -> {
               targetDocumentsById.get(dd.getId()).setData(getConstraintManager().decodeDataTypes(otherCollection, dd));
            });

            // for every unique source value and all source documents sharing the value
            source.forEach((value, documentIds) -> {
               // find target documents matching this value
               final List<Document> matchingDocuments = findMatchingDocuments(targetDocuments, value);

               // for all source documents
               documentIds.forEach(sourceDocumentId -> {
                  // filter out the matching documents to which the source document is already linked, and create new links for the rest
                  final List<Document> linkingDocuments = filterExistingLinks(sourceDocumentId, matchingDocuments, existingLinks);
                  linksForCreation.addAll(linkingDocuments.stream().map(doc -> {
                     var l = new LinkInstance(linkType.getId(), List.of(sourceDocumentId, doc.getId()));
                     l.setCreatedBy(user.getId());
                     l.setCreationDate(ZonedDateTime.now());
                     return l;
                  }).collect(Collectors.toList()));
               });
            });

            // submit changes
            if (linksForCreation.size() > 0) {
               final List<LinkInstance> newLinks = daoContextSnapshot.getLinkInstanceDao().createLinkInstances(linksForCreation, false);
               linkTypeAdapter.mapLinkTypeData(linkType);
               changesTracker.addLinkTypes(Set.of(linkType));
               changesTracker.updateLinkTypesMap(Map.of(linkType.getId(), linkType));
               changesTracker.addCreatedLinkInstances(newLinks);
            }
         }
      } catch (Exception e) {
         log.log(Level.SEVERE, "Error running auto-link batch: ", e);
      }
   }

   public AutoLinkRule getRule() {
      return rule;
   }

   private List<Document> findMatchingDocuments(final List<Document> allDocuments, final Object value) {
      final Tuple<List<Document>, List<LinkInstance>> tuple =
            DataFilter.filterDocumentsAndLinksByQueryDecodingFromJson(
               allDocuments, List.of(otherCollection), List.of(), List.of(), matchQuery.apply(value),
               permissions, Map.of(),
               constraintData,
               true,
               language
      );
      return tuple.getFirst();
   }

   private List<Document> filterExistingLinks(final String sourceDocumentId, final List<Document> documents, final List<LinkInstance> links) {
      final Set<String> linkedDocumentIds = links
            .stream()
            .filter(link -> link.getDocumentIds().contains(sourceDocumentId))
            .map(link -> link.getDocumentIds().get(0).equals(sourceDocumentId) ? link.getDocumentIds().get(1) : link.getDocumentIds().get(0))
            .collect(Collectors.toSet());
      return documents.stream().filter(document -> !linkedDocumentIds.contains(document.getId())).collect(Collectors.toList());
   }
}
