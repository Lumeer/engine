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
package io.lumeer.storage.api.query;

import javax.annotation.concurrent.Immutable;

@Immutable
public class SuggestionQuery extends DatabaseQuery {

   private final String text;

   private SuggestionQuery(final Builder builder) {
      super(builder);

      this.text = builder.text;
   }

   public String getText() {
      return text;
   }

   public static Builder createBuilder(String user) {
      return new Builder(user);
   }

   public static class Builder extends DatabaseQuery.Builder<SuggestionQuery.Builder> {

      private String text;

      private Builder(final String user) {
         super(user);
      }

      public Builder text(String text) {
         this.text = text;
         return this;
      }

      public SuggestionQuery build() {
         validate();

         return new SuggestionQuery(this);
      }
   }
}
