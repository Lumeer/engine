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
package io.lumeer.core.task.executor;

import static io.lumeer.core.util.Utils.computeIfNotNull;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.core.task.executor.request.GenericPrintRequest;
import io.lumeer.core.task.executor.request.NavigationRequest;
import io.lumeer.core.task.executor.request.SendEmailRequest;
import io.lumeer.core.task.executor.request.UserMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChangesTracker {

   private final Set<Document> createdDocuments = new HashSet<>();
   private final Set<LinkInstance> createdLinkInstances = new HashSet<>();
   private final Set<Document> updatedDocuments = new HashSet<>();
   private final Set<Document> removedDocuments = new HashSet<>();
   private final Set<LinkInstance> updatedLinkInstances = new HashSet<>();
   private final Set<LinkInstance> removedLinkInstances = new HashSet<>();
   private final Set<Collection> collections = new HashSet<>();
   private final Set<LinkType> linkTypes = new HashSet<>();
   private final Set<String> sequences = new HashSet<>();
   private final List<UserMessageRequest> userMessageRequests = new ArrayList<>();
   private final List<GenericPrintRequest> printRequests = new ArrayList<>();
   private final List<NavigationRequest> navigationRequests = new ArrayList<>();
   private final List<SendEmailRequest> sendEmailRequests = new ArrayList<>();

   final Map<String, Collection> collectionsMap = new HashMap<>();
   final Map<String, LinkType> linkTypesMap = new HashMap<>();

   public ChangesTracker() {
   }

   public ChangesTracker merge(final Set<Collection> collections, final Set<Document> createdDocuments,
         final Set<Document> updatedDocuments, final Set<Document> removedDocuments, final Set<LinkType> linkTypes,
         final Set<LinkInstance> createdLinkInstances, final Set<LinkInstance> updatedLinkInstances,
         final Set<LinkInstance> removedLinkInstances, final Set<String> sequences,
         final List<UserMessageRequest> userMessageRequests, final List<GenericPrintRequest> printRequests,
         final List<NavigationRequest> navigationRequests, final List<SendEmailRequest> sendEmailRequests,
         final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap) {
      if (collections != null) {
         this.collections.removeAll(collections);
         this.collections.addAll(collections);
      }

      if (createdDocuments != null) {
         this.createdDocuments.removeAll(createdDocuments);
         this.createdDocuments.addAll(createdDocuments);
      }

      if (updatedDocuments != null) {
         this.updatedDocuments.removeAll(updatedDocuments);
         this.updatedDocuments.addAll(updatedDocuments);
      }

      if (removedDocuments != null) {
         this.removedDocuments.removeAll(removedDocuments);
         this.removedDocuments.addAll(removedDocuments);
      }

      if (linkTypes != null) {
         this.linkTypes.removeAll(linkTypes);
         this.linkTypes.addAll(linkTypes);
      }

      if (createdLinkInstances != null) {
         this.createdLinkInstances.removeAll(createdLinkInstances);
         this.createdLinkInstances.addAll(createdLinkInstances);
      }

      if (updatedLinkInstances != null) {
         this.updatedLinkInstances.removeAll(updatedLinkInstances);
         this.updatedLinkInstances.addAll(updatedLinkInstances);
      }

      if (removedLinkInstances != null) {
         this.removedLinkInstances.removeAll(removedLinkInstances);
         this.removedLinkInstances.addAll(removedLinkInstances);
      }

      if (sequences != null) {
         this.sequences.addAll(sequences);
      }

      if (userMessageRequests != null) {
         this.userMessageRequests.addAll(userMessageRequests);
      }

      if (collectionsMap != null) {
         this.collectionsMap.putAll(collectionsMap);
      }

      if (linkTypesMap != null) {
         this.linkTypesMap.putAll(linkTypesMap);
      }

      if (printRequests != null) {
         this.printRequests.addAll(printRequests);
      }

      if (navigationRequests != null) {
         this.navigationRequests.addAll(navigationRequests);
      }

      if (sendEmailRequests != null) {
         this.sendEmailRequests.addAll(sendEmailRequests);
      }

      return this;
   }

   public ChangesTracker merge(final ChangesTracker other) {
      if (other == null) {
         return this;
      }

      return merge(other.collections, other.createdDocuments, other.updatedDocuments, other.removedDocuments,
            other.linkTypes, other.createdLinkInstances, other.updatedLinkInstances, other.removedLinkInstances,
            other.sequences, other.userMessageRequests, other.printRequests,
            other.navigationRequests, other.sendEmailRequests,
            other.collectionsMap, other.linkTypesMap);
   }

   public Set<Document> getCreatedDocuments() {
      return createdDocuments;
   }

   public Set<Document> getUpdatedDocuments() {
      return updatedDocuments;
   }

   public Set<Document> getRemovedDocuments() {
      return removedDocuments;
   }

   public Set<LinkInstance> getCreatedLinkInstances() {
      return createdLinkInstances;
   }

   public Set<LinkInstance> getUpdatedLinkInstances() {
      return updatedLinkInstances;
   }

   public Set<LinkInstance> getRemovedLinkInstances() {
      return removedLinkInstances;
   }

   public Set<Collection> getCollections() {
      return collections;
   }

   public Set<LinkType> getLinkTypes() {
      return linkTypes;
   }

   public Set<String> getSequences() {
      return sequences;
   }

   public List<UserMessageRequest> getUserMessages() {
      return userMessageRequests;
   }

   public Map<String, Collection> getCollectionsMap() {
      return collectionsMap;
   }

   public Map<String, LinkType> getLinkTypesMap() {
      return linkTypesMap;
   }

   public List<GenericPrintRequest> getPrintRequests() {
      return printRequests;
   }

   public List<NavigationRequest> getNavigationRequests() {
      return navigationRequests;
   }

   public List<SendEmailRequest> getSendEmailRequests() {
      return sendEmailRequests;
   }

   public void addCreatedDocuments(final java.util.Collection<Document> createdDocuments) {
      this.createdDocuments.addAll(createdDocuments);
   }

   public void addUpdatedDocuments(final java.util.Collection<Document> updatedDocuments) {
      this.updatedDocuments.addAll(updatedDocuments);
   }

   public void addRemovedDocuments(final java.util.Collection<Document> removedDocuments) {
      this.removedDocuments.addAll(removedDocuments);
   }

   public void addCreatedLinkInstances(final java.util.Collection<LinkInstance> createdLinkInstances) {
      this.createdLinkInstances.addAll(createdLinkInstances);
   }

   public void addUpdatedLinkInstances(final java.util.Collection<LinkInstance> updatedLinkInstances) {
      this.updatedLinkInstances.addAll(updatedLinkInstances);
   }

   public void addRemovedLinkInstances(final java.util.Collection<LinkInstance> removedLinkInstances) {
      this.removedLinkInstances.addAll(removedLinkInstances);
   }

   public void addCollections(final java.util.Collection<Collection> collections) {
      this.collections.addAll(collections);
   }

   public void addLinkTypes(final java.util.Collection<LinkType> linkTypes) {
      this.linkTypes.addAll(linkTypes);
   }

   public void addSequence(final String sequence) {
      this.sequences.add(sequence);
   }

   public void addUserMessageRequests(final List<UserMessageRequest> userMessageRequests) {
      this.userMessageRequests.addAll(userMessageRequests);
   }

   public void addPrintRequests(final List<GenericPrintRequest> printRequests) {
      this.printRequests.addAll(printRequests);
   }

   public void addNavigationRequests(final List<NavigationRequest> navigationRequests) {
      this.navigationRequests.addAll(navigationRequests);
   }

   public void addSendEmailRequests(final List<SendEmailRequest> sendEmailRequests) {
      this.sendEmailRequests.addAll(sendEmailRequests);
   }

   public void updateCollectionsMap(final Map<String, Collection> collectionsMap) {
      this.collectionsMap.putAll(collectionsMap);
   }

   public void updateLinkTypesMap(final Map<String, LinkType> linkTypesMap) {
      this.linkTypesMap.putAll(linkTypesMap);
   }

   @Override
   public String toString() {
      return "ChangesTracker{" +
            "collections=" + computeIfNotNull(collections, c -> c.stream().map(Collection::getId).collect(Collectors.joining(", "))) +
            ", createdDocuments=" + createdDocuments + //computeIfNotNull(createdDocuments, d -> d.stream().map(Document::getId).collect(Collectors.joining(", "))) +
            ", updatedDocuments=" + updatedDocuments + //computeIfNotNull(updatedDocuments, d -> d.stream().map(Document::getId).collect(Collectors.joining(", "))) +
            ", removedDocuments=" + updatedDocuments + //computeIfNotNull(updatedDocuments, d -> d.stream().map(Document::getId).collect(Collectors.joining(", "))) +
            ", linkTypes=" + computeIfNotNull(linkTypes, t -> t.stream().map(LinkType::getId).collect(Collectors.joining(", "))) +
            ", createdLinkInstances=" + computeIfNotNull(createdLinkInstances, l -> l.stream().map(LinkInstance::getId).collect(Collectors.joining(", "))) +
            ", updatedLinkInstances=" + computeIfNotNull(updatedLinkInstances, l -> l.stream().map(LinkInstance::getId).collect(Collectors.joining(", "))) +
            ", removedLinkInstances=" + computeIfNotNull(removedLinkInstances, l -> l.stream().map(LinkInstance::getId).collect(Collectors.joining(", "))) +
            ", sequences=" + sequences +
            ", userMessages=" + userMessageRequests +
            ", printRequests=" + printRequests +
            ", navigationRequests=" + printRequests +
            ", sendEmailRequests=" + printRequests +
            '}';
   }
}
