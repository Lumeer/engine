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

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.facade.CompanyContactFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations")
public class OrganizationService extends AbstractService {

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private CompanyContactFacade companyContactFacade;

   @POST
   public Organization createOrganization(Organization organization) {
      return organizationFacade.createOrganization(organization);
   }

   @PUT
   @Path("{organizationId}")
   public Organization updateOrganization(@PathParam("organizationId") String organizationId, Organization organization) {
      return organizationFacade.updateOrganization(organizationId, organization);
   }

   @DELETE
   @Path("{organizationId}")
   public Response deleteOrganization(@PathParam("organizationId") String organizationId) {
      organizationFacade.deleteOrganization(organizationId);

      return Response.ok().link(getParentUri(organizationId), "parent").build();
   }

   @GET
   @Path("{organizationId}")
   public Organization getOrganization(@PathParam("organizationId") String organizationId) {
      return organizationFacade.getOrganizationById(organizationId);
   }

   @GET
   public List<Organization> getOrganizations() {
      return organizationFacade.getOrganizations();
   }

   @GET
   @Path("info/codes")
   public Set<String> getOrganizationsCodes() {
      return organizationFacade.getOrganizationsCodes();
   }

   @GET
   @Path("{organizationId}/permissions")
   public Permissions getOrganizationPermissions(@PathParam("organizationId") String organizationId) {
      return organizationFacade.getOrganizationPermissions(organizationId);
   }

   @PUT
   @Path("{organizationId}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("organizationId") String organizationId, Set<Permission> userPermission) {
      return organizationFacade.updateUserPermissions(organizationId, userPermission);
   }

   @DELETE
   @Path("{organizationId}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("organizationId") String organizationId, @PathParam("userId") String userId) {
      organizationFacade.removeUserPermission(organizationId, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{organizationId}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("organizationId") String organizationId, Set<Permission> groupPermission) {
      return organizationFacade.updateGroupPermissions(organizationId, groupPermission);
   }

   @DELETE
   @Path("{organizationId}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("organizationId") String organizationId, @PathParam("groupId") String groupId) {
      organizationFacade.removeGroupPermission(organizationId, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   /* Gets a complete list of all payments sorted by valid until descending. */
   @GET
   @Path("{organizationId}/payments")
   public List<Payment> getPayments(@PathParam("organizationId") final String organizationId) {
      return paymentFacade.getPayments(organizationFacade.getOrganizationById(organizationId));
   }

   /* Gets a complete list of all payments sorted by valid until descending. */
   @GET
   @Path("{organizationId}/payment/{paymentId}")
   public Payment getPayments(@PathParam("organizationId") final String organizationId, @PathParam("paymentId") final String paymentId) {
      return paymentFacade.getPayment(organizationFacade.getOrganizationById(organizationId), paymentId);
   }

   /* Gets the current service level the organization has prepaid. */
   @GET
   @Path("{organizationId}/serviceLimit")
   public ServiceLimits getServiceLimits(@PathParam("organizationId") final String organizationId) {
      return paymentFacade.getCurrentServiceLimits(organizationFacade.getOrganizationById(organizationId));
   }

   @GET
   @Path("info/serviceLimits")
   public Map<String, ServiceLimits> getAllServiceLimits() {
      return paymentFacade.getAllServiceLimits(organizationFacade.getOrganizations());
   }

   /* Creates a new payment. Communicates with payment gateway. Returns the payment updated with payment ID.
      Must pass RETURN_URL header for the successful redirect. */
   @POST
   @Path("{organizationId}/payments")
   public Payment createPayment(@PathParam("organizationId") final String organizationCode, final Payment payment,
         @Context final HttpServletRequest servletContext) {
      final String notifyUrl = servletContext.getRequestURL().toString().replaceAll("/payments$", "").replaceFirst("organizations", "paymentNotify");
      final String returnUrl = servletContext.getHeader("RETURN_URL");

      return paymentFacade.createPayment(
            organizationFacade.getOrganizationById(organizationCode),
            paymentFacade.checkPaymentValues(payment),
            notifyUrl,
            returnUrl);
   }

   @GET
   @Path("{organizationId}/contact")
   public CompanyContact getCompanyContact(@PathParam("organizationId") final String organizationId) {
      return companyContactFacade.getCompanyContact(organizationFacade.getOrganizationById(organizationId));
   }

   @PUT
   @Path("{organizationId}/contact")
   public CompanyContact setCompanyContact(@PathParam("organizationId") final String organizationId, final CompanyContact companyContact) {
      return companyContactFacade.setCompanyContact(organizationFacade.getOrganizationById(organizationId), companyContact);
   }

}
