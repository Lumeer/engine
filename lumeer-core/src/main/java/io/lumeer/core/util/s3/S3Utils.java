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

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;

import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Utils {

   public static URI presign(final PresignUrlRequest request) {
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
                               .signingName(S3Client.SERVICE_NAME)
                               .signingRegion(request.region())
                               .build();

      return AwsS3V4Signer.create().presign(httpRequest, presignRequest).getUri();
   }

}
