/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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
