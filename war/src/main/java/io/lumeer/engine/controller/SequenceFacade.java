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
