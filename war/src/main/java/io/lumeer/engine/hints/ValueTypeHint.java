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
package io.lumeer.engine.hints;

import io.lumeer.engine.api.data.DataDocument;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class ValueTypeHint implements Hint {

   private DataDocument dataDocument;
   private String userName;
   private String collectionName;
   private List<Object> objects;

   @Override
   public Hint call() throws Exception {
      if (dataDocument != null) {
         return testOneDocument(dataDocument);
      }
      return new ValueTypeHint();
   }

   private ValueTypeHint testOneDocument(DataDocument dataDocument) {
      Iterator<Map.Entry<String, Object>> iter = dataDocument.entrySet().iterator();
      Map<String, Object> documentMetadata = new HashMap<>();
      while (iter.hasNext()) {
         Map.Entry<String, Object> entry = iter.next();
         Object obj = entry.getKey();
         if (obj instanceof Integer) {
         }
         if (obj instanceof Boolean) {

         }
      }
      return new ValueTypeHint();
   }

   @Override
   public boolean isApplicable() {
      return true;
   }

   @Override
   public boolean apply() {
      return true;
   }

   @Override
   public void sendNotification() {
   }

   public void setDocument(DataDocument dataDocument) {
      this.dataDocument = dataDocument;
   }

   @Override
   public void setCollection(final String collectionName) {
      this.collectionName = collectionName;
   }

   @Override
   public void setUser(final String userName) {
      this.userName = userName;
   }
}
