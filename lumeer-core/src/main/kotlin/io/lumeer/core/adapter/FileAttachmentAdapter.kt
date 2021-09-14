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
package io.lumeer.core.adapter

import io.lumeer.api.model.FileAttachment
import io.lumeer.api.model.FileAttachment.AttachmentType
import io.lumeer.core.facade.FileAttachmentFacade
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer
import io.lumeer.core.util.s3.PresignUrlRequest
import io.lumeer.core.util.s3.S3Utils
import org.apache.commons.lang3.StringUtils
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class FileAttachmentAdapter(val configurationProducer: DefaultConfigurationProducer) {

   val PRESIGN_TIMEOUT = 60
   private val S3_KEY: String
   private val S3_SECRET: String
   private val S3_BUCKET: String
   private val S3_REGION: String
   private val S3_ENDPOINT: String

   private var region: Region? = null
   private var awsCredentials: AwsCredentials? = null
   private var staticCredentialsProvider: StaticCredentialsProvider? = null
   private var s3: S3Client? = null

   init {
      S3_KEY = Optional.ofNullable(configurationProducer[DefaultConfigurationProducer.S3_KEY]).orElse("")
      S3_SECRET = Optional.ofNullable(configurationProducer[DefaultConfigurationProducer.S3_SECRET]).orElse("")
      S3_BUCKET = Optional.ofNullable(configurationProducer[DefaultConfigurationProducer.S3_BUCKET]).orElse("")
      S3_REGION = Optional.ofNullable(configurationProducer[DefaultConfigurationProducer.S3_REGION]).orElse("")
      S3_ENDPOINT = Optional.ofNullable(configurationProducer[DefaultConfigurationProducer.S3_ENDPOINT]).orElse("")

      if (StringUtils.isNotEmpty(S3_KEY)) {
         region = Region.of(S3_REGION)
         awsCredentials = AwsBasicCredentials.create(S3_KEY, S3_SECRET)
         staticCredentialsProvider = StaticCredentialsProvider.create(awsCredentials)
         s3 = try {
            S3Client
                  .builder()
                  .region(region)
                  .endpointOverride(URI("https://$S3_REGION.$S3_ENDPOINT"))
                  .credentialsProvider(staticCredentialsProvider)
                  .build()
         } catch (e: URISyntaxException) {
            throw IllegalStateException("Unable to initialize S3 client. Wrong endpoint. Unable to work with file attachments.")
         }
      }
   }

   private fun presignFileAttachment(fileAttachment: FileAttachment, write: Boolean): FileAttachment? {
      if (s3 == null) {
         return fileAttachment
      }
      val key = getFileAttachmentKey(fileAttachment)
      val uri = S3Utils.presign(PresignUrlRequest.builder()
            .region(region)
            .bucket(S3_BUCKET)
            .key(key)
            .httpMethod(if (write) SdkHttpMethod.PUT else SdkHttpMethod.GET)
            .signatureDuration(Duration.of(FileAttachmentFacade.PRESIGN_TIMEOUT.toLong(), ChronoUnit.SECONDS))
            .credentialsProvider(staticCredentialsProvider)
            .endpoint(S3_ENDPOINT)
            .build())
      fileAttachment.presignedUrl = uri.toString()
      return fileAttachment
   }

   private fun getFileAttachmentLocation(organizationId: String, projectId: String, collectionId: String?, documentId: String?, attributeId: String?, type: AttachmentType): String {
      val sb = StringBuilder(configurationProducer.environment.name + "/" + organizationId + "/" + projectId + "/" + type.name)
      if (collectionId != null) {
         sb.append("/").append(collectionId)
         if (attributeId != null) {
            sb.append("/").append(attributeId)
            if (documentId != null) {
               sb.append("/").append(documentId)
            }
         }
      }
      return sb.toString()
   }

   private fun getFileAttachmentKey(fileAttachment: FileAttachment): String {
      return (getFileAttachmentLocation(fileAttachment.organizationId, fileAttachment.projectId, fileAttachment.collectionId, fileAttachment.documentId, fileAttachment.attributeId, fileAttachment.attachmentType) + "/"
            + fileAttachment.id)
   }

}