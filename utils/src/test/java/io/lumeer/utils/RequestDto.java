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
