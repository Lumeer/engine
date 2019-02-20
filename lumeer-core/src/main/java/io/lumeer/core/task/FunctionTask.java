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
import io.lumeer.api.model.function.Function;
import io.lumeer.core.task.executor.FunctionTaskExecutor;

import java.util.Set;

public class FunctionTask extends AbstractContextualTask {

   private Attribute attribute;
   private Collection collection;
   private Set<Document> documents;
   private FunctionTask parent;

   public void setFunctionTask(final Attribute attribute, final Collection collection, final Set<Document> documents, final FunctionTask parent) {
      this.attribute = attribute;
      this.collection = collection;
      this.documents = documents;
      this.parent = parent;
   }

   public Function getFunction() {
      return attribute.getFunction();
   }

   public Attribute getAttribute() {
      return attribute;
   }

   @Override
   public void process() {

      documents.forEach(document -> {
         final FunctionTaskExecutor executor = new FunctionTaskExecutor(this, collection, document);
         executor.execute();
      });

      if (parent != null) {
         parent.process();
      }
   }
}
