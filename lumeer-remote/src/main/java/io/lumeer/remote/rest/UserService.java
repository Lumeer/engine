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

import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.LogEvent;
import io.lumeer.api.model.PaymentStats;
import io.lumeer.api.model.ProductDemo;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserInvitation;
import io.lumeer.api.model.UserOnboarding;
import io.lumeer.api.view.UserViews;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.core.facade.UserFacade;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.remote.rest.annotation.HealthCheck;
import io.lumeer.remote.rest.annotation.PATCH;

import com.auth0.json.auth.TokenHolder;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("users")
public class UserService extends AbstractService {

   @Inject
   private UserFacade userFacade;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @GET
   @JsonView(UserViews.DefaultView.class)
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users")
   public List<User> getUsers(@PathParam("organizationId") String organizationId) {
      workspaceKeeper.setOrganizationId(organizationId);
      return userFacade.getUsers(organizationId);
   }

   @POST
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users")
   @JsonView(UserViews.DefaultView.class)
   @HealthCheck
   public User createUserInOrganization(@PathParam("organizationId") String organizationId, User user) {
      workspaceKeeper.setOrganizationId(organizationId);
      return userFacade.createUser(organizationId, user);
   }

   @POST
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/users/invite")
   @JsonView(UserViews.DefaultView.class)
   @HealthCheck
   public List<User> createUsersInOrganization(@PathParam("organizationId") final String organizationId, @PathParam("projectId") final String projectId, final List<UserInvitation> invitations) {
      workspaceKeeper.setOrganizationId(organizationId);
      return userFacade.createUsersInWorkspace(organizationId, projectId, invitations);
   }

   @GET
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users/{userId:[0-9a-fA-F]{24}}")
   @JsonView(UserViews.DefaultView.class)
   public User getUser(@PathParam("organizationId") String organizationId, @PathParam("userId") String userId) {
      workspaceKeeper.setOrganizationId(organizationId);
      return userFacade.getUser(organizationId, userId);
   }

   @PUT
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users/{userId:[0-9a-fA-F]{24}}")
   @JsonView(UserViews.DefaultView.class)
   @HealthCheck
   public User updateUserInOrganization(@PathParam("organizationId") String organizationId,
         @PathParam("userId") String userId, User user) {
      workspaceKeeper.setOrganizationId(organizationId);
      return userFacade.updateUser(organizationId, userId, user);
   }

   @POST
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users/{userId:[0-9a-fA-F]{24}}/groups")
   public Response updateUserGroups(@PathParam("organizationId") String organizationId,
         @PathParam("userId") String userId, Set<String> groups) {
      workspaceKeeper.setOrganizationId(organizationId);
      userFacade.setUserGroups(organizationId, userId, groups);
      return Response.ok().build();
   }

   @DELETE
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/users/{userId:[0-9a-fA-F]{24}}")
   public Response deleteUserFromOrganization(@PathParam("organizationId") String organizationId,
         @PathParam("userId") String userId) {
      workspaceKeeper.setOrganizationId(organizationId);
      organizationFacade.removeUserPermission(organizationId, userId);
      userFacade.deleteUser(organizationId, userId);

      return Response.ok().link(getParentUri(userId), "parent").build();
   }

   @GET
   @Path("current")
   @JsonView(UserViews.FullView.class)
   public User getCurrentUser(@HeaderParam("X-Lumeer-Timezone") final String timeZone) {
      final User user = userFacade.getCurrentUser();
      userFacade.checkUserTimeZone(user, timeZone);
      return user;
   }

   @GET
   @Path("current/referrals")
   public PaymentStats getReferralStats() {
      return paymentFacade.getReferralPayments();
   }

   @GET
   @Path("current/hints")
   public DataDocument getHints() {
      return userFacade.getCurrentUser().getHints();
   }

   @PATCH
   @Path("current")
   @JsonView(UserViews.FullView.class)
   @HealthCheck
   public User patchCurrentUser(User user) {
      return userFacade.patchCurrentUser(user, requestDataKeeper.getUserLocale());
   }

   @PUT
   @Path("workspace")
   public Response updateWorkspace(DefaultWorkspace defaultWorkspace) {
      if (defaultWorkspace == null) {
         throw new BadRequestException("defaultWorkspace");
      }

      userFacade.setDefaultWorkspace(defaultWorkspace);

      return Response.ok().build();
   }

   @POST
   @Path("feedback")
   @HealthCheck
   public Response createFeedback(Feedback feedback) {
      userFacade.createFeedback(feedback);

      return Response.ok().build();
   }

   @POST
   @Path("product-demo")
   public Response scheduleDemo(ProductDemo demo) {
      userFacade.scheduleDemo(demo);

      return Response.ok().build();
   }

   @GET
   @Path("check")
   public Response checkAuthentication() {
      return Response.ok().build();
   }

   @PUT
   @Path("current/hints")
   public DataDocument updateHints(final DataDocument hints) {
      return userFacade.updateHints(hints);
   }

   @PUT
   @Path("current/onboarding")
   public UserOnboarding updateHints(final UserOnboarding onboarding) {
      return userFacade.updateOnboarding(onboarding);
   }

   @POST
   @Path("current/log")
   public Response logEvent(final LogEvent logEvent) {
      userFacade.logEvent(logEvent.getEvent(), logEvent.getPriority());

      return Response.ok().build();
   }

   @POST
   @Path("current/emailVerified")
   public Response verifyUserEmail() {
      userFacade.setUserEmailVerified();

      return Response.ok().build();
   }

   @DELETE
   @Path("current")
   public Response removeCurrentUser() {
      userFacade.removeUser();

      return Response.ok().build();
   }

   @POST
   @Path("login")
   @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
   public Response userLogin(@FormParam("userName") final String userName, @FormParam("password") final String password) {
      final TokenHolder holder = userFacade.userLogin(userName, password);
      return Response.ok(holder).build();
   }

}
