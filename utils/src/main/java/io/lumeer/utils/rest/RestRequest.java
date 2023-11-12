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
package io.lumeer.utils.rest;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.Optional;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class RestRequest {

   private final Invocation invocation;
   private Response response;

   private RestRequest(final Invocation invocation) {
      this.invocation = invocation;
   }

   public static RestRequest json(final String url, final String path, final String method, final MultivaluedMap<String, Object> headers, final JSONObject content) {
      return jsonAware(url, path, method, headers, content);
   }

   public static RestRequest jsonArray(final String url, final String path, final String method, final MultivaluedMap<String, Object> headers, final JSONArray content) {
      return jsonAware(url, path, method, headers, content);
   }

   private static RestRequest jsonAware(final String url, final String path, final String method, final MultivaluedMap<String, Object> headers, final JSONAware content) {
      System.out.print(method + " " + path + ": ");

      return new RestRequest(ClientBuilder.newClient()
                                          .target(url)
                                          .path(path)
                                          .request(MediaType.APPLICATION_JSON_TYPE)
                                          .accept(MediaType.APPLICATION_JSON_TYPE)
                                          .headers(headers)
                                          .build(method, Entity.json(content.toJSONString())));
   }

   public static RestRequest xml(String url, String path, String method, MultivaluedMap<String, Object> headers, String content) {
      System.out.print(method + " " + path + ": ");

      return new RestRequest(ClientBuilder.newClient()
                                          .target(url)
                                          .path(path)
                                          .request(MediaType.APPLICATION_JSON_TYPE)
                                          .accept(MediaType.APPLICATION_JSON_TYPE)
                                          .headers(headers)
                                          .build(method, Entity.xml(content)));
   }

   public static RestRequest simple(String url, String path, String method, MultivaluedMap<String, Object> headers) {
      System.out.print(method + " " + path + ": ");

      return new RestRequest(ClientBuilder.newClient()
                                          .target(url)
                                          .path(path)
                                          .request(MediaType.APPLICATION_JSON_TYPE)
                                          .accept(MediaType.APPLICATION_JSON_TYPE)
                                          .headers(headers)
                                          .build(method));
   }

   public Response invoke() {
      this.response = invocation.invoke();
      return response;
   }

   public void finalizeResponse(final boolean verbose) {
      System.out.println(response.getStatus() + " " + response.getStatusInfo().getReasonPhrase());

      if (verbose) {
         System.out.println(Optional.ofNullable(response.getEntity()).orElse("").toString());
      }

      response.close();
   }
}
