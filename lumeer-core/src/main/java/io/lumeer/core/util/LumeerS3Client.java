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
package io.lumeer.core.util;

import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.s3.PresignUrlRequest;
import io.lumeer.core.util.s3.S3ObjectItem;

import org.apache.commons.lang3.StringUtils;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class LumeerS3Client {

   public static final int PRESIGN_TIMEOUT = 60;
   private String S3_KEY;
   private String S3_SECRET;
   private String S3_BUCKET;
   private String S3_REGION;
   private String S3_ENDPOINT;

   private Region region;
   private AwsCredentials awsCredentials;
   private StaticCredentialsProvider staticCredentialsProvider;
   private S3Client s3 = null;

   public LumeerS3Client(final DefaultConfigurationProducer configurationProducer) {
      S3_KEY = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_KEY)).orElse("");
      S3_SECRET = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_SECRET)).orElse("");
      S3_BUCKET = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_BUCKET)).orElse("");
      S3_REGION = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_REGION)).orElse("");
      S3_ENDPOINT = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.S3_ENDPOINT)).orElse("");

      if (StringUtils.isNotEmpty(S3_KEY)) {
         region = Region.of(S3_REGION);
         awsCredentials = AwsBasicCredentials.create(S3_KEY, S3_SECRET);
         staticCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);
         try {
            s3 = software.amazon.awssdk.services.s3.S3Client
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

   public boolean isInitialized() {
      return s3 != null;
   }

   public URI presign(final String key, final boolean write) {
      return LumeerS3Client.presign(PresignUrlRequest.builder()
                                                     .region(region)
                                                     .bucket(S3_BUCKET)
                                                     .key(key)
                                                     .httpMethod(write ? SdkHttpMethod.PUT : SdkHttpMethod.GET)
                                                     .signatureDuration(Duration.of(PRESIGN_TIMEOUT, ChronoUnit.SECONDS))
                                                     .credentialsProvider(staticCredentialsProvider)
                                                     .endpoint(S3_ENDPOINT)
                                                     .build());
   }

   public void putObject(final String key, final byte[] data) {
      s3.putObject(PutObjectRequest.builder().bucket(S3_BUCKET).key(key).build(), RequestBody.fromBytes(data));
   }

   public void deleteObject(final String key) {
      s3.deleteObject(DeleteObjectRequest.builder().bucket(S3_BUCKET).key(key).build());
   }

   public void deleteObjects(final List<S3ObjectItem> objects) {
      final Delete delete = Delete.builder().objects(objects.stream().map(s3ObjectItem -> ObjectIdentifier.builder().key(s3ObjectItem.getKey()).build()).collect(Collectors.toList())).build();
      s3.deleteObjects(DeleteObjectsRequest.builder().bucket(S3_BUCKET).delete(delete).build());
   }

   public List<S3ObjectItem> listObjects(final String prefix) {
      final ListObjectsV2Response response = s3.listObjectsV2(
            ListObjectsV2Request
                  .builder()
                  .encodingType("UTF-8")
                  .bucket(S3_BUCKET)
                  .prefix(prefix)
                  .build());

      return response.contents().stream().map(s3Object -> new S3ObjectItem(s3Object.key(), s3Object.size())).collect(Collectors.toList());
   }

   public byte[] readObject(final String key) {
      return s3.getObjectAsBytes(
            GetObjectRequest
                  .builder()
                  .bucket(S3_BUCKET)
                  .key(key)
                  .build()
      ).asByteArray();
   }

   public void copyObject(final String sourceKey, final String targetKey) {
      s3.copyObject(
            CopyObjectRequest
                  .builder()
                  .copySource(S3_BUCKET + "/" + sourceKey)
                  .destinationBucket(S3_BUCKET)
                  .destinationKey(targetKey)
                  .build()
      );
   }

   private static URI presign(final PresignUrlRequest request) {
      String encodedBucket, encodedKey;
      try {
         encodedBucket = URLEncoder.encode(request.bucket(), "UTF-8");
         encodedKey = URLEncoder.encode(request.key(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new UncheckedIOException(e);
      }

      SdkHttpFullRequest httpRequest =
            SdkHttpFullRequest.builder()
                              .method(request.httpMethod())
                              .protocol("https")
                              .host(encodedBucket + "." + request.region().id() + "." + request.endpoint())
                              .encodedPath(request.key())
                              .build();

      Instant expirationTime = request.signatureDuration() == null ? null : Instant.now().plus(request.signatureDuration());
      Aws4PresignerParams presignRequest =
            Aws4PresignerParams.builder()
                               .expirationTime(expirationTime)
                               .awsCredentials(request.credentialsProvider().resolveCredentials())
                               .signingName(software.amazon.awssdk.services.s3.S3Client.SERVICE_NAME)
                               .signingRegion(request.region())
                               .build();

      return AwsS3V4Signer.create().presign(httpRequest, presignRequest).getUri();
   }

}
