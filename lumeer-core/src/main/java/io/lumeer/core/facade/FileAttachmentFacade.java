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
package io.lumeer.core.facade;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.core.adapter.FileAttachmentAdapter;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.LinkInstanceUtils;
import io.lumeer.core.util.LumeerS3Client;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.StorageException;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class FileAttachmentFacade extends AbstractFacade {

   private LumeerS3Client lumeerS3Client = null;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private FileAttachmentDao fileAttachmentDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   private FileAttachmentAdapter adapter;

   @PostConstruct
   public void init() {
      lumeerS3Client = new LumeerS3Client(configurationProducer);
      adapter = new FileAttachmentAdapter(lumeerS3Client, fileAttachmentDao, configurationProducer.getEnvironment().name());
   }

   public List<FileAttachment> createFileAttachments(final List<FileAttachment> fileAttachments) {
      checkFileAttachmentsCanEdit(fileAttachments);

      return fileAttachmentDao.createFileAttachments(fileAttachments).stream()
                              .map(fileAttachment -> presignFileAttachment(fileAttachment, true))
                              .collect(Collectors.toList());
   }

   private void checkFileAttachmentsCanEdit(final List<FileAttachment> fileAttachments) {
      Set<String> checkedDocumentIds = new HashSet<>();
      Set<String> checkedLinkInstanceIds = new HashSet<>();

      fileAttachments.forEach(fileAttachment -> {
         if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
            if (!checkedDocumentIds.contains(fileAttachment.getDocumentId())) {
               checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
               checkedDocumentIds.add(fileAttachment.getDocumentId());
            }
         } else if (!checkedLinkInstanceIds.contains(fileAttachment.getDocumentId())) {
            checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
            checkedLinkInstanceIds.add(fileAttachment.getDocumentId());
         }
      });
   }

   public FileAttachment createFileAttachment(final FileAttachment fileAttachment) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      } else {
         checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      }

      return presignFileAttachment(fileAttachmentDao.createFileAttachment(fileAttachment), true);
   }

   public FileAttachment createFileAttachment(final FileAttachment fileAttachment, final byte[] data) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      } else {
         checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      }

      return adapter.createFileAttachment(fileAttachment, data);
   }

   public FileAttachment getFileAttachment(final String fileAttachmentId, final boolean write) {
      final FileAttachment fileAttachment = fileAttachmentDao.findFileAttachment(fileAttachmentId);

      if (!permissionsChecker.isPublic()) {
         if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
            checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
         } else {
            checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
         }
      }

      return presignFileAttachment(fileAttachment, write);
   }

   public List<FileAttachment> getAllFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanReadDocument(collectionId, documentId);
      } else {
         checkCanReadLinkInstance(collectionId, documentId);
      }

      return adapter.getAllFileAttachments(
                          getOrganization(),
                          getProject(),
                          collectionId, documentId, attributeId, type)
                    .stream()
                    .map(fa -> presignFileAttachment(fa, false))
                    .collect(Collectors.toList());
   }

   protected void duplicateFileAttachments(final String collectionId, final Map<String, String> sourceTargetIdMap, final FileAttachment.AttachmentType type) {
      // we don't have to check permissions because method is not called from service

      sourceTargetIdMap.forEach((sourceId, targetId) -> {
         List<FileAttachment> fileAttachments = fileAttachmentDao.findAllFileAttachments(
               getOrganization(),
               getProject(),
               collectionId, sourceId, type);

         fileAttachments.forEach(fa -> {
            final FileAttachment targetFileAttachment = new FileAttachment(fa);
            targetFileAttachment.setDocumentId(targetId);

            copyFileAttachment(fa, targetFileAttachment);
         });
      });
   }

   public List<FileAttachment> getAllFileAttachments(final String collectionId, final String documentId, final FileAttachment.AttachmentType type) {
      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanReadDocument(collectionId, documentId);
      } else {
         checkCanReadLinkInstance(collectionId, documentId);
      }

      return fileAttachmentDao.findAllFileAttachments(
                                    getOrganization(),
                                    getProject(),
                                    collectionId, documentId, type)
                              .stream()
                              .map(fa -> presignFileAttachment(fa, false))
                              .collect(Collectors.toList());
   }

   public List<FileAttachment> getAllFileAttachments(final String resourceId, final FileAttachment.AttachmentType type) {
      List<FileAttachment> attachments = fileAttachmentDao.findAllFileAttachments(getOrganization(), getProject(), resourceId, type)
                                                          .stream()
                                                          .map(fa -> presignFileAttachment(fa, false))
                                                          .collect(Collectors.toList());

      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         return filterDocumentsAttachments(attachments, resourceId);
      } else {
         return filterLinkAttachments(attachments, resourceId);
      }
   }

   private List<FileAttachment> filterDocumentsAttachments(final List<FileAttachment> attachments, final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      Set<String> documentIds = attachments.stream().map(FileAttachment::getDocumentId).collect(Collectors.toSet());
      List<Document> documents = documentDao.getDocumentsByCollection(collectionId, documentIds);
      Map<String, Document> documentsMap = DocumentUtils.loadDocumentsData(dataDao, collection, documents).stream().collect(Collectors.toMap(Document::getId, doc -> doc));

      return attachments.stream()
                        .filter(fileAttachment -> permissionsChecker.canReadDocument(collection, documentsMap.get(fileAttachment.getDocumentId())))
                        .collect(Collectors.toList());
   }

   private List<FileAttachment> filterLinkAttachments(final List<FileAttachment> attachments, final String linkTypeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      Set<String> linkIds = attachments.stream().map(FileAttachment::getDocumentId).collect(Collectors.toSet());
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(linkIds);
      Map<String, LinkInstance> linkInstanceMap = LinkInstanceUtils.loadLinkInstancesData(linkDataDao, linkType, linkInstances).stream().collect(Collectors.toMap(LinkInstance::getId, doc -> doc));

      return attachments.stream()
                        .filter(fileAttachment -> permissionsChecker.canReadLinkInstance(linkType, linkInstanceMap.get(fileAttachment.getDocumentId())))
                        .collect(Collectors.toList());
   }

   public FileAttachment renameFileAttachment(final FileAttachment fileAttachment) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      } else {
         checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      }

      final FileAttachment storedFileAttachment = fileAttachmentDao.findFileAttachment(fileAttachment);

      if (storedFileAttachment.getFileName().equals(fileAttachment.getFileName())) {
         return fileAttachment;
      }

      checkFileAttachmentName(fileAttachment);

      storedFileAttachment.setFileName(fileAttachment.getFileName());

      return fileAttachmentDao.updateFileAttachment(fileAttachment);
   }

   public void removeFileAttachments(final java.util.Collection<String> fileAttachmentIds) {
      final List<FileAttachment> fileAttachments = fileAttachmentDao.findFileAttachments(fileAttachmentIds);

      checkFileAttachmentsCanEdit(fileAttachments);

      adapter.removeFileAttachments(fileAttachments);
   }

   public void removeFileAttachment(final String fileAttachmentId) {
      final FileAttachment fileAttachment = fileAttachmentDao.findFileAttachment(fileAttachmentId);

      if (fileAttachment != null) {
         removeFileAttachment(fileAttachment);
      } else {
         throw new InvalidValueException("File attachment with the given ID was not found.");
      }
   }

   public void removeFileAttachment(final FileAttachment fileAttachment) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanEditDocument(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      } else {
         checkCanEditLinkInstance(fileAttachment.getCollectionId(), fileAttachment.getDocumentId());
      }

      adapter.removeFileAttachment(fileAttachment);
   }

   private FileAttachment presignFileAttachment(final FileAttachment fileAttachment, final boolean write) {
      if (!lumeerS3Client.isInitialized()) {
         return fileAttachment;
      }

      final String key = adapter.getFileAttachmentKey(fileAttachment);
      final URI uri = lumeerS3Client.presign(key, write);

      fileAttachment.setPresignedUrl(uri.toString());

      return fileAttachment;
   }

   /**
    * Lists the file attachments really present in the S3 bucket.
    *
    * @param collectionId ID of a collection.
    * @param documentId   ID of a document.
    * @param attributeId  ID of an attribute.
    * @param type         the type of file attachment.
    * @return Files present in S3 bucket for the specified collection, document and its attribute.
    */
   public List<FileAttachment> listFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (!lumeerS3Client.isInitialized()) {
         return Collections.emptyList();
      }

      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCanReadDocument(collectionId, documentId);
      } else {
         checkCanReadLinkInstance(collectionId, documentId);
      }

      final String organizationId = getOrganization().getId();
      final String projectId = getProject().getId();

      return lumeerS3Client.listObjects(
            adapter.getFileAttachmentLocation(
                  organizationId,
                  projectId,
                  collectionId,
                  documentId,
                  attributeId,
                  type)
      ).stream().map(s3ObjectItem -> {
         final FileAttachment fileAttachment = new FileAttachment(organizationId, projectId, collectionId, documentId, attributeId, s3ObjectItem.getKey(), type);
         fileAttachment.setSize(s3ObjectItem.getSize());

         return fileAttachment;
      }).collect(Collectors.toList());
   }

   private FileAttachment copyFileAttachment(final FileAttachment sourceFileAttachment, final FileAttachment targetFileAttachment) {
      targetFileAttachment.setFileName(sourceFileAttachment.getFileName());

      final FileAttachment result = fileAttachmentDao.createFileAttachment(targetFileAttachment);
      copyFileAttachmentData(sourceFileAttachment, result);

      return result;
   }

   private void copyFileAttachmentData(final FileAttachment sourceFileAttachment, final FileAttachment targetFileAttachment) {
      if (lumeerS3Client.isInitialized()) {
         lumeerS3Client.copyObject(adapter.getFileAttachmentKey(sourceFileAttachment), adapter.getFileAttachmentKey(targetFileAttachment));
      }
   }

   void removeAllFileAttachments(final String collectionId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeAllFileAttachments(collectionId, null, null, type);

      fileAttachmentDao.removeAllFileAttachments(getOrganization(), getProject(), collectionId, type);
   }

   void removeAllFileAttachments(final String collectionId, final String attributeId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeFileAttachments(collectionId, null, attributeId, type);

      fileAttachmentDao.removeAllFileAttachments(getOrganization(), getProject(), collectionId, attributeId, type);
   }

   void removeAllFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeFileAttachments(collectionId, documentId, attributeId, type);

      fileAttachmentDao.removeAllFileAttachments(getOrganization(), getProject(), collectionId, documentId, attributeId, type);
   }

   private void removeFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (lumeerS3Client.isInitialized()) {
         final String organizationId = getOrganization().getId();
         final String projectId = getProject().getId();

         adapter.removeFileAttachments(adapter.getFileAttachmentLocation(
                     organizationId,
                     projectId,
                     collectionId,
                     documentId,
                     attributeId,
                     type
               )
         );
      }
   }

   private void checkCanEditDocument(final String collectionId, final String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, documentId);

      permissionsChecker.checkEditDocument(collection, document);
   }

   private void checkCanReadDocument(final String collectionId, final String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, documentId);

      permissionsChecker.checkReadDocument(collection, document);
   }

   private void checkCanEditLinkInstance(final String linkTypeId, final String linkInstanceId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);

      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);
   }

   private void checkCanReadLinkInstance(final String linkTypeId, final String linkInstanceId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);

      permissionsChecker.checkReadLinkInstance(linkType, linkInstance);
   }

   private void checkFileAttachmentName(final FileAttachment fileAttachment) {
      if (fileAttachment.getFileName() == null || "".equals(fileAttachment.getFileName())) {
         throw new StorageException("Cannot store a FileAttachment with an empty file name");
      }
   }
}
