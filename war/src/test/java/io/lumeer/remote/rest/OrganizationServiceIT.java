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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentGatewayFacade;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class OrganizationServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<Role> USER_ROLES = Organization.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(Role.READ);

   private Permission userPermission;
   private Permission groupPermission;
   private User user;

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String ORGANIZATION_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations";
   private static final String ORGANIZATION_URL = SERVER_URL + ORGANIZATION_PATH;
   private static final String PERMISSIONS_URL = ORGANIZATION_URL + "/" + CODE1 + "/permissions";

   private static final CompanyContact CONTACT = new CompanyContact(null, CODE1, "Acme s.r.o.",
         "Ferda", "Buřišek", "Hornodolní 34", "", "Požíňky", "052 61","",
         "Česká Republika", "ferda@acme.cz", "+420 601 789 432", "00123987", "CZ00123987");

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PaymentGatewayFacade paymentGatewayFacade;

   @Inject
   private UserDao userDao;

   @Before
   public void prepare(){
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = new SimplePermission(this.user.getId(), USER_ROLES);
      groupPermission = new SimplePermission(GROUP, GROUP_ROLES);
   }

   @Test
   public void testGetOrganizations() {
      createOrganization(CODE1);
      createOrganization(CODE2);

      Response response = client.target(ORGANIZATION_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonOrganization> organizations = response.readEntity(new GenericType<List<JsonOrganization>>() {
      });
      assertThat(organizations).extracting(Resource::getCode).containsOnly(CODE1, CODE2);

      Permissions permissions1 = organizations.get(0).getPermissions();
      assertThat(permissions1).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(userPermission));
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());

      Permissions permissions2 = organizations.get(1).getPermissions();
      assertThat(permissions2).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(userPermission));
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());
   }

   private Organization createOrganization(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null, null);

      return organizationFacade.createOrganization(organization);
   }

   private void createOrganizationWithSpecificPermissions(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null, null);
      organization.getPermissions().updateUserPermissions(userPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      organizationDao.createOrganization(organization);
   }

   @Test
   public void testGetOrganization() {
      createOrganization(CODE1);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(JsonOrganization.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testDeleteOrganization() {
      createOrganization(CODE1);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(ORGANIZATION_URL).build());

      assertThatThrownBy(() -> organizationFacade.getOrganization(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = new JsonOrganization(CODE1, NAME, ICON, COLOR, null, null);
      Entity entity = Entity.json(organization);

      Response response = client.target(ORGANIZATION_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(JsonOrganization.class);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateOrganization() {
      createOrganization(CODE1);

      Organization updatedOrganization = new JsonOrganization(CODE2, NAME, ICON, COLOR, null, null);
      Entity entity = Entity.json(updatedOrganization);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(JsonOrganization.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      Organization storedOrganization = organizationFacade.getOrganization(CODE2);
      assertThat(storedOrganization).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testGetOrganizationPermissions() {
      createOrganizationWithSpecificPermissions(CODE1);

      Response response = client.target(PERMISSIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(JsonPermissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      createOrganizationWithSpecificPermissions(CODE1);

      SimplePermission userPermission = new SimplePermission(this.user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      Entity entity = Entity.json(userPermission);

      Response response = client.target(PERMISSIONS_URL).path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission);

      Permissions storedPermissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      createOrganizationWithSpecificPermissions(CODE1);

      Response response = client.target(PERMISSIONS_URL).path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createOrganizationWithSpecificPermissions(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(PERMISSIONS_URL).path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission);

      Permissions storedPermissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createOrganizationWithSpecificPermissions(CODE1);

      Response response = client.target(PERMISSIONS_URL).path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testPaymentsAssessments() {
      paymentGatewayFacade.setDryRun(true);
      createOrganization(CODE1);
      createOrganization(CODE2);

      Payment payment = new Payment(null, new Date(), 1770, "1234",
            Date.from(ZonedDateTime.ofLocal(
                  LocalDateTime.of(2018, 4, 1, 12, 0), ZoneId.systemDefault(), null).toInstant()),
            Date.from(ZonedDateTime.ofLocal(
               LocalDateTime.of(2019, 4, 1, 12, 0), ZoneId.systemDefault(), null).toInstant()),
            Payment.PaymentState.CREATED, Payment.ServiceLevel.BASIC, 10, "cz", "CZK", "");

      payment = createPayment(CODE1, payment);
      ServiceLimits serviceLimits = getServiceLimits(CODE1);
      assertThat(serviceLimits.getServiceLevel()).isEqualTo(Payment.ServiceLevel.FREE);

      // todo call updateService once we know the message format
   }

   private Payment createPayment(final String organization, final Payment payment) {
      Entity entity = Entity.json(payment);

      Response response = client.target(ORGANIZATION_URL).path(organization).path("payments")
                                .request(MediaType.APPLICATION_JSON)
                                .header("RETURN_URL", "https://app.lumeer.io/ui/ORG/payment")
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      return response.readEntity(Payment.class);
   }

   private ServiceLimits getServiceLimits(final String organization) {
      return getEntity(organization, "serviceLimits", ServiceLimits.class);
   }

   @Test
   public void testCompanyContact() {
      final Organization org = createOrganization(CODE1);
      CONTACT.setOrganizationId(org.getId());

      CompanyContact contact = getCompanyContact(CODE1);
      assertThat(contact.getOrganizationId()).isEqualTo(org.getId());
      assertThat(contact.getCompany()).isEmpty();
      assertThat(contact.getFirstName()).isEmpty();
      assertThat(contact.getLastName()).isEmpty();
      assertThat(contact.getAddress1()).isEmpty();
      assertThat(contact.getAddress2()).isEmpty();
      assertThat(contact.getState()).isEmpty();
      assertThat(contact.getCity()).isEmpty();
      assertThat(contact.getCountry()).isEmpty();
      assertThat(contact.getIc()).isEmpty();
      assertThat(contact.getDic()).isEmpty();
      assertThat(contact.getEmail()).isEmpty();
      assertThat(contact.getPhone()).isEmpty();

      // set a new contact
      contact = setCompanyContact(CODE1, CONTACT);

      // we have an id from db, store it for later double check
      assertThat(contact.getId()).isNotEmpty();
      final String originalId = contact.getId();
      contact.setId(CONTACT.getId()); // the id is dynamic
      assertThat(contact).isEqualTo(CONTACT);

      // verify that it has been set
      contact = getCompanyContact(CODE1);
      assertThat(contact.getId()).isEqualTo(originalId); // make sure the id matches
      contact.setId(CONTACT.getId()); // and then delete it so that it equals with our template contact
      assertThat(contact).isEqualTo(CONTACT);
   }

   private CompanyContact getCompanyContact(final String organization) {
      return getEntity(organization, "contact", CompanyContact.class);
   }

   private CompanyContact setCompanyContact(final String organization, final CompanyContact contact) {
      Entity entity = Entity.json(contact);

      Response response = client.target(ORGANIZATION_URL).path(organization).path("contact")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();

      return response.readEntity(CompanyContact.class);
   }

   private <T> T getEntity(final String organization, final String path, final Class<T> clazz) {
      Response response = client.target(ORGANIZATION_URL).path(organization).path(path)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      return response.readEntity(clazz);
   }
}
