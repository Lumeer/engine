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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class JsonResource implements Comparable<JsonResource> {

   private static final int DEFAULT_ORDER = 99_999;

   private final int order;

   private final File path;

   public JsonResource(final String fileName) {
      this.path = new File(fileName);

      int o = DEFAULT_ORDER;
      final Matcher m = Pattern.compile("[0-9]+").matcher(fileName);
      if (m.find()) {
         try {
            o = Integer.parseInt(m.group());
         } catch (NumberFormatException nfe) {
         }
      }

      this.order = o;
   }

   public int getOrder() {
      return order;
   }

   public File getPath() {
      return path;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final JsonResource that = (JsonResource) o;

      if (order != that.order) {
         return false;
      }
      return path != null ? path.equals(that.path) : that.path == null;
   }

   @Override
   public int hashCode() {
      int result = order;
      result = 31 * result + (path != null ? path.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "JsonResource{" +
            "order=" + order +
            ", path=" + path +
            '}';
   }

   @Override
   public int compareTo(final JsonResource o) {
      return this.order - o.order;
   }
}
