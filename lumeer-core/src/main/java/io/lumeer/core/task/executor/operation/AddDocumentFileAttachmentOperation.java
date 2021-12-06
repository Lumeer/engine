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
import io.lumeer.core.task.executor.operation.data.FileAttachmentData;
import io.lumeer.engine.api.data.DataDocument;

public class AddDocumentFileAttachmentOperation extends ResourceOperation<Document> {

   private final DocumentOperation relatedUpdate;

   public AddDocumentFileAttachmentOperation(final Document entity, final String attrId, final FileAttachmentData value, final DocumentOperation relatedUpdate) {
      super(entity, attrId, value);
      this.relatedUpdate = relatedUpdate;
   }

   public FileAttachmentData getFileAttachmentData() {
      return (FileAttachmentData) getValue();
   }

   public void updateRelatedValue(final Object value) {
      relatedUpdate.updateValue(value);

      if (relatedUpdate.entity != null) {
         if (relatedUpdate.entity.getData() != null) {
            relatedUpdate.entity.getData().append(getAttrId(), value);
         } else {
            relatedUpdate.entity.setData(new DataDocument().append(getAttrId(), value));
         }
      }
   }
}
