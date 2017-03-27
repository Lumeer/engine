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
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Can obtain a new unique number in a named row (sequence).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@SessionScoped
public class SequenceFacade implements Serializable {

   /**
    * The name of the sequences collection.
    */
   private static final String SEQUENCE_COLLECTION = "_sequences";

   /**
    * The name of the document attribute to find sequences by.
    */
   private static final String SEQUENCE_INDEX_ATTR = "name";

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   /**
    * Initializes collections needed for storing sequences.
    */
   @PostConstruct
   public void init() {
      if (!systemDataStorage.hasCollection(SEQUENCE_COLLECTION)) {
         systemDataStorage.createCollection(SEQUENCE_COLLECTION);
         systemDataStorage.createIndex(SEQUENCE_COLLECTION, new DataDocument(SEQUENCE_INDEX_ATTR, LumeerConst.Index.ASCENDING), false);
      }
   }

   /**
    * Gets the next value of the given sequence.
    *
    * @param sequenceName
    *       The name of the sequence.
    * @return The next value of the sequence.
    */
   public int getNext(final String sequenceName) {
      return systemDataStorage.getNextSequenceNo(SEQUENCE_COLLECTION, SEQUENCE_INDEX_ATTR, sequenceName);
   }

   /**
    * Resets the sequence to zero.
    *
    * @param sequenceName
    *       The name of the sequence to reset.
    */
   public void resetSequence(final String sequenceName) {
      systemDataStorage.resetSequence(SEQUENCE_COLLECTION, SEQUENCE_INDEX_ATTR, sequenceName);
   }

}
