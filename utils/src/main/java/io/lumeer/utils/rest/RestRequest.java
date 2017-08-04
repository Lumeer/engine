/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.utils.rest;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.Optional;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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
