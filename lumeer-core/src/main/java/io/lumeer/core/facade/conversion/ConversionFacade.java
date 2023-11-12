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
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintConverter;
import io.lumeer.core.constraint.ConstraintConverterFactory;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FileAttachmentFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.ReloadLinkTypeContent;
import io.lumeer.engine.api.event.ReloadResourceContent;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class ConversionFacade {

   @Inject
   protected AuthenticatedUser authenticatedUser;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private Event<ReloadResourceContent> reloadResourceContentEvent;

   @Inject
   private Event<ReloadLinkTypeContent> reloadLinkTypeContentEvent;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   private ConstraintManager constraintManager;

   private ConstraintConverterFactory constraintConverterFactory;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      constraintConverterFactory = new ConstraintConverterFactory(constraintManager, requestDataKeeper.getUserLocale());
   }

   public void convertStoredDocuments(final Collection collection, final Attribute originalAttribute, final Attribute newAttribute) {
      if (areConstraintsDifferent(originalAttribute, newAttribute)) {

         removeFileAttachments(collection, originalAttribute, newAttribute);

         final ConstraintConverter converter = constraintConverterFactory.getConstraintConverter(originalAttribute, newAttribute);

         if (converter != null) {
            final List<Document> documents = documentDao.getDocumentsByCollection(collection.getId());

            if (documents.size() < 1_000_000) { // only if the number of documents is manageable

               documents.forEach(doc -> {
                  final DataDocument update = getConversionUpdate(converter, dataDao.getData(collection.getId(), doc.getId()));

                  if (update != null && update.size() > 0) {
                     dataDao.patchData(collection.getId(), doc.getId(), update);
                     updateDocument(doc);
                  }
               });

               if (reloadResourceContentEvent != null) {
                  reloadResourceContentEvent.fire(new ReloadResourceContent(collection));
               }
            }

            converter.close();
         }
      }
   }

   public void convertStoredDocuments(final LinkType linkType, final Attribute originalAttribute, final Attribute newAttribute) {
      if (areConstraintsDifferent(originalAttribute, newAttribute)) {

         removeFileAttachments(linkType, originalAttribute, newAttribute);

         final ConstraintConverter converter = constraintConverterFactory.getConstraintConverter(originalAttribute, newAttribute);

         if (converter != null) {
            final List<LinkInstance> links = linkInstanceDao.getLinkInstancesByLinkType(linkType.getId());

            if (links.size() < 1_000_000) { // only if the number of links is manageable

               links.forEach(l -> {
                  final DataDocument update = getConversionUpdate(converter, linkDataDao.getData(linkType.getId(), l.getId()));

                  if (update != null && update.size() > 0) {
                     linkDataDao.patchData(linkType.getId(), l.getId(), update);
                     updateLink(l);
                  }
               });

               if (reloadResourceContentEvent != null) {
                  reloadLinkTypeContentEvent.fire(new ReloadLinkTypeContent(linkType));
               }
            }

            converter.close();
         }
      }
   }

   private void removeFileAttachments(final Collection collection, final Attribute originalAttribute, final Attribute newAttribute) {
      if (originalAttribute.getConstraintType() == ConstraintType.FileAttachment && newAttribute.getConstraintType() != ConstraintType.FileAttachment) {
         fileAttachmentFacade.removeAllFileAttachments(collection.getId(), originalAttribute.getId(), FileAttachment.AttachmentType.DOCUMENT);
      }
   }

   private void removeFileAttachments(final LinkType linkType, final Attribute originalAttribute, final Attribute newAttribute) {
      if (originalAttribute.getConstraintType() == ConstraintType.FileAttachment && newAttribute.getConstraintType() != ConstraintType.FileAttachment) {
         fileAttachmentFacade.removeAllFileAttachments(linkType.getId(), originalAttribute.getId(), FileAttachment.AttachmentType.LINK);
      }
   }

   private void updateDocument(final Document document) {
      document.setUpdatedBy(authenticatedUser.getCurrentUserId());
      document.setUpdateDate(ZonedDateTime.now());

      documentDao.updateDocument(document.getId(), document);
   }

   private void updateLink(final LinkInstance link) {
      link.setUpdatedBy(authenticatedUser.getCurrentUserId());
      link.setUpdateDate(ZonedDateTime.now());

      linkInstanceDao.updateLinkInstance(link.getId(), link);
   }

   private boolean areConstraintsDifferent(final Attribute originalAttribute, final Attribute newAttribute) {
      return originalAttribute.getConstraintType() != newAttribute.getConstraintType() ||
            (originalAttribute.getConstraintType() == ConstraintType.Select && areSelectConstraintDifferent(originalAttribute, newAttribute));
   }

   @SuppressWarnings("unchecked")
   private boolean areSelectConstraintDifferent(final Attribute originalAttribute, final Attribute newAttribute) {
      var originalDisplayValues = ((Map<String, Object>) originalAttribute.getConstraint().getConfig()).get("displayValues");
      var newDisplayValues = ((Map<String, Object>) newAttribute.getConstraint().getConfig()).get("displayValues");

      if (originalDisplayValues == null && newDisplayValues == null) {
         return false;
      }

      if (originalDisplayValues != null && newDisplayValues != null) {
         return !originalDisplayValues.equals(newDisplayValues);
      }

      return true;
   }

   private DataDocument getConversionUpdate(final ConstraintConverter constraintConverter, final DataDocument originalDocument) {
      return constraintConverter.getPatchDocument(originalDocument);
   }
}
