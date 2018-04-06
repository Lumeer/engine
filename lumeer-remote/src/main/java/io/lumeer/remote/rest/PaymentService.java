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
package io.lumeer.remote.rest;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * url-pattern in web.xml cannot work with wildcards in the middle of the URL.
 * We need organization code in the URL but it must be at the end.
 * Hence we needed to extract this single method in a separate class
 * with its own prefix.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("paymentNotify")
public class PaymentService extends AbstractService {

   /* Callback method for the payment gateway. */
   @GET
   @Path("{organizationCode}")
   public Response updatePaymentState(@PathParam("organizationCode") final String organizationCode, final String message) {
      System.out.println("Update of payment id " + message);

      // TODO: 1) ask gopay about the payment state
      // TODO: 2) update the payment accordingly

      return Response.ok().build();
   }

}
