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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.batch.AbstractCollectionBatch;
import io.lumeer.engine.api.batch.Batch;
import io.lumeer.engine.api.batch.MergeBatch;
import io.lumeer.engine.api.batch.SplitBatch;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.BatchFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * Runs batch operations on collections.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/batch/")
@RequestScoped
public class BatchService {

   @Inject
   private BatchFacade batchFacade;

   private CollectionMetadataFacade collectionMetadataFacade;

   /**
    * Runs a merge batch operation.
    *
    * @param batch
    *       The batch operation to run.
    */
   @POST
   @Path("/merge")
   @Consumes(MediaType.APPLICATION_JSON)
   public void runMergeBatch(final MergeBatch batch) throws DbException {
      runBatch(batch);
   }

   /**
    * Runs a split batch operation.
    *
    * @param batch
    *       The batch operation to run.
    */
   @POST
   @Path("/split")
   @Consumes(MediaType.APPLICATION_JSON)
   public void runSplitBatch(final SplitBatch batch) throws DbException {
      runBatch(batch);
   }

   private void runBatch(final Batch batch) throws DbException {
      try {
         if (batch instanceof AbstractCollectionBatch) {
            final AbstractCollectionBatch collectionBatch = (AbstractCollectionBatch) batch;
            collectionBatch.setCollectionName(collectionMetadataFacade.getOriginalCollectionName(collectionBatch.getCollectionName()));
         }
      } catch (CollectionMetadataDocumentNotFoundException | CollectionNotFoundException e) {
         throw new NotAcceptableException("Cannot determine collection name in the system: ", e);
      }

      batchFacade.executeBatch(batch);
   }
}
