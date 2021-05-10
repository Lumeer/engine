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

import io.lumeer.api.model.common.WithId;
import io.lumeer.core.task.executor.operation.Operation;

import org.apache.commons.lang3.StringUtils;

public abstract class ResourceOperation<T extends WithId> extends Operation<T> {

   private final String attrId;
   private final Object value;

   public <R> ResourceOperation(final T entity, final String attrId, final R value) {
      super(entity);
      this.attrId = attrId;
      this.value = value;
   }

   public boolean isComplete() {
      return entity != null && StringUtils.isNotEmpty(attrId);
   }

   public String getAttrId() {
      return attrId;
   }

   public Object getValue() {
      return value;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "{" +
            "entity=" + getEntity().getId() +
            ", attrId='" + getAttrId() + '\'' +
            ", value=" + getValue() +
            '}';
   }
}
