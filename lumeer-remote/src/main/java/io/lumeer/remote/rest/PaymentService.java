/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import io.lumeer.core.facade.PaymentFacade;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * url-pattern in web.xml cannot work with wildcards in the middle of the URL.
 * We need organization code in the URL but it must be at the end.
 * Hence we needed to extract this single method in a separate class
 * with its own prefix.
 */
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("paymentNotify")
public class PaymentService extends AbstractService {

   @Inject
   private Logger log;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private HttpServletRequest request;

   /* Callback method for the payment gateway. */
   @GET
   @Path("{organizationId:[0-9a-fA-F]{24}}/{id:[0-9a-fA-F]{1,24}}")
   public Response updatePaymentState(@PathParam("organizationId") final String organizationId, @PathParam("id") final String id) {
      final String ip = request.getRemoteAddr();
      log.log(Level.INFO, String.format("Update payment status for organization '%s' and payment id '%s' from IP '%s'.", organizationId, id, ip));

      paymentFacade.updatePayment(organizationId, id);

      return Response.ok().build();
   }

}
