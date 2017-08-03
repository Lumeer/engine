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
import io.lumeer.engine.api.push.PushMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class NormalForm implements Hint {

   private DataDocument dataDocument;
   private String userName;
   private String collectionName;
   private List<Object> objects;
   private Date date;

   private String wrongArg;

   @Override
   public Hint call() throws Exception {
      date = new Date();
      if (dataDocument != null) {
         return testOneDocument(dataDocument);
      }
      return new NormalForm();
   }

   private NormalForm testOneDocument(DataDocument dataDocument) {
      Iterator<Map.Entry<String, Object>> iter = dataDocument.entrySet().iterator();
      Map<String, Object> documentMetadata = new HashMap<>();
      while (iter.hasNext()) {
         Map.Entry<String, Object> entry = iter.next();
         if (entry.getValue().toString().contains(" ")){
            NormalForm nf = new NormalForm();
            nf.setWrongArg(entry.getKey());
            nf.setDocument(dataDocument);
            if (collectionName != null){
               nf.setCollection(collectionName);
            }
            if (userName != null){
               nf.setUser(userName);
            }
            return nf;
         }
      }
      return null;
   }

   @Override
   public boolean isApplicable() {
      return true;
   }

   @Override
   public boolean apply() {
      return true;
   }

   public void setWrongArg(String wrongArg){
      this.wrongArg = wrongArg;
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

   @Override
   public Date getCreateDate() {
      return date;
   }

   @Override
   public PushMessage getMessage() {
      return new PushMessage("Hint message","Hint","You have space in: " + wrongArg );
   }
}
