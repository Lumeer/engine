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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentGatewayFacade;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@ExtendWith(ArquillianExtension.class)
public class OrganizationServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<Role> USER_ROLES = Organization.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(new Role(RoleType.Read));

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;

   private String organizationUrl;

   private static final CompanyContact CONTACT = new CompanyContact(null, CODE1, "Acme s.r.o.",
         "Ferda", "Buřišek", "Hornodolní 34", "", "Požíňky", "052 61", "",
         "Česká Republika", "ferda@acme.cz", "+420 601 789 432", "00123987", "CZ00123987");

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PaymentGatewayFacade paymentGatewayFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @BeforeEach
   public void prepare() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, GROUP_ROLES);

      organizationUrl = basePath() + "organizations";

      PermissionCheckerUtil.allowGroups();
   }

   @Test
   public void testGetOrganizations() {
      createOrganization(CODE1);
      createOrganization(CODE2);

      Response response = client.target(organizationUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Organization> organizations = response.readEntity(new GenericType<List<Organization>>() {
      });
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);

      Permissions permissions1 = organizations.get(0).getPermissions();
      assertThat(permissions1.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions1.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions1.getGroupPermissions()).isEmpty();

      Permissions permissions2 = organizations.get(1).getPermissions();
      assertThat(permissions2.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions2.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions2.getGroupPermissions()).isEmpty();
   }

   private Organization createOrganization(final String code) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);

      return organizationFacade.createOrganization(organization);
   }

   private Organization createOrganizationWithSpecificPermissions(final String code) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);
      organization.getPermissions().updateUserPermissions(userPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      return organizationDao.createOrganization(organization);
   }

   @Test
   public void testGetOrganization() {
      final Organization organization = createOrganization(CODE1);

      Response response = client.target(organizationUrl).path(organization.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(Organization.class);
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
      final Organization organization = createOrganization(CODE1);

      Response response = client.target(organizationUrl).path(organization.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(organizationUrl).build());

      assertThatThrownBy(() -> organizationFacade.getOrganizationById(organization.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = new Organization(CODE1, NAME, ICON, COLOR, null, null, null);
      Entity entity = Entity.json(organization);

      Response response = client.target(organizationUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(Organization.class);

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
      final Organization organization = createOrganization(CODE1);

      Organization updatedOrganization = new Organization(CODE2, NAME, ICON, COLOR, null, null, new Permissions(Set.of(userPermission), Set.of()));
      Entity entity = Entity.json(updatedOrganization);

      Response response = client.target(organizationUrl).path(organization.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(Organization.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      Organization storedOrganization = organizationFacade.getOrganizationById(organization.getId());
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
      final Organization organization = createOrganizationWithSpecificPermissions(CODE1);

      Response response = client.target(organizationUrl).path(organization.getId()).path("permissions")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(Permissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      final Organization organization = createOrganizationWithSpecificPermissions(CODE1);

      Permission[] userPermission = { Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.TechConfig, true), new Role(RoleType.DataWrite))) };
      Entity entity = Entity.json(userPermission);

      Response response = client.target(organizationUrl).path(organization.getId()).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission[0]);

      Permissions storedPermissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission[0]);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      final Organization organization = createOrganizationWithSpecificPermissions(CODE1);

      Response response = client.target(organizationUrl).path(organization.getId()).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(organizationUrl + "/" + organization.getId() + "/permissions").build());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      final Organization organization = createOrganizationWithSpecificPermissions(CODE1);

      Permission[] groupPermission = { Permission.buildWithRoles(GROUP, Set.of(new Role(RoleType.Manage), new Role(RoleType.DataWrite, true))) };
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(organizationUrl).path(organization.getId()).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission[0]);

      Permissions storedPermissions = organizationDao.getOrganizationById(organization.getId()).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission[0]);
   }

   @Test
   public void testRemoveGroupPermission() {
      Organization organization = createOrganizationWithSpecificPermissions(CODE1);

      Group group = new Group(GROUP);
      groupDao.setOrganization(organization);
      this.group = groupDao.createGroup(group);

      organization.getPermissions().removeGroupPermission(GROUP);
      organization.getPermissions().updateGroupPermissions(new Permission(group.getId(), GROUP_ROLES));
      organization = organizationDao.updateOrganization(organization.getId(), organization);

      Response response = client.target(organizationUrl).path(organization.getId()).path("permissions").path("groups").path(this.group.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(organizationUrl + "/" + organization.getId() + "/permissions").build());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testPaymentsAssessments() {
      paymentGatewayFacade.setDryRun(true);
      final Organization organization = createOrganization(CODE1);
      createOrganization(CODE2);

      Payment payment = new Payment(null, new Date(), 1770, "1234",
            Date.from(ZonedDateTime.ofLocal(
                  LocalDateTime.of(2018, 4, 1, 12, 0), ZoneId.systemDefault(), null).toInstant()),
            Date.from(ZonedDateTime.ofLocal(
                  LocalDateTime.of(2019, 4, 1, 12, 0), ZoneId.systemDefault(), null).toInstant()),
            Payment.PaymentState.CREATED, Payment.ServiceLevel.BASIC, 10, "cz", "CZK", "", null);

      payment = createPayment(organization.getId(), payment);
      ServiceLimits serviceLimits = getServiceLimits(organization.getId());
      assertThat(serviceLimits.getServiceLevel()).isEqualTo(Payment.ServiceLevel.FREE);

      // todo call updateService once we know the message format
   }

   private Payment createPayment(final String organizationId, final Payment payment) {
      Entity entity = Entity.json(payment);

      Response response = client.target(organizationUrl).path(organizationId).path("payments")
                                .request(MediaType.APPLICATION_JSON)
                                .header("RETURN_URL", "https://app.lumeer.io/ui/ORG/payment")
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      return response.readEntity(Payment.class);
   }

   private ServiceLimits getServiceLimits(final String organizationId) {
      return getEntity(organizationId, "serviceLimit", ServiceLimits.class);
   }

   @Test
   public void testCompanyContact() {
      final Organization org = createOrganization(CODE1);
      CONTACT.setOrganizationId(org.getId());

      CompanyContact contact = getCompanyContact(org.getId());
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
      contact = setCompanyContact(org.getId(), CONTACT);

      // we have an id from db, store it for later double check
      assertThat(contact.getId()).isNotEmpty();
      final String originalId = contact.getId();
      contact.setId(CONTACT.getId()); // the id is dynamic
      assertThat(contact).isEqualTo(CONTACT);

      // verify that it has been set
      contact = getCompanyContact(org.getId());
      assertThat(contact.getId()).isEqualTo(originalId); // make sure the id matches
      contact.setId(CONTACT.getId()); // and then delete it so that it equals with our template contact
      assertThat(contact).isEqualTo(CONTACT);
   }

   private CompanyContact getCompanyContact(final String organizationId) {
      return getEntity(organizationId, "contact", CompanyContact.class);
   }

   private CompanyContact setCompanyContact(final String organizationId, final CompanyContact contact) {
      Entity entity = Entity.json(contact);

      Response response = client.target(organizationUrl).path(organizationId).path("contact")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();

      return response.readEntity(CompanyContact.class);
   }

   private <T> T getEntity(final String organization, final String path, final Class<T> clazz) {
      Response response = client.target(organizationUrl).path(organization).path(path)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      return response.readEntity(clazz);
   }
}
