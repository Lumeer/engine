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

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
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
   public JsonOrganization createOrganization(JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.createOrganization(organization);

      return JsonOrganization.convert(storedOrganization);
   }

   @PUT
   @Path("{organizationCode}")
   public JsonOrganization updateOrganization(@PathParam("organizationCode") String organizationCode, JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.updateOrganization(organizationCode, organization);

      return JsonOrganization.convert(storedOrganization);
   }

   @DELETE
   @Path("{organizationCode}")
   public Response deleteOrganization(@PathParam("organizationCode") String organizationCode) {
      organizationFacade.deleteOrganization(organizationCode);

      return Response.ok().link(getParentUri(organizationCode), "parent").build();
   }

   @GET
   @Path("{organizationCode}")
   public JsonOrganization getOrganization(@PathParam("organizationCode") String organizationCode) {
      Organization organization = organizationFacade.getOrganization(organizationCode);
      return JsonOrganization.convert(organization);
   }

   @GET
   public List<JsonOrganization> getOrganizations() {
      List<Organization> organizations = organizationFacade.getOrganizations();
      return JsonOrganization.convert(organizations);
   }

   @GET
   @Path("info/codes")
   public Set<String> getOrganizationsCodes() {
      return organizationFacade.getOrganizationsCodes();
   }

   @GET
   @Path("{organizationCode}/permissions")
   public JsonPermissions getOrganizationPermissions(@PathParam("organizationCode") String organizationCode) {
      Permissions permissions = organizationFacade.getOrganizationPermissions(organizationCode);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{organizationCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("organizationCode") String organizationCode, JsonPermission... userPermission) {
      Set<Permission> storedUserPermissions = organizationFacade.updateUserPermissions(organizationCode, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{organizationCode}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("organizationCode") String organizationCode, @PathParam("userId") String userId) {
      organizationFacade.removeUserPermission(organizationCode, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{organizationCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("organizationCode") String organizationCode, JsonPermission... groupPermission) {
      Set<Permission> storedGroupPermissions = organizationFacade.updateGroupPermissions(organizationCode, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
   }

   @DELETE
   @Path("{organizationCode}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("organizationCode") String organizationCode, @PathParam("groupId") String groupId) {
      organizationFacade.removeGroupPermission(organizationCode, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   /* Gets a complete list of all payments sorted by valid until descending. */
   @GET
   @Path("{organizationCode}/payments")
   public List<Payment> getPayments(@PathParam("organizationCode") final String organizationCode) {
      return paymentFacade.getPayments(organizationFacade.getOrganization(organizationCode));
   }

   /* Gets a complete list of all payments sorted by valid until descending. */
   @GET
   @Path("{organizationCode}/payment/{paymentId}")
   public Payment getPayments(@PathParam("organizationCode") final String organizationCode, @PathParam("paymentId") final String paymentId) {
      return paymentFacade.getPayment(organizationFacade.getOrganization(organizationCode), paymentId);
   }

   /* Gets the current service level the organization has prepaid. */
   @GET
   @Path("{organizationCode}/serviceLimit")
   public ServiceLimits getServiceLimits(@PathParam("organizationCode") final String organizationCode) {
      return paymentFacade.getCurrentServiceLimits(organizationFacade.getOrganization(organizationCode));
   }

   @GET
   @Path("info/serviceLimits")
   public Map<String, ServiceLimits> getAllServiceLimits() {
      return paymentFacade.getAllServiceLimits(organizationFacade.getOrganizations());
   }

   /* Creates a new payment. Communicates with payment gateway. Returns the payment updated with payment ID.
      Must pass RETURN_URL header for the successful redirect. */
   @POST
   @Path("{organizationCode}/payments")
   public Payment createPayment(@PathParam("organizationCode") final String organizationCode, final Payment payment,
         @Context final HttpServletRequest servletContext) {
      final String notifyUrl = servletContext.getRequestURL().toString().replaceAll("/payments$", "").replaceFirst("organizations", "paymentNotify");
      final String returnUrl = servletContext.getHeader("RETURN_URL");

      return paymentFacade.createPayment(
            organizationFacade.getOrganization(organizationCode),
            paymentFacade.checkPaymentValues(payment),
            notifyUrl,
            returnUrl);
   }

   @GET
   @Path("{organizationCode}/contact")
   public CompanyContact getCompanyContact(@PathParam("organizationCode") final String organizationCode) {
      return companyContactFacade.getCompanyContact(organizationFacade.getOrganization(organizationCode));
   }

   @PUT
   @Path("{organizationCode}/contact")
   public CompanyContact setCompanyContact(@PathParam("organizationCode") final String organizationCode, final CompanyContact companyContact) {
      return companyContactFacade.setCompanyContact(organizationFacade.getOrganization(organizationCode), companyContact);
   }

}
