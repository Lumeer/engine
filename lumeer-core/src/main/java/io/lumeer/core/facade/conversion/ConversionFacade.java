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
package io.lumeer.core.facade.conversion;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintConverter;
import io.lumeer.core.constraint.ConstraintConverterFactory;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.ReloadResourceContent;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class ConversionFacade {

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private Event<ReloadResourceContent> reloadResourceContentEvent;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   private ConstraintManager constraintManager;

   private ConstraintConverterFactory constraintConverterFactory;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      constraintConverterFactory = new ConstraintConverterFactory(constraintManager, requestDataKeeper.getUserLocale());
   }

   public void convertStoredDocuments(final Collection collection, final Attribute originalAttribute, final Attribute newAttribute) {
      if (areConstraintsDifferent(originalAttribute, newAttribute)) {
         if (originalAttribute.getConstraint() == null || newAttribute.getConstraint() == null) { // for now, we only proceed for conversions NONE -> SOME, SOME -> NONE

            final ConstraintConverter converter = constraintConverterFactory.getConstraintConverter(originalAttribute, newAttribute);

            if (converter != null) {

               final List<Document> documents = documentDao.getDocumentsByCollection(collection.getId());

               if (documents.size() < 1_000_000) { // only if the number of documents is manageable

                  documents.forEach(doc -> {
                     final DataDocument update = getConversionUpdate(converter, dataDao.getData(collection.getId(), doc.getId()));

                     if (update != null && update.size() > 0) {
                        dataDao.patchData(collection.getId(), doc.getId(), update);
                     }
                  });

                  if (reloadResourceContentEvent != null) {
                     reloadResourceContentEvent.fire(new ReloadResourceContent(collection));
                  }
               }

            }
         }
      }
   }

   private boolean areConstraintsDifferent(final Attribute originalAttribute, final Attribute newAttribute) {
      if (originalAttribute.getConstraint() == null && newAttribute.getConstraint() == null) {
         return false;
      }

      if (originalAttribute.getConstraint() != null && newAttribute.getConstraint() != null) {
         if (originalAttribute.getConstraint().getType() == newAttribute.getConstraint().getType()) {
            return false;
         }
      }

      return true;
   }

   private DataDocument getConversionUpdate(final ConstraintConverter constraintConverter, final DataDocument originalDocument) {
      return constraintConverter.getPatchDocument(originalDocument);
   }
}
