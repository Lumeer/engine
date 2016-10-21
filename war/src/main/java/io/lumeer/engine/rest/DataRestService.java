/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.UpdateElement;
import io.lumeer.engine.api.task.SearchTask;
import io.lumeer.engine.util.JmsService;

import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/data")
@RequestScoped
public class DataRestService {

   @Inject
   private Logger log;

   @Inject
   private DataStorage storage;

   @Inject
   private Event<UpdateElement> element;

   @Inject
   private JmsService jmsService;

   /**
    * Sample get request handler.
    *
    * @param batchId
    *       Id of object to obtain.
    * @return Returns the list of objects.
    */
   @GET
   @Path("/element/{batch:[0-9][0-9]*}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> getElements(@PathParam("batch") final String batchId) {
      element.fire(new UpdateElement(new DataDocument())); // this shows how to fire a CDI event
      jmsService.enqueueTask(new SearchTask("search for green keys")); // this is how we send a jms message

      return null;
   }

}
