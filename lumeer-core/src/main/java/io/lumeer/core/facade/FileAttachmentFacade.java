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
import io.lumeer.api.model.Role;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.s3.PresignUrlRequest;
import io.lumeer.core.util.s3.S3Utils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.exception.StorageException;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private FileAttachmentDao fileAttachmentDao;

   @Inject
   private CollectionDao collectionDao;

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
      }
   }

   public FileAttachment createFileAttachment(final FileAttachment fileAttachment) {
      checkCollectionWritePermissions(fileAttachment.getCollectionId());

      return presignFileAttachment(fileAttachmentDao.createFileAttachment(fileAttachment), true);
   }

   public List<FileAttachment> getAllFileAttachments(final String collectionId, final String documentId, final String attributeId) {
      checkCollectionReadPermissions(collectionId);

      return fileAttachmentDao.findAllFileAttachments(
            workspaceKeeper.getOrganization().get(),
            workspaceKeeper.getProject().get(),
            collectionId, documentId, attributeId)
                              .stream()
                              .map(fa -> presignFileAttachment(fa, false))
                              .collect(Collectors.toList());
   }

   public FileAttachment renameFileAttachment(final FileAttachment fileAttachment) {
      checkCollectionWritePermissions(fileAttachment.getCollectionId());

      final FileAttachment storedFileAttachment = fileAttachmentDao.findFileAttachment(fileAttachment);

      if (storedFileAttachment.getFileName().equals(fileAttachment.getFileName())) {
         return fileAttachment;
      }

      checkFileAttachmentName(fileAttachment);

      storedFileAttachment.setFileName(fileAttachment.getFileName());

      return fileAttachmentDao.updateFileAttachment(fileAttachment);
   }

   public void removeFileAttachment(final FileAttachment fileAttachment) {
      checkCollectionWritePermissions(fileAttachment.getCollectionId());

      // S3 remove file

      fileAttachmentDao.removeFileAttachment(fileAttachment);
   }

   public FileAttachment presignFileAttachment(final FileAttachment fileAttachment, final boolean write) {
      final String key =
            fileAttachment.getOrganizationId() + "/"
                  + fileAttachment.getProjectId() + "/"
                  + fileAttachment.getCollectionId() + "/"
                  + fileAttachment.getAttributeId() + "/"
                  + fileAttachment.getDocumentId() + "/"
                  + fileAttachment.getId();

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

   // TODO: use other remove attachment methods at corresponding places in other facades

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

   private void checkFileAttachmentName(final FileAttachment fileAttachment) {
      if (fileAttachment.getFileName() == null || "".equals(fileAttachment.getFileName())) {
         throw new StorageException("Cannot store a FileAttachment with an empty file name");
      }
   }
}
