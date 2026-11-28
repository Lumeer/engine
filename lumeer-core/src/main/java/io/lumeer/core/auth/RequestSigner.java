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
package io.lumeer.core.auth;

import io.lumeer.api.model.User;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.storage.api.dao.UserDao;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs and verifies short-term URLs that carry their authorization in the query string,
 * so that a browser can open them directly (e.g. a download link) without the Authorization header.
 * The signature covers the path, the user and the expiration time.
 */
@ApplicationScoped
public class RequestSigner {

   public static final String USER_PARAM = "user";
   public static final String EXPIRES_PARAM = "expires";
   public static final String SIGNATURE_PARAM = "signature";

   private static final String ALGORITHM = "HmacSHA256";

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private UserDao userDao;

   /**
    * Creates the query string authorizing the user to GET the path until the given time.
    *
    * @param path    the path relative to the REST root, as returned by {@link HttpServletRequest#getPathInfo()}
    * @param userId  the user the request will run as
    * @param expires the time after which the URL stops working
    * @return the query string without the leading question mark
    * @throws IllegalStateException when the signing secret is not configured
    */
   public String sign(final String path, final String userId, final Instant expires) {
      final long expiresSeconds = expires.getEpochSecond();

      return USER_PARAM + "=" + URLEncoder.encode(userId, StandardCharsets.UTF_8) +
            "&" + EXPIRES_PARAM + "=" + expiresSeconds +
            "&" + SIGNATURE_PARAM + "=" + signature(path, userId, expiresSeconds);
   }

   public boolean isSigned(final HttpServletRequest req) {
      return "GET".equals(req.getMethod()) && req.getPathInfo() != null && req.getParameter(SIGNATURE_PARAM) != null;
   }

   /**
    * Verifies the signed request.
    *
    * @return the user the request was signed for, or null when the signature is invalid, the URL expired, or the user does not exist
    */
   public User verify(final HttpServletRequest req) {
      final String userId = req.getParameter(USER_PARAM);
      final String expires = req.getParameter(EXPIRES_PARAM);
      final String signature = req.getParameter(SIGNATURE_PARAM);

      if (StringUtils.isAnyEmpty(userId, expires, signature) || !StringUtils.isNumeric(expires) || getSecret() == null) {
         return null;
      }

      final long expiresSeconds = Long.parseLong(expires);
      if (Instant.now().getEpochSecond() > expiresSeconds) {
         return null;
      }

      final byte[] expected = signature(req.getPathInfo(), userId, expiresSeconds).getBytes(StandardCharsets.UTF_8);
      if (!MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8))) {
         return null;
      }

      try {
         return userDao.getUserById(userId);
      } catch (RuntimeException e) {
         return null;
      }
   }

   private String signature(final String path, final String userId, final long expiresSeconds) {
      final String secret = getSecret();
      if (secret == null) {
         throw new IllegalStateException("Signing secret (" + DefaultConfigurationProducer.SIGNING_SECRET + ") is not configured");
      }

      try {
         final Mac mac = Mac.getInstance(ALGORITHM);
         mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
         final byte[] digest = mac.doFinal((path + "\n" + userId + "\n" + expiresSeconds).getBytes(StandardCharsets.UTF_8));
         return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
      } catch (GeneralSecurityException e) {
         throw new IllegalStateException("Unable to sign the request", e);
      }
   }

   private String getSecret() {
      return StringUtils.defaultIfEmpty(configurationProducer.get(DefaultConfigurationProducer.SIGNING_SECRET), null);
   }
}
