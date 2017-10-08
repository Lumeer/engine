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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.push.PushService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This is for development purposes only, will be deleted!
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/example")
public class ExampleService {

   @Inject
   private Event<UpdateDocument> element;

   //@Inject
   //private JmsService jmsService;

   @Inject
   private PushService pushService;

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
      element.fire(new UpdateDocument("collection", new DataDocument())); // this shows how to fire a CDI event
      //jmsService.enqueueTask(new SearchTask("run for green keys")); // this is how we send a jms message

      return null;
   }

   /**
    * Broadcasts a push notification to browsers.
    */
   @GET
   @Path("/broadcast")
   public void broadcast() {
      pushService.publishMessage("chat", "Hello everyone, this is Lumeer speaking!");
   }

   @GET
   @Path("/document")
   public DataDocument getSampleDocument() {
      final DataDocument d = new DataDocument();
      final DataDocument subDoc = new DataDocument();
      subDoc.put("param1", "string");
      subDoc.put("param2", 43.12);
      d.put("string", "Supergirl");
      d.put("integer", new Integer(12));
      d.put("int", 42);
      d.put("boolean", true);
      d.put("Boolean", Boolean.valueOf(true));
      d.put("list", Arrays.asList("abc", "def", "gah"));
      d.put("double", 2.72d);
      d.put("date", new Date());
      d.put("map<int, str>", Collections.singletonMap(13, "abc"));
      d.put("complex key name :.;\"'", "test");
      d.put("subdoc", subDoc);

      return d;
   }

   @GET
   @Path("/query")
   public Query getSampleQuery() {
      final Query q = new Query();
      q.getCollections().add("Students");
      q.getCollections().add("Universities");
      q.getFilters().put("name", "Pepa z Depa");
      q.getFilters().put("city", "Brno");
      q.setLimit(12);

      return q;
   }

}
