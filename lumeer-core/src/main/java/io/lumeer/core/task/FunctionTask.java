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
package io.lumeer.core.task;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.function.Function;
import io.lumeer.core.task.executor.FunctionTaskExecutor;

import java.util.List;
import java.util.Set;

public class FunctionTask extends AbstractContextualTask {

   private Attribute attribute;
   private Collection collection;
   private Set<Document> documents;
   private LinkType linkType;
   private Set<LinkInstance> linkInstances;
   private List<FunctionTask> parents;

   public void setFunctionTask(final Attribute attribute, final Collection collection, final Set<Document> documents, final List<FunctionTask> parents) {
      this.attribute = attribute;
      this.collection = collection;
      this.documents = documents;
      this.parents = parents;
      this.linkType = null;
      this.linkInstances = null;
   }

   public void setFunctionTask(final Attribute attribute, final LinkType linkType, final Set<LinkInstance> linkInstances, List<FunctionTask> parents) {
      this.attribute = attribute;
      this.linkType = linkType;
      this.linkInstances = linkInstances;
      this.parents = parents;
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

   public List<FunctionTask> getParents() {
      return parents;
   }

   @Override
   public void process() {

      if (documents != null && collection != null) {
         documents.forEach(document -> {
            final FunctionTaskExecutor executor = new FunctionTaskExecutor(this, collection, document);
            executor.execute();
         });
      } else if (linkType != null && linkInstances != null) {
         // TODO
      }

      if (parents != null) {
         parents.forEach(FunctionTask::process);
      }
   }
}
