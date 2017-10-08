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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.batch.AbstractCollectionBatch;
import io.lumeer.engine.api.batch.Batch;
import io.lumeer.engine.api.batch.MergeBatch;
import io.lumeer.engine.api.batch.SplitBatch;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.BatchFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * Runs batch operations on collections.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/organizations/{organization}/projects/{project}/batch/")
@RequestScoped
public class BatchService {

   @Inject
   private BatchFacade batchFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @PathParam("organization")
   private String organisationCode;

   @PathParam("project")
   private String projectCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   /**
    * Runs a merge batch operation.
    *
    * @param batch
    *       The batch operation to run.
    * @throws DbException
    *       When there was an error working with the data storage.
    * @throws InvalidConstraintException
    *       When the new change did not pass the constraint criteria.
    */
   @POST
   @Path("/merge")
   @Consumes(MediaType.APPLICATION_JSON)
   public void runMergeBatch(final MergeBatch batch) throws DbException, InvalidConstraintException {
      runBatch(batch);
   }

   /**
    * Runs a split batch operation.
    *
    * @param batch
    *       The batch operation to run.
    * @throws DbException
    *       When there was an error working with the data storage.
    * @throws InvalidConstraintException
    *       When the new change did not pass the constraint criteria.
    */
   @POST
   @Path("/split")
   @Consumes(MediaType.APPLICATION_JSON)
   public void runSplitBatch(final SplitBatch batch) throws DbException, InvalidConstraintException {
      runBatch(batch);
   }

   private void runBatch(final Batch batch) throws DbException, InvalidConstraintException {
      if (batch instanceof AbstractCollectionBatch) {
         final AbstractCollectionBatch collectionBatch = (AbstractCollectionBatch) batch;
         collectionBatch.setCollectionCode(collectionBatch.getCollectionCode());
      }

      batchFacade.executeBatch(batch);
   }
}
