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
package io.lumeer.core.util.s3;

import java.time.Duration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

public class PresignUrlRequest implements ToCopyableBuilder<PresignUrlRequest.Builder, PresignUrlRequest> {
   private final AwsCredentialsProvider credentialsProvider;
   private final SdkHttpMethod httpMethod;
   private final Region region;
   private final String bucket;
   private final String key;
   private final Duration signatureDuration;
   private final String endpoint;

   private PresignUrlRequest(Builder builder) {
      this.credentialsProvider = Validate.notNull(builder.credentialsProvider, "credentialsProvider");
      this.httpMethod = Validate.notNull(builder.httpMethod, "httpMethod");
      this.region = Validate.notNull(builder.region, "region");
      this.bucket = Validate.notNull(builder.bucket, "bucket");
      this.key = Validate.notNull(builder.key, "key");
      this.endpoint = Validate.notEmpty(builder.endpoint, "endpoint");
      this.signatureDuration = builder.signatureDuration;
   }

   public static Builder builder() {
      return new Builder();
   }

   public AwsCredentialsProvider credentialsProvider() {
      return credentialsProvider;
   }

   public SdkHttpMethod httpMethod() {
      return httpMethod;
   }

   public Region region() {
      return region;
   }

   public String bucket() {
      return bucket;
   }

   public String key() {
      return key;
   }

   public Duration signatureDuration() {
      return signatureDuration;
   }

   public String endpoint() {
      return endpoint;
   }

   @Override
   public Builder toBuilder() {
      return builder()
            .credentialsProvider(credentialsProvider)
            .region(region)
            .bucket(bucket)
            .key(key)
            .endpoint(endpoint)
            .signatureDuration(signatureDuration);
   }

   public static class Builder implements CopyableBuilder<Builder, PresignUrlRequest> {
      private AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
      private SdkHttpMethod httpMethod = SdkHttpMethod.GET;
      private Region region;
      private String bucket;
      private String key;
      private Duration signatureDuration;
      private String endpoint;

      public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
         this.credentialsProvider = credentialsProvider;
         return this;
      }

      public Builder httpMethod(SdkHttpMethod httpMethod) {
         this.httpMethod = httpMethod;
         return this;
      }

      public Builder region(Region region) {
         this.region = region;
         return this;
      }

      public Builder bucket(String bucket) {
         this.bucket = bucket;
         return this;
      }

      public Builder key(String key) {
         this.key = key;
         return this;
      }

      public Builder signatureDuration(Duration signatureDuration) {
         this.signatureDuration = signatureDuration;
         return this;
      }

      public Builder endpoint(String endpoint) {
         this.endpoint = endpoint;
         return this;
      }

      @Override
      public PresignUrlRequest build() {
         return new PresignUrlRequest(this);
      }
   }
}
