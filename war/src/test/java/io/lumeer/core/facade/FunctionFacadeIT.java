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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FunctionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class FunctionFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private Permission userPermission;
   private Permission groupPermission;

   private Collection c1;
   private Collection c2;
   private Collection c3;
   private Collection c4;
   private Collection c5;

   @Inject
   private FunctionFacade functionFacade;

   @Inject
   private FunctionDao functionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private DocumentDao documentDao;

   @Before
   public void configureProject() {
      User user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);
      groupDao.setOrganization(organization);
      Group group = groupDao.createGroup(new Group(GROUP));

      userPermission = Permission.buildWithRoles(user.getId(), Collection.ROLES);
      groupPermission = Permission.buildWithRoles(group.getId(), Collections.singleton(Role.READ));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      functionDao.setProject(project);
   }

   @Test
   public void testCreateCollectionTaskSameCollection() {
      createTestData();
      // C1(a1) = C1(a2) + C1(a3)

      FunctionRow row1 = FunctionRow.createForCollection(c1.getId(), "a1", c1.getId(), null, "a2");
      FunctionRow row2 = FunctionRow.createForCollection(c1.getId(), "a1", c1.getId(), null, "a3");
      functionDao.createRows(Arrays.asList(row1, row2));

      final Deque<FunctionFacade.FunctionParameterDocuments> queue = functionFacade.createQueueForCollection(c1, getAttribute(c1, "a1"), Arrays.asList(row1, row2));
      assertThat(queue).hasSize(1);
      assertThat(queue.getFirst().getAttributeId()).isEqualTo("a1");
      assertThat(queue.getFirst().getResourceId()).isEqualTo(c1.getId());
      assertThat(queue.getFirst().getDocuments()).hasSize(5);

      Document document = getAnyDocument(c1);
      final Deque<FunctionFacade.FunctionParameterDocuments> queue2 = functionFacade.createQueueForDocumentChanged(c1.getId(), "a2", document.getId());
      assertThat(queue2).hasSize(1);
      assertThat(queue2.getFirst().getAttributeId()).isEqualTo("a1");
      assertThat(queue2.getFirst().getResourceId()).isEqualTo(c1.getId());
      assertThat(queue2.getFirst().getDocuments()).hasSize(1);

      final Deque<FunctionFacade.FunctionParameterDocuments> queue3 = functionFacade.createQueueForCreatedDocument(c1, document);
      assertThat(queue3).hasSize(5);
   }

   @Test
   public void testCreateCollectionTaskLinkedCollection() {
      createTestData();
      // C1(a1) = C2(a2) + C3(a3); C2(a2) = C4(a4) + C5(a5); C2(a3) = C2(a2) + C2(a4);

      LinkType l12 = getLinkType(c1, c2);
      LinkType l13 = getLinkType(c1, c3);
      LinkType l24 = getLinkType(c2, c4);
      LinkType l25 = getLinkType(c2, c5);

      FunctionRow row1 = FunctionRow.createForCollection(c1.getId(), "a1", c2.getId(), l12.getId(), "a2");
      FunctionRow row2 = FunctionRow.createForCollection(c1.getId(), "a1", c3.getId(), l13.getId(), "a3");
      FunctionRow row3 = FunctionRow.createForCollection(c2.getId(), "a2", c4.getId(), l24.getId(), "a4");
      FunctionRow row4 = FunctionRow.createForCollection(c2.getId(), "a2", c5.getId(), l25.getId(), "a5");
      FunctionRow row5 = FunctionRow.createForCollection(c2.getId(), "a3", c2.getId(), null, "a2");
      FunctionRow row6 = FunctionRow.createForCollection(c2.getId(), "a3", c2.getId(), null, "a4");
      functionDao.createRows(Arrays.asList(row1, row2, row3, row4, row5, row6));

      Document c4Document = getAnyDocument(c4);
      List<Document> c2Documents = getDocuments(c2).subList(0, 2);
      List<Document> c1Documents = getDocuments(c1).subList(0, 2);
      createLinks(l24, Collections.singletonList(c4Document), c2Documents);
      createLinks(l12, c2Documents, c1Documents);

      Deque<FunctionFacade.FunctionParameterDocuments> queue = functionFacade.createQueueForDocumentChanged(c4.getId(), "a4", c4Document.getId());
      assertThat(queue).hasSize(3);
      assertThat(queue.getFirst().getResourceId()).isEqualTo(c2.getId());
      assertThat(queue.getFirst().getAttributeId()).isEqualTo("a2");

      FunctionTask task = functionFacade.convertQueueToTask(queue);
      assertThat(task).isNotNull();
      assertThat(task.getCollection().getId()).isEqualTo(c2.getId());
      assertThat(task.getAttribute().getId()).isEqualTo("a2");
      assertThat(task.getDocuments()).hasSize(2);
      assertThat(task.getParents()).hasSize(1);
      assertThat(task.getParents().get(0).getParents()).hasSize(1);
      assertThat(task.getParents().get(0).getParents().get(0).getParents()).isNull();
   }

   @Test
   public void testCreateCollectionTaskWithCycle() {
      createTestData();
      // C1(a1) = C2(a2) + C3(a3); C2(a2) = C4(a4) + C5(a5); C4(a4) = C1(a1) + C1(a2)

      LinkType l12 = getLinkType(c1, c2);
      LinkType l13 = getLinkType(c1, c3);
      LinkType l24 = getLinkType(c2, c4);
      LinkType l25 = getLinkType(c2, c5);
      LinkType l41 = getLinkType(c4, c1);

      FunctionRow row1 = FunctionRow.createForCollection(c1.getId(), "a1", c2.getId(), l12.getId(), "a2");
      FunctionRow row2 = FunctionRow.createForCollection(c1.getId(), "a1", c3.getId(), l13.getId(), "a3");
      FunctionRow row3 = FunctionRow.createForCollection(c2.getId(), "a2", c4.getId(), l24.getId(), "a4");
      FunctionRow row4 = FunctionRow.createForCollection(c2.getId(), "a2", c5.getId(), l25.getId(), "a5");
      FunctionRow row5 = FunctionRow.createForCollection(c4.getId(), "a4", c1.getId(), l41.getId(), "a1");
      FunctionRow row6 = FunctionRow.createForCollection(c4.getId(), "a4", c1.getId(), l41.getId(), "a2");
      functionDao.createRows(Arrays.asList(row1, row2, row3, row4, row5, row6));

      List<Document> c4Documents = getDocuments(c4).subList(0, 2);
      List<Document> c2Documents = getDocuments(c2).subList(0, 2);
      List<Document> c1Documents = getDocuments(c1).subList(0, 2);
      createLinks(l12, c2Documents, c1Documents);
      createLinks(l24, c4Documents, c2Documents);
      createLinks(l41, c4Documents, c1Documents);

      Deque<FunctionFacade.FunctionParameterDocuments> queue = functionFacade.createQueueForDocumentChanged(c1.getId(), "a2", c1Documents.get(0).getId());
      assertThat(queue).hasSize(3);
      assertThat(queue).extracting(FunctionFacade.FunctionParameterDocuments::getResourceId).contains(c4.getId(), c2.getId(), c1.getId());

      FunctionTask task = functionFacade.convertQueueToTask(queue);
      assertThat(task).isNotNull();
      assertThat(task.getParents()).hasSize(1);
      assertThat(task.getParents().get(0).getParents()).hasSize(1);
      assertThat(task.getParents().get(0).getParents().get(0).getParents()).isNull();
   }

   @Test
   public void testCreateLinkTaskSameLink() {
      createTestData();
      // L12(a1) = L12(a2) + L12(a3)

      LinkType l12 = getLinkType(c1, c2);
      FunctionRow row1 = FunctionRow.createForLink(l12.getId(), "a1", null, l12.getId(), "a2");
      FunctionRow row2 = FunctionRow.createForLink(l12.getId(), "a1", null, l12.getId(), "a3");
      functionDao.createRows(Arrays.asList(row1, row2));

      List<Document> c2Documents = getDocuments(c2).subList(0, 2);
      List<Document> c1Documents = getDocuments(c1).subList(0, 2);
      List<LinkInstance> linkInstances = createLinks(l12, c2Documents, c1Documents);

      final Deque<FunctionFacade.FunctionParameterDocuments> queue = functionFacade.createQueueForLinkType(l12, getAttribute(l12, "a1"), Arrays.asList(row1, row2));
      assertThat(queue).hasSize(1);
      assertThat(queue.getFirst().getAttributeId()).isEqualTo("a1");
      assertThat(queue.getFirst().getResourceId()).isEqualTo(l12.getId());
      assertThat(queue.getFirst().getLinkInstances()).hasSize(linkInstances.size());

      LinkInstance linkInstance = getAnyLink(l12);
      final Deque<FunctionFacade.FunctionParameterDocuments> queue2 = functionFacade.createQueueForLinkChanged(l12.getId(), "a2", linkInstance.getId());
      assertThat(queue2).hasSize(1);
      assertThat(queue2.getFirst().getAttributeId()).isEqualTo("a1");
      assertThat(queue2.getFirst().getResourceId()).isEqualTo(l12.getId());
      assertThat(queue2.getFirst().getLinkInstances()).hasSize(1);

      final Deque<FunctionFacade.FunctionParameterDocuments> queue3 = functionFacade.createQueueForCreatedLink(l12, linkInstance);
      assertThat(queue3).hasSize(5);
   }

   @Test
   public void testCreateLinkTaskToCollection() {
      createTestData();
      // L12(a1) = C2(a2) + C2(a3); C2(a2) = L23(a1) + L23(a2); L23(a1) = C3(a3) + C3(a4)

      LinkType l12 = getLinkType(c1, c2);
      LinkType l23 = getLinkType(c2, c3);
      FunctionRow row1 = FunctionRow.createForLink(l12.getId(), "a1", c2.getId(), l12.getId(), "a2");
      FunctionRow row2 = FunctionRow.createForLink(l12.getId(), "a1", c2.getId(), l12.getId(), "a3");
      FunctionRow row3 = FunctionRow.createForCollection(c2.getId(), "a2", c3.getId(), l23.getId(), "a1");
      FunctionRow row4 = FunctionRow.createForCollection(c2.getId(), "a2", c3.getId(), l23.getId(), "a2");
      FunctionRow row5 = FunctionRow.createForLink(l23.getId(), "a1", c3.getId(), l23.getId(), "a3");
      FunctionRow row6 = FunctionRow.createForLink(l23.getId(), "a1", c3.getId(), l23.getId(), "a4");
      functionDao.createRows(Arrays.asList(row1, row2, row3, row4, row5, row6));

      List<Document> c1Documents = getDocuments(c1).subList(0, 2);
      List<Document> c2Documents = getDocuments(c2).subList(0, 2);
      List<Document> c3Documents = getDocuments(c3).subList(0, 2);
      createLinks(l12, c1Documents, c2Documents);
      List<LinkInstance> l23Links = createLinks(l23, c2Documents, c3Documents);

      Deque<FunctionFacade.FunctionParameterDocuments> queue = functionFacade.createQueueForLinkChanged(l23.getId(), "a1", l23Links.get(0).getId());
      assertThat(queue).hasSize(2);
      assertThat(queue.getFirst().getResourceId()).isEqualTo(c2.getId());
      assertThat(queue.getLast().getResourceId()).isEqualTo(l12.getId());

      FunctionTask task = functionFacade.convertQueueToTask(queue);
      assertThat(task.getCollection().getId()).isEqualTo(c2.getId());
      assertThat(task.getDocuments()).hasSize(1);
      assertThat(task.getParents()).hasSize(1);

      task = task.getParents().get(0);
      assertThat(task.getLinkType().getId()).isEqualTo(l12.getId());
      assertThat(task.getLinkInstances()).hasSize(2);
      assertThat(task.getParents()).isNullOrEmpty();
   }

   private List<LinkInstance> createLinks(LinkType lt, List<Document> docs1, List<Document> docs2) {
      List<LinkInstance> linkInstances = new ArrayList<>();
      for (Document doc1 : docs1) {
         for (Document doc2 : docs2) {
            linkInstances.add(linkInstanceDao.createLinkInstance(new LinkInstance(lt.getId(), Arrays.asList(doc1.getId(), doc2.getId()))));
         }
      }
      return linkInstances;
   }

   private LinkType getLinkType(Collection coll1, Collection coll2) {
      return linkTypeDao.getLinkTypesByCollectionId(coll1.getId()).
            stream().filter(lt -> lt.getCollectionIds().containsAll(Arrays.asList(coll1.getId(), coll2.getId())))
                        .findFirst().get();
   }

   private Attribute getAttribute(Collection collection, String attributeId){
      return collection.getAttributes().stream().filter(attr -> attr.getId().equals(attributeId)).findFirst().get();
   }

   private Attribute getAttribute(LinkType linkType, String attributeId){
      return linkType.getAttributes().stream().filter(attr -> attr.getId().equals(attributeId)).findFirst().get();
   }

   private List<Document> getDocuments(Collection collection) {
      return documentDao.getDocumentsByCollection(collection.getId());
   }

   private Document getAnyDocument(Collection collection) {
      return getDocuments(collection).get(0);
   }

   private LinkInstance getAnyLink(LinkType linkType){
      return linkInstanceDao.getLinkInstancesByLinkType(linkType.getId()).get(0);
   }

   private void createTestData() {
      c1 = createCollectionWithAttributes("C1", "a1", "a2", "a3", "a4", "a5");
      createDocumentsForCollection(c1);

      c2 = createCollectionWithAttributes("C2", "a1", "a2", "a3", "a4", "a5");
      createDocumentsForCollection(c2);

      c3 = createCollectionWithAttributes("C3", "a1", "a2", "a3", "a4", "a5");
      createDocumentsForCollection(c3);

      c4 = createCollectionWithAttributes("C4", "a1", "a2", "a3", "a4", "a5");
      createDocumentsForCollection(c4);

      c5 = createCollectionWithAttributes("C5", "a1", "a2", "a3", "a4", "a5");
      createDocumentsForCollection(c5);

      createLinkWithAttribute("L12", Arrays.asList(c1.getId(), c2.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L13", Arrays.asList(c1.getId(), c3.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L14", Arrays.asList(c1.getId(), c4.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L23", Arrays.asList(c2.getId(), c3.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L24", Arrays.asList(c2.getId(), c4.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L25", Arrays.asList(c2.getId(), c5.getId()), "a1", "a2", "a3", "a4", "a5");
      createLinkWithAttribute("L51", Arrays.asList(c5.getId(), c1.getId()), "a1", "a2", "a3", "a4", "a5");
   }

   private Collection createCollectionWithAttributes(String code, String... attributeIds) {
      Set<Attribute> attributes = Arrays.stream(attributeIds).map(attributeId ->
            new Attribute(attributeId, attributeId, null, new Function("", "", "", 0, false), 1))
                                        .collect(Collectors.toSet());
      return createCollection(code, attributes);
   }

   private Collection createCollection(String code, Set<Attribute> attributes) {
      Collection collection = new Collection(code, code, "", "", null);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      collection.setAttributes(attributes);
      return collectionDao.createCollection(collection);
   }

   private LinkType createLinkWithAttribute(String name, List<String> collectionIds, String... attributeIds) {
      List<Attribute> attributes = Arrays.stream(attributeIds).map(attributeId ->
            new Attribute(attributeId, attributeId, null, new Function("", "", "", 0, false), 1))
                                         .collect(Collectors.toList());
      LinkType linKType = new LinkType(name, collectionIds, attributes);
      return linkTypeDao.createLinkType(linKType);
   }

   private List<Document> createDocumentsForCollection(Collection collection) {
      List<Document> documents = new ArrayList<>();
      ZonedDateTime time = ZonedDateTime.now().withNano(0);
      for (int i = 0; i < 5; i++) {
         documents.add(documentDao.createDocument(new Document(collection.getId(), time, null, "Joey", null, 1, new DataDocument())));
      }
      return documents;
   }

}
