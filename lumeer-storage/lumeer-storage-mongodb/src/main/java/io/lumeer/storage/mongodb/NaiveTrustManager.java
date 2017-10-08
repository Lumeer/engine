/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.storage.mongodb;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NaiveTrustManager implements X509TrustManager {

   private static SSLSocketFactory sslSocketFactory;

   /**
    * Doesn't throw an exception, so this is how it approves a certificate.
    *
    * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], String)
    **/
   public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
   }

   /**
    * Doesn't throw an exception, so this is how it approves a certificate.
    *
    * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], String)
    **/
   public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
   }

   /**
    * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
    **/
   public X509Certificate[] getAcceptedIssuers() {
      return null;  // I've seen someone return new X509Certificate[ 0 ];
   }

   public static final SSLSocketFactory getSocketFactory() {
      if (sslSocketFactory == null) {
         try {
            TrustManager[] tm = new TrustManager[] { new NaiveTrustManager() };
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(new KeyManager[0], tm, new SecureRandom());

            sslSocketFactory = context.getSocketFactory();
         } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
         }
      }
      return sslSocketFactory;
   }
}
