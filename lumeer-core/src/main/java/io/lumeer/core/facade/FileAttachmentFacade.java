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
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Role;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.s3.PresignUrlRequest;
import io.lumeer.core.util.s3.S3Utils;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.StorageException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

@RequestScoped
public class FileAttachmentFacade extends AbstractFacade {

   private static final int PRESIGN_TIMEOUT = 60;
   private String S3_KEY;
   private String S3_SECRET;
   private String S3_BUCKET;
   private String S3_REGION;
   private String S3_ENDPOINT;

   private Region region;
   private AwsCredentials awsCredentials;
   private StaticCredentialsProvider staticCredentialsProvider;
   private S3Client s3 = null;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private FileAttachmentDao fileAttachmentDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private ConfigurationFacade configurationFacade;

   @PostConstruct
   public void init() {
      S3_KEY = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_KEY)).orElse("");
      S3_SECRET = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_SECRET)).orElse("");
      S3_BUCKET = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_BUCKET)).orElse("");
      S3_REGION = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_REGION)).orElse("");
      S3_ENDPOINT = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_ENDPOINT)).orElse("");

      if (S3_KEY != null && !"".equals(S3_KEY)) {
         region = Region.of(S3_REGION);
         awsCredentials = AwsBasicCredentials.create(S3_KEY, S3_SECRET);
         staticCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);
         try {
            s3 = S3Client
                    .builder()
                    .region(region)
                    .endpointOverride(new URI("https://" + S3_REGION + "." + S3_ENDPOINT))
                    .credentialsProvider(staticCredentialsProvider)
                    .build();
         } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to initialize S3 client. Wrong endpoint. Unable to work with file attachments.");
         }
      }
   }

   public FileAttachment createFileAttachment(final FileAttachment fileAttachment) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionWritePermissions(fileAttachment.getCollectionId());
      } else {
         checkLinkTypeWritePermissions(fileAttachment.getCollectionId());
      }

      return presignFileAttachment(fileAttachmentDao.createFileAttachment(fileAttachment), true);
   }

   public FileAttachment getFileAttachment(final String fileAttachmentId, final boolean write) {
      final FileAttachment fileAttachment = fileAttachmentDao.findFileAttachment(fileAttachmentId);

      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionWritePermissions(fileAttachment.getCollectionId());
      } else {
         checkLinkTypeWritePermissions(fileAttachment.getCollectionId());
      }

      return presignFileAttachment(fileAttachment, write);
   }

   public List<FileAttachment> getAllFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionReadPermissions(collectionId);
      } else {
         checkLinkTypeReadPermissions(collectionId);
      }

      return fileAttachmentDao.findAllFileAttachments(
              workspaceKeeper.getOrganization().get(),
              workspaceKeeper.getProject().get(),
              collectionId, documentId, attributeId, type)
              .stream()
              .map(fa -> presignFileAttachment(fa, false))
              .collect(Collectors.toList());
   }

   public void duplicateFileAttachments(final String collectionId, final Map<String, String> sourceTargetIdMap, final FileAttachment.AttachmentType type) {
      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionReadPermissions(collectionId);
      } else {
         checkLinkTypeReadPermissions(collectionId);
      }

      sourceTargetIdMap.forEach((sourceId, targetId) -> {
         List<FileAttachment> fileAttachments = fileAttachmentDao.findAllFileAttachments(
               workspaceKeeper.getOrganization().get(),
               workspaceKeeper.getProject().get(),
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
         checkCollectionReadPermissions(collectionId);
      } else {
         checkLinkTypeReadPermissions(collectionId);
      }

      return fileAttachmentDao.findAllFileAttachments(
              workspaceKeeper.getOrganization().get(),
              workspaceKeeper.getProject().get(),
              collectionId, documentId, type)
              .stream()
              .map(fa -> presignFileAttachment(fa, false))
              .collect(Collectors.toList());
   }

   public List<FileAttachment> getAllFileAttachments(final String collectionId, final FileAttachment.AttachmentType type) {
      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionReadPermissions(collectionId);
      } else {
         checkLinkTypeReadPermissions(collectionId);
      }

      return fileAttachmentDao.findAllFileAttachments(
              workspaceKeeper.getOrganization().get(),
              workspaceKeeper.getProject().get(),
              collectionId,
              type)
              .stream()
              .map(fa -> presignFileAttachment(fa, false))
              .collect(Collectors.toList());
   }

   public FileAttachment renameFileAttachment(final FileAttachment fileAttachment) {
      if (fileAttachment.getAttachmentType().equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionWritePermissions(fileAttachment.getCollectionId());
      } else {
         checkLinkTypeWritePermissions(fileAttachment.getCollectionId());
      }

      final FileAttachment storedFileAttachment = fileAttachmentDao.findFileAttachment(fileAttachment);

      if (storedFileAttachment.getFileName().equals(fileAttachment.getFileName())) {
         return fileAttachment;
      }

      checkFileAttachmentName(fileAttachment);

      storedFileAttachment.setFileName(fileAttachment.getFileName());

      return fileAttachmentDao.updateFileAttachment(fileAttachment);
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
         checkCollectionWritePermissions(fileAttachment.getCollectionId());
      } else {
         checkLinkTypeWritePermissions(fileAttachment.getCollectionId());
      }

      if (s3 != null) {
         s3.deleteObject(DeleteObjectRequest.builder().bucket(S3_BUCKET).key(getFileAttachmentKey(fileAttachment)).build());
      }

      fileAttachmentDao.removeFileAttachment(fileAttachment);
   }

   private FileAttachment presignFileAttachment(final FileAttachment fileAttachment, final boolean write) {
      if (s3 == null) {
         return fileAttachment;
      }

      final String key = getFileAttachmentKey(fileAttachment);
      final URI uri = S3Utils.presign(PresignUrlRequest.builder()
              .region(region)
              .bucket(S3_BUCKET)
              .key(key)
              .httpMethod(write ? SdkHttpMethod.PUT : SdkHttpMethod.GET)
              .signatureDuration(Duration.of(PRESIGN_TIMEOUT, ChronoUnit.SECONDS))
              .credentialsProvider(staticCredentialsProvider)
              .endpoint(S3_ENDPOINT)
              .build());

      fileAttachment.setPresignedUrl(uri.toString());

      return fileAttachment;
   }

   private String getFileAttachmentLocation(final String organizationId, final String projectId, final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      final StringBuilder sb = new StringBuilder(configurationFacade.getEnvironment().name() + "/" + organizationId + "/" + projectId + "/" + type.name());

      if (collectionId != null) {
         sb.append("/").append(collectionId);

         if (attributeId != null) {
            sb.append("/").append(attributeId);

            if (documentId != null) {
               sb.append("/").append(documentId);
            }
         }
      }

      return sb.toString();
   }

   private String getFileAttachmentKey(final FileAttachment fileAttachment) {
      return getFileAttachmentLocation(fileAttachment.getOrganizationId(), fileAttachment.getProjectId(), fileAttachment.getCollectionId(), fileAttachment.getDocumentId(), fileAttachment.getAttributeId(), fileAttachment.getAttachmentType()) + "/"
              + fileAttachment.getId();
   }

   /**
    * Lists the file attachments really present in S3 bucket.
    *
    * @param collectionId ID of a collection.
    * @param documentId   ID of a document.
    * @param attributeId  ID of an attribute.
    * @return Files present in S3 bucket for the specified collection, document and its attribute.
    */
   public List<FileAttachment> listFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (s3 == null) {
         return Collections.emptyList();
      }

      if (type.equals(FileAttachment.AttachmentType.DOCUMENT)) {
         checkCollectionReadPermissions(collectionId);
      } else {
         checkLinkTypeReadPermissions(collectionId);
      }

      final String organizationId = workspaceKeeper.getOrganization().get().getId();
      final String projectId = workspaceKeeper.getProject().get().getId();
      final ListObjectsV2Response response = s3.listObjectsV2(
              ListObjectsV2Request
                      .builder()
                      .encodingType("UTF-8")
                      .bucket(S3_BUCKET)
                      .prefix(
                              getFileAttachmentLocation(
                                      organizationId,
                                      projectId,
                                      collectionId,
                                      documentId,
                                      attributeId,
                                      type)
                      ).build());

      return response.contents().stream().map(s3Object -> {
         final FileAttachment fileAttachment = new FileAttachment(organizationId, projectId, collectionId, documentId, attributeId, s3Object.key(), type);
         fileAttachment.setSize(s3Object.size());

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
      if (s3 == null) {
         return;
      }

      s3.copyObject(
            CopyObjectRequest
                  .builder()
                  .copySource(S3_BUCKET + "/" + getFileAttachmentKey(sourceFileAttachment))
                  .bucket(S3_BUCKET)
                  .key(getFileAttachmentKey(targetFileAttachment))
                  .build()
      );
   }

   void removeAllFileAttachments(final String collectionId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeAllFileAttachments(collectionId, null, null, type);

      fileAttachmentDao.removeAllFileAttachments(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), collectionId, type);
   }

   void removeAllFileAttachments(final String collectionId, final String attributeId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeFileAttachments(collectionId, null, attributeId, type);

      fileAttachmentDao.removeAllFileAttachments(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), collectionId, attributeId, type);
   }

   void removeAllFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      // not checking access right - only have package access

      removeFileAttachments(collectionId, documentId, attributeId, type);

      fileAttachmentDao.removeAllFileAttachments(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), collectionId, documentId, attributeId, type);
   }

   private void removeFileAttachments(final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      if (s3 == null) {
         return;
      }

      final String organizationId = workspaceKeeper.getOrganization().get().getId();
      final String projectId = workspaceKeeper.getProject().get().getId();
      final ListObjectsV2Response response = s3.listObjectsV2(
              ListObjectsV2Request
                      .builder()
                      .encodingType("UTF-8")
                      .bucket(S3_BUCKET)
                      .prefix(
                              getFileAttachmentLocation(
                                      organizationId,
                                      projectId,
                                      collectionId,
                                      documentId,
                                      attributeId,
                                      type)
                      ).build());

      final Delete delete = Delete.builder().objects(response.contents().stream().map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build()).collect(Collectors.toList())).build();
      s3.deleteObjects(DeleteObjectsRequest.builder().bucket(S3_BUCKET).delete(delete).build());
   }

   private Collection checkCollectionWritePermissions(final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      return collection;
   }

   private Collection checkCollectionReadPermissions(final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      return collection;
   }

   private LinkType checkLinkTypeWritePermissions(String linkTypeId) {
      return checkLinkTypePermissions(linkTypeId, Role.WRITE);
   }

   private LinkType checkLinkTypeReadPermissions(String linkTypeId) {
      return checkLinkTypePermissions(linkTypeId, Role.READ);
   }

   private LinkType checkLinkTypePermissions(String linkTypeId, Role role) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      List<Collection> collections = collectionDao.getCollectionsByIds(linkType.getCollectionIds());
      for (Collection collection : collections) {
         permissionsChecker.checkRoleWithView(collection, role, role);
      }
      return linkType;
   }

   private void checkFileAttachmentName(final FileAttachment fileAttachment) {
      if (fileAttachment.getFileName() == null || "".equals(fileAttachment.getFileName())) {
         throw new StorageException("Cannot store a FileAttachment with an empty file name");
      }
   }
}
