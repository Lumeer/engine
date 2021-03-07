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
package io.lumeer.core.task.executor.operation;

import io.lumeer.api.model.Document;
import io.lumeer.core.task.executor.ResourceOperation;

public class DocumentOperation extends ResourceOperation<Document> {

   private final Document originalDocument;

   public DocumentOperation(final Document entity, final String attrId, final Object value) {
      super(entity, attrId, value);
      originalDocument = new Document(entity);
   }

   public Document getOriginalDocument() {
      return originalDocument;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "{" +
            "entity=" + entity +
            ", attrId='" + getAttrId() + '\'' +
            ", value=" + getValue() +
            ", originalDocument=" + originalDocument +
            '}';
   }
}
