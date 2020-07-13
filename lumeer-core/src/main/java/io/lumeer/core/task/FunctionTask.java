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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.function.Function;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.task.executor.FunctionTaskExecutor;
import io.lumeer.engine.api.data.DataDocument;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionTask extends AbstractContextualTask {

   private Attribute attribute;
   private Collection collection;
   private Set<Document> documents;
   private LinkType linkType;
   private Set<LinkInstance> linkInstances;

   private static DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
   private static ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);

   public void setFunctionTask(final Attribute attribute, final Collection collection, final Set<Document> documents, final  Task parent) {
      this.attribute = attribute;
      this.collection = collection;
      this.documents = documents;
      this.parent = parent;
      this.linkType = null;
      this.linkInstances = null;
   }

   public void setFunctionTask(final Attribute attribute, final LinkType linkType, final Set<LinkInstance> linkInstances, Task parent) {
      this.attribute = attribute;
      this.linkType = linkType;
      this.linkInstances = linkInstances;
      this.parent = parent;
      this.collection = null;
      this.documents = null;
   }

   public Function getFunction() {
      return attribute.getFunction();
   }

   public Attribute getAttribute() {
      return attribute;
   }

   public Collection getCollection() {
      return collection;
   }

   public Set<Document> getDocuments() {
      return documents;
   }

   public LinkType getLinkType() {
      return linkType;
   }

   public Set<LinkInstance> getLinkInstances() {
      return linkInstances;
   }

   @Override
   public void process() {
      if (documents != null && collection != null) {
         getDocumentsWithData(collection, documents).forEach(document -> {
            final FunctionTaskExecutor executor = new FunctionTaskExecutor(this, collection, document);
            executor.execute();
         });
      } else if (linkType != null && linkInstances != null) {
         getLinkInstancesWithData(linkType, linkInstances).forEach(linkInstance -> {
            final FunctionTaskExecutor executor = new FunctionTaskExecutor(this, linkType, linkInstance);
            executor.execute();
         });
      }

      if (parent != null) {
         parent.process();
      }
   }

   private Set<Document> getDocumentsWithData(final Collection collection, final Set<Document> documents) {
      if (documents.isEmpty()) {
         return Collections.emptySet();
      }
      Map<String, DataDocument> data = getDaoContextSnapshot()
            .getDataDao()
            .getData(collection.getId(), documents.stream().map(Document::getId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(DataDocument::getId, d -> d));
      return new HashSet<>(documents).stream().peek(document -> {
         DataDocument dataDocument = data.get(document.getId());
         constraintManager.encodeDataTypesForFce(collection, dataDocument);
         document.setData(dataDocument);
      }).collect(Collectors.toSet());
   }

   private Set<LinkInstance> getLinkInstancesWithData(final LinkType linkType, final Set<LinkInstance> linkInstances) {
      if (linkInstances.isEmpty()) {
         return Collections.emptySet();
      }
      Map<String, DataDocument> data = getDaoContextSnapshot()
            .getLinkDataDao()
            .getData(linkType.getId(), linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(DataDocument::getId, d -> d));
      return new HashSet<>(linkInstances).stream().peek(linkInstance -> {
         final DataDocument dataDocument = data.get(linkInstance.getId());
         constraintManager.encodeDataTypesForFce(linkType, dataDocument);
         linkInstance.setData(dataDocument);
      }).collect(Collectors.toSet());
   }

   @Override
   public void propagateChanges(final List<Document> documents, final List<LinkInstance> links) {
      if (documents != null && this.documents != null && this.documents.size() > 0 && this.documents.stream().anyMatch(documents::contains)) {
         this.documents = this.documents.stream().map(doc -> {
            int idx = documents.indexOf(doc);
            return idx >= 0 ? documents.get(idx) : doc;
         }).collect(Collectors.toSet());
      }

      if (links != null && this.linkInstances != null && this.linkInstances.size() > 0 && this.linkInstances.stream().anyMatch(links::contains)) {
         this.linkInstances = this.linkInstances.stream().map(link -> {
            int idx = links.indexOf(link);
            return idx >= 0 ? links.get(idx) : link;
         }).collect(Collectors.toSet());
      }

      super.propagateChanges(documents, links);
   }
}
