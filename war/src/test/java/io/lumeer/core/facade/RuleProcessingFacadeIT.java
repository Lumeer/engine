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

import static org.assertj.core.api.Assertions.assertThat;

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
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RunWith(Arquillian.class)
public class RuleProcessingFacadeIT extends IntegrationTestBase {

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private GroupDao groupDao;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String STRANGER_USER = "stranger@nowhere.com";
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String CODE3 = "TPROJ3";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private Permission userPermissions;
   private Permission userReadonlyPermissions;
   private Permission userStrangerPermissions;
   private Permission groupPermissions;

   private User user;
   private User stranger;
   private Organization organization;
   private Group group;
   private Project project;

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   @Before
   public void configureProject() {
      this.user = userDao.createUser(new User(USER));
      this.stranger = userDao.createUser(new User(STRANGER_USER));

      userPermissions = Permission.buildWithRoles(this.user.getId(), Project.ROLES);
      userReadonlyPermissions = Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ));
      userStrangerPermissions = Permission.buildWithRoles(this.stranger.getId(), Collections.singleton(Role.READ));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      Group group = new Group(GROUP);
      this.group = groupDao.createGroup(group);
      groupPermissions = Permission.buildWithRoles(this.group.getId(), Collections.singleton(Role.READ));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(project);
   }

   private Collection createCollection(final String code, final String name, Map<String, String> attributes) {
      Collection collection = new Collection(code, name, ICON, COLOR, null);
      collection.getPermissions().updateUserPermissions(userPermissions);
      collection.getPermissions().updateGroupPermissions(groupPermissions);
      attributes.entrySet().forEach(e -> {
         Attribute a = new Attribute(e.getKey(), e.getValue(), null, null, 0);
         collection.updateAttribute(e.getKey(), a);
      });
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testBlocklyRules() throws InterruptedException {
      final String ruleName = "blocklyRule";
      final Collection c1 = createCollection("c1", "name1", Map.of("a0", "A", "a1", "B"));
      final Collection c2 = createCollection("c2", "name2", Map.of("a0", "C", "a1", "D"));
      final LinkType l = linkTypeFacade.createLinkType(new LinkType(null, "link1", List.of(c1.getId(), c2.getId()), Collections.emptyList()));

      final Document c1d1 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line1").append("a1", 10)));
      final Document c1d2 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line2").append("a1", 20)));

      final Document c2d1 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline1").append("a1", "")));
      final Document c2d2 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline2").append("a1", "")));
      final Document c2d3 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline3").append("a1", "")));

      linkInstanceFacade.createLinkInstance(new LinkInstance(null, l.getId(), List.of(c1d1.getId(), c2d1.getId()), Collections.emptyMap()));
      linkInstanceFacade.createLinkInstance(new LinkInstance(null, l.getId(), List.of(c1d1.getId(), c2d2.getId()), Collections.emptyMap()));

      final BlocklyRule rule = new BlocklyRule(new Rule(Rule.RuleType.BLOCKLY, Rule.RuleTiming.UPDATE, new DataDocument()));
      rule.setDryRun(true);
      rule.setJs("var i, newDocument;\n"
            + "\n"
            + "\n"
            + "var lumeer = Polyglot.import('lumeer');\n"
            + "  var i_list = lumeer.getLinkedDocuments(newDocument, '" + l.getId() + "');\n"
            + "  for (var i_index in i_list) {\n"
            + "    i = i_list[i_index];\n"
            + "    lumeer.setDocumentAttribute(i, 'a1', lumeer.getDocumentAttribute(newDocument, 'a1'))}\n");

      c1.getRules().put(ruleName, rule.getRule());
      collectionFacade.updateCollection(c1.getId(), c1);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 11));

      Collection updatedCollection;
      BlocklyRule updatedRule;
      int cycles = 10;
      do {
         Thread.sleep(500);
         updatedCollection = collectionFacade.getCollection(c1.getId());
         updatedRule = new BlocklyRule(updatedCollection.getRules().get(ruleName));
      } while (updatedRule.getDryRunResult() == null && cycles-- > 0);

      assertThat(updatedRule.getDryRunResult()).matches(Pattern.compile("^name2......: D = 11\\.0\nname2......: D = 11\\.0\n$"));
      assertThat(System.currentTimeMillis() - updatedRule.getResultTimestamp()).isLessThan(5000);

      // It was a dry run, no changes should have been introduced
      Document c2d1updated = documentFacade.getDocument(c2d1.getCollectionId(), c2d1.getId());
      Document c2d2updated = documentFacade.getDocument(c2d2.getCollectionId(), c2d2.getId());

      assertThat(c2d1updated.getData().get("a1")).isInstanceOf(String.class).isEqualTo("");
      assertThat(c2d2updated.getData().get("a1")).isInstanceOf(String.class).isEqualTo("");

      // Run it for real
      rule.setDryRun(false);
      rule.setResultTimestamp(0);
      c1.getRules().put(ruleName, rule.getRule());
      collectionFacade.updateCollection(c1.getId(), c1);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 12));

      cycles = 10;
      do {
         Thread.sleep(500);
         updatedCollection = collectionFacade.getCollection(c1.getId());
         updatedRule = new BlocklyRule(updatedCollection.getRules().get(ruleName));
      } while (updatedRule.getResultTimestamp() == 0 && cycles-- > 0);

      c2d1updated = documentFacade.getDocument(c2d1.getCollectionId(), c2d1.getId());
      c2d2updated = documentFacade.getDocument(c2d2.getCollectionId(), c2d2.getId());

      assertThat(c2d1updated.getData().get("a1")).isInstanceOf(Number.class).isEqualTo(12.0);
      assertThat(c2d2updated.getData().get("a1")).isInstanceOf(Number.class).isEqualTo(12.0);
   }

   @Test
   public void testSyntaxExceptionBlocklyRules() throws InterruptedException {
      final String ruleName = "blocklyRule";
      final String syntaxException = "  syntax exception @#$~@#$~@$";
      final Collection c1 = createCollection("c1", "name1", Map.of("a0", "A", "a1", "B"));

      final Document c1d1 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line1").append("a1", 10)));
      final Document c1d2 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line2").append("a1", 20)));

      final BlocklyRule rule = new BlocklyRule(new Rule(Rule.RuleType.BLOCKLY, Rule.RuleTiming.UPDATE, new DataDocument()));
      rule.setDryRun(false);
      rule.setResultTimestamp(0);
      rule.setJs("var i, newDocument;\n"
            + "\n"
            + "\n"
            + "var lumeer = Polyglot.import('lumeer');\n"
            + syntaxException);

      c1.getRules().put(ruleName, rule.getRule());
      collectionFacade.updateCollection(c1.getId(), c1);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 11));

      Collection updatedCollection;
      BlocklyRule updatedRule;
      int cycles = 10;
      do {
         Thread.sleep(500);
         updatedCollection = collectionFacade.getCollection(c1.getId());
         updatedRule = new BlocklyRule(updatedCollection.getRules().get(ruleName));
      } while (updatedRule.getResultTimestamp() == 0 && cycles-- > 0);

      assertThat(updatedRule.getError()).contains(syntaxException);
      assertThat(System.currentTimeMillis() - updatedRule.getResultTimestamp()).isLessThan(5000);
   }

   @Test
   public void testEndlessLoopBlocklyRules() throws InterruptedException {
      final String ruleName = "blocklyRule";
      final Collection c1 = createCollection("c1", "name1", Map.of("a0", "A", "a1", "B"));

      final Document c1d1 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line1").append("a1", 10)));
      final Document c1d2 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line2").append("a1", 20)));

      final BlocklyRule rule = new BlocklyRule(new Rule(Rule.RuleType.BLOCKLY, Rule.RuleTiming.UPDATE, new DataDocument()));
      rule.setDryRun(false);
      rule.setResultTimestamp(0);
      rule.setJs("var i, newDocument;\n"
            + "\n"
            + "\n"
            + "var lumeer = Polyglot.import('lumeer');\n"
            + "while (true) {}");

      c1.getRules().put(ruleName, rule.getRule());
      collectionFacade.updateCollection(c1.getId(), c1);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 11));

      Collection updatedCollection;
      BlocklyRule updatedRule;

      Thread.sleep(5000);
      updatedCollection = collectionFacade.getCollection(c1.getId());
      updatedRule = new BlocklyRule(updatedCollection.getRules().get(ruleName));

      assertThat(updatedRule.getError()).contains("Thread was interrupted");
      // it should have been interrupted after 3000ms
      assertThat(System.currentTimeMillis() - updatedRule.getResultTimestamp()).isLessThan(5000);
   }

   @Test
   public void testAutoLinkRules() throws InterruptedException {
      final String ruleName = "autoLinkRule";
      final Collection c1 = createCollection("ac1", "auto1", Map.of("a0", "A", "a1", "B"));
      final Collection c2 = createCollection("ac2", "auto2", Map.of("a0", "C", "a1", "D"));
      final LinkType l = linkTypeFacade.createLinkType(new LinkType(null, "link1", List.of(c1.getId(), c2.getId()), Collections.emptyList()));

      final Document c1d1 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line1").append("a1", 10)));
      final Document c1d2 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line2").append("a1", 20)));

      final Document c2d1 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline1").append("a1", 10)));
      final Document c2d2 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline2").append("a1", 20)));
      final Document c2d3 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline3").append("a1", 20)));
      final Document c2d4 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline3").append("a1", 30)));
      final Document c2d5 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline3").append("a1", 30)));
      final Document c2d6 = documentFacade.createDocument(c2.getId(), new Document(new DataDocument("a0", "subline3").append("a1", 30)));

      final AutoLinkRule rule = new AutoLinkRule(new Rule(Rule.RuleType.AUTO_LINK, Rule.RuleTiming.ALL, new DataDocument()));
      rule.setCollection1(c1.getId());
      rule.setAttribute1("a1");
      rule.setCollection2(c2.getId());
      rule.setAttribute2("a1");
      rule.setLinkType(l.getId());

      c1.getRules().put(ruleName, rule.getRule());
      collectionFacade.updateCollection(c1.getId(), c1);

      assertThat(getLinksByType(l.getId())).hasSize(0);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 11));
      Thread.sleep(1000); // there is no way we can detect the change :-(
      assertThat(getLinksByType(l.getId())).hasSize(0);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 10));
      List<LinkInstance> instances = waitForLinksByType(l.getId());
      assertThat(instances).hasSize(1);
      assertThat(instances.get(0).getDocumentIds()).contains(c1d1.getId(), c2d1.getId());

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 11));
      instances = waitForLinksByType(l.getId());
      assertThat(instances).hasSize(0);

      documentFacade.patchDocumentData(c1.getId(), c1d1.getId(), new DataDocument("a1", 20));
      instances = waitForLinksByType(l.getId());
      assertThat(instances).hasSize(2);
      assertThat(instances.get(0).getDocumentIds()).contains(c1d1.getId()).containsAnyOf(c2d2.getId(), c2d3.getId());
      assertThat(instances.get(1).getDocumentIds()).contains(c1d1.getId()).containsAnyOf(c2d2.getId(), c2d3.getId());

      final Document c1d3 = documentFacade.createDocument(c1.getId(), new Document(new DataDocument("a0", "line3").append("a1", 30)));
      instances = waitForLinksByType(l.getId());
      assertThat(instances).hasSize(5);

      documentFacade.deleteDocument(c1.getId(), c1d1.getId());
      instances = waitForLinksByType(l.getId());
      assertThat(instances).hasSize(3);
   }

   private List<LinkInstance> getLinksByType(final String linkTypeId) {
      final SearchQuery query = SearchQuery
            .createBuilder()
            .stems(Arrays.asList(
                  SearchQueryStem
                        .createBuilder("")
                        .linkTypeIds(Arrays.asList(linkTypeId))
                        .build()))
            .build();

      return linkInstanceDao.searchLinkInstances(query);
   }

   private List<LinkInstance> waitForLinksByType(final String linkTypeId) throws InterruptedException {
      List<LinkInstance> instances;
      int cycles = 10;
      do {
         Thread.sleep(500);
         instances = getLinksByType(linkTypeId);
      } while (instances.size() == 0 && cycles-- > 0);

      // we might have caught it in the middle, reload once more
      Thread.sleep(500);
      instances = getLinksByType(linkTypeId);

      return instances;
   }

}