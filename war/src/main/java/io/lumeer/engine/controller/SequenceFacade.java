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
import io.lumeer.engine.api.data.DataStorage;

import java.io.Serializable;
import java.util.Collections;
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

   private static final String SEQUENCE_COLLECTION = "_sequences";
   private static final String SEQUENCE_INDEX_ATTR = "name";

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @PostConstruct
   public void init() {
      if (!dataStorage.hasCollection(SEQUENCE_COLLECTION)) {
         dataStorage.createCollection(SEQUENCE_COLLECTION);
         dataStorage.createIndex(SEQUENCE_COLLECTION, Collections.singletonMap(SEQUENCE_INDEX_ATTR, "1"));
      }
   }

   public int getNext(final String sequenceName) {
      return dataStorage.getNextSequenceNo(SEQUENCE_COLLECTION, SEQUENCE_INDEX_ATTR, sequenceName);
   }

   public void resetSequence(final String sequenceName) {
      dataStorage.resetSequence(SEQUENCE_COLLECTION, SEQUENCE_INDEX_ATTR, sequenceName);
   }

}
