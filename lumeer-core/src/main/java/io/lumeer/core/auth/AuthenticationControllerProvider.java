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

import com.auth0.AuthenticationController;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Verification;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import jakarta.servlet.ServletConfig;

public abstract class AuthenticationControllerProvider {

   protected static AuthenticationController getInstance(ServletConfig config) throws UnsupportedEncodingException {
      String domain = config.getServletContext().getInitParameter("com.auth0.domain");
      String clientId = config.getServletContext().getInitParameter("com.auth0.clientId");
      String clientSecret = config.getServletContext().getInitParameter("com.auth0.clientSecret");

      return AuthenticationController.newBuilder(domain, clientId, clientSecret).build();
   }

   private static String readAll(Reader rd) throws IOException {
      StringBuilder sb = new StringBuilder();
      int cp;
      while ((cp = rd.read()) != -1) {
         sb.append((char) cp);
      }
      return sb.toString();
   }

   private static JSONObject readJsonFromUrl(String url) throws IOException, ParseException {
      final InputStream is = new URL(url).openStream();
      try {
         final BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
         final String jsonText = readAll(rd);
         final JSONParser parser = new JSONParser();
         final JSONObject json = (JSONObject) parser.parse(jsonText);
         return json;
      } finally {
         is.close();
      }
   }

   private static RSAPublicKey getPublicKeyFromCertificate(String certificate) throws GeneralSecurityException, IOException {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      X509Certificate cer = (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(certificate.getBytes("UTF-8")));
      return (RSAPublicKey) cer.getPublicKey();
   }

   protected static JWTVerifier getVerifier(final String domain) {
      try {
         final String pem = (String) ((JSONArray) ((JSONObject) ((JSONArray) AuthenticationControllerProvider.readJsonFromUrl("https://" + domain + "/.well-known/jwks.json")
                                                                                                             .get("keys"))
               .get(0))
               .get("x5c")).get(0);

         final StringBuilder sb = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
         int i = 0;
         while (i < pem.length()) {
            sb.append(pem, i, Math.min(i + 64, pem.length()));
            sb.append("\n");
            i += 64;
         }
         sb.append("-----END CERTIFICATE-----");
         final String pemN = sb.toString();

         final RSAPublicKey pubKey = AuthenticationControllerProvider.getPublicKeyFromCertificate(pemN);
         final Verification verification = JWT.require(Algorithm.RSA256(pubKey, null));
         final JWTVerifier verifier = verification.acceptExpiresAt(60).build();
         return verifier;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
}
