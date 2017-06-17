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
package io.lumeer.utils;

import io.vertx.core.MultiMap;

import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RequestDto {

   private String method;

   private String content;

   private MultiMap headers;

   public RequestDto(final String method, final String content, final MultiMap headers) {
      this.method = method;
      this.content = content;
      this.headers = headers;
   }

   public String getMethod() {
      return method;
   }

   public String getContent() {
      return content;
   }

   public MultiMap getHeaders() {
      return headers;
   }

   @Override
   public String toString() {
      return "RequestDto{" +
            "method='" + method + '\'' +
            ", content='" + content + '\'' +
            ", headers={\n  " + headers.entries().stream().map(t -> t.toString()).collect(Collectors.joining(",\n  ")) +
            "\n  }\n}";
   }
}
