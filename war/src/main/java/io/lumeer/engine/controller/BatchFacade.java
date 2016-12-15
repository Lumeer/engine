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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.batch.Batch;
import io.lumeer.engine.api.batch.MergeBatch;
import io.lumeer.engine.api.batch.SplitBatch;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Runs various types of batches.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@SessionScoped
public class BatchFacade implements Serializable {

   private static final long serialVersionUID = -6509744496908392550L;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   public void executeBatch(final Batch batch) throws DbException, InvalidConstraintException {
      // first check collection size
      if (dataStorage.count(batch.getCollectionName(), null) > 10000) {
         throw new UnsupportedOperationException("Cannot run a batch process on collections with more than 10000 documents.");
      }

      if (batch instanceof MergeBatch) {
         internalExecuteBatch((MergeBatch) batch);
      } else if (batch instanceof SplitBatch) {
         internalExecuteBatch((SplitBatch) batch);
      }
   }

   private void internalExecuteBatch(final MergeBatch batch) throws DbException, InvalidConstraintException {
      List<DataDocument> documents = dataStorage.search(batch.getCollectionName(), null, null, -1, -1);

      for (final DataDocument doc : documents) {
         if (batch.getMergeType() == MergeBatch.MergeType.JOIN) {
            final StringBuilder sb = new StringBuilder();

            batch.getAttributes().forEach(attr -> {
               final Object value = doc.get(attr);

               if (value != null) {
                  if (sb.length() > 0) {
                     sb.append(batch.getJoin());
                  }

                  sb.append(value);
               }
            });

            doc.put(batch.getResultAttribute(), sb.toString());
         } else if (batch.getMergeType() == MergeBatch.MergeType.SUM) {
            double sum = 0d;
            long longSum = 0l;
            BigDecimal bigDecimal = new BigDecimal("0");
            BigInteger bigInteger = new BigInteger("0");
            boolean wasFloating = false;
            boolean onlyBigDecimal = true;
            boolean onlyBigInteger = true;

            for (final String attr : batch.getAttributes()) {
               final Object value = doc.get(attr);

               if (value != null) {
                  if (value instanceof Double) {
                     sum = sum + (double) value;
                     wasFloating = true;
                     onlyBigDecimal = false;
                  } else if (value instanceof Integer) {
                     sum = sum + (int) value;
                     longSum = longSum + (int) value;
                     onlyBigDecimal = false;
                  } else if (value instanceof Long) {
                     sum = sum + (long) value;
                     longSum = longSum + (long) value;
                     onlyBigDecimal = false;
                  } else if (value instanceof Float) {
                     sum = sum + (float) value;
                     wasFloating = true;
                     onlyBigDecimal = false;
                  } else if (value instanceof Byte) {
                     sum = sum + (byte) value;
                     longSum = longSum + (byte) value;
                     onlyBigDecimal = false;
                  } else if (value instanceof BigDecimal) {
                     bigDecimal = bigDecimal.add((BigDecimal) value);
                     sum = sum + ((BigDecimal) value).doubleValue();
                     onlyBigInteger = false;
                  } else if (value instanceof BigInteger) {
                     bigDecimal = bigDecimal.add(new BigDecimal((BigInteger) value));
                     bigInteger = bigInteger.add((BigInteger) value);
                     sum = sum + ((BigInteger) value).longValue();
                  }
               }
            }

            // now see what types we had and try to be as restrictive as possible
            if (onlyBigInteger) {
               doc.put(batch.getResultAttribute(), bigInteger);
            } else if (onlyBigDecimal) {
               doc.put(batch.getResultAttribute(), bigDecimal);
            } else if (!wasFloating) {
               doc.put(batch.getResultAttribute(), longSum);
            } else {
               doc.put(batch.getResultAttribute(), sum);
            }
         } else {
            final DataDocument subDoc = new DataDocument();

            batch.getAttributes().forEach(attr -> {
               final Object value = doc.get(attr);

               if (value != null) {
                  subDoc.put(attr, doc.get(attr));
               }
            });

            doc.put(batch.getResultAttribute(), subDoc);
         }

         if (!batch.isKeepOriginal()) {
            batch.getAttributes().forEach(doc::remove);
         }

         documentFacade.updateDocument(batch.getCollectionName(), doc);
      }
   }

   private void internalExecuteBatch(final SplitBatch batch) throws DbException, InvalidConstraintException {
      List<DataDocument> documents = dataStorage.search(batch.getCollectionName(), null, null, -1, -1);

      for (final DataDocument doc : documents) {
         final Object value = doc.get(batch.getAttribute());

         if (value != null) {
            final String original = value instanceof String ? (String) value : value.toString();
            final String[] parts = original.split(batch.getDelimiter(), batch.getSplitAttributes().size());

            for (int i = 0; i < parts.length; i++) {
               doc.put(batch.getSplitAttributes().get(i), batch.isTrim() ? parts[i].trim() : parts[i]);
            }
         }

         if (!batch.isKeepOriginal()) {
            doc.remove(batch.getAttribute());
         }

         documentFacade.updateDocument(batch.getCollectionName(), doc);
      }
   }

}
