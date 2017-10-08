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

import io.lumeer.engine.annotation.UserDataStorage;
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
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   public void executeBatch(final Batch batch) throws DbException, InvalidConstraintException {
      // first check collection size
      if (dataStorage.count(batch.getCollectionCode(), null) > 10000) {
         throw new UnsupportedOperationException("Cannot run a batch process on collections with more than 10000 documents.");
      }

      if (batch instanceof MergeBatch) {
         internalExecuteBatch((MergeBatch) batch);
      } else if (batch instanceof SplitBatch) {
         internalExecuteBatch((SplitBatch) batch);
      }
   }

   private void internalExecuteBatch(final MergeBatch batch) throws DbException, InvalidConstraintException {
      List<DataDocument> documents = dataStorage.search(batch.getCollectionCode(), null, null, 0, 0);

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
            batch.getAttributes().forEach(a -> {
               try {
                  documentFacade.dropAttribute(batch.getCollectionCode(), doc.getId(), a);
               } catch (DbException e) {
                  // nps, we cannot do m ore
               }
               doc.remove(a); // TODO check - for future compatibility but has no effect now
            });
         }

         documentFacade.updateDocument(batch.getCollectionCode(), doc);
      }
   }

   private void internalExecuteBatch(final SplitBatch batch) throws DbException, InvalidConstraintException {
      List<DataDocument> documents = dataStorage.search(batch.getCollectionCode(), null, null, 0, 0);

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
            documentFacade.dropAttribute(batch.getCollectionCode(), doc.getId(), batch.getAttribute());
            doc.remove(batch.getAttribute()); // TODO check - for future compatibility
         }

         documentFacade.updateDocument(batch.getCollectionCode(), doc);
      }
   }

}
