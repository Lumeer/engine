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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.js.AutomationCodeMigrationReport;
import io.lumeer.core.js.AutomationCodeMigrationReport.Status;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class AutomationCodeMigrationFacadeIT extends IntegrationTestBase {

   private static final String HEADER = "var newRecord, oldRecord;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\n";

   private static final String LEGACY_RULE = HEADER
         + "lumeer.setDocumentAttribute(newRecord, 'a2', formatMomentJsDate(/** MomentJs **/ moment((lumeer.getDocumentAttribute(newRecord, 'a1'))).add((1), 'days').toDate(), 'YYYY-MM-DD', 'en') + ' ' + /** he.js **/ he.escape('<b>'));\n";
   private static final String MIGRATED_RULE = HEADER
         + "lumeer.setDocumentAttribute(newRecord, 'a2', lumeer.formatDate(new Date(lumeer.changeDate((lumeer.getDocumentAttribute(newRecord, 'a1')), 'add', (1), 'days')), 'YYYY-MM-DD', 'en') + ' ' + lumeer.escapeHtml('<b>'));\n";

   private static final String UNKNOWN_RULE = HEADER + "lumeer.setDocumentAttribute(newRecord, 'a2', moment(lumeer.getDocumentAttribute(newRecord, 'a1')).add(1, 'days').toDate());\n";
   private static final String NEW_RULE = HEADER + "lumeer.setDocumentAttribute(newRecord, 'a2', lumeer.formatDate(new Date(), 'YYYY', 'en'));\n";

   private static final String LEGACY_FUNCTION = "var thisRecord;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\nlumeer.setDocumentAttribute(thisRecord, 'a3', /** numbro.js **/ lumeer_numbro('cs-CZ', 2, 1234.5));";
   private static final String MIGRATED_FUNCTION = "var thisRecord;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\nlumeer.setDocumentAttribute(thisRecord, 'a3', lumeer.formatCurrency('cs-CZ', 2, 1234.5));";

   private static final String LEGACY_LINK_RULE = "var newLink, oldLink, lumeer_date_tmp;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\n"
         + "lumeer.setLinkAttribute(newLink, 'a1', /** MomentJs **/ ((lumeer_date_tmp = moment((lumeer.getLinkAttribute(newLink, 'a2')))).daysInMonth() === lumeer_date_tmp.date() ? lumeer_date_tmp.add((1), 'months').endOf('months') : lumeer_date_tmp.add((1), 'months')).toDate());\n";
   private static final String MIGRATED_LINK_RULE = "var newLink, oldLink, lumeer_date_tmp;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\n"
         + "lumeer.setLinkAttribute(newLink, 'a1', new Date(lumeer.changeDate((lumeer.getLinkAttribute(newLink, 'a2')), 'add', (1), 'sticky_months')));\n";

   private static final String LEGACY_LINK_FUNCTION = "var thisLink;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\nlumeer.setLinkAttribute(thisLink, 'a3', /** he.js **/ he.unescape(lumeer.getLinkAttribute(thisLink, 'a2')));";
   private static final String MIGRATED_LINK_FUNCTION = "var thisLink;\n\n\nvar lumeer = Polyglot.import('lumeer');\n\nlumeer.setLinkAttribute(thisLink, 'a3', lumeer.unescapeHtml(lumeer.getLinkAttribute(thisLink, 'a2')));";

   private static final String XML = "<xml xmlns=\"http://www.w3.org/1999/xhtml\"><block type=\"test\"/></xml>";

   @Inject
   private AutomationCodeMigrationFacade migrationFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private DocumentFacade documentFacade;

   private User user;
   private Organization organization1;
   private Organization organization2;
   private Project project1;
   private Project project2;
   private Project project3;
   private Collection collection1;
   private LinkType linkType1;
   private Collection collection2;
   private Collection collection3;

   @BeforeEach
   public void configure() {
      user = userDao.createUser(new User(AuthenticatedUser.DEFAULT_EMAIL));

      organization1 = createOrganization("MIGIT1");
      organization2 = createOrganization("MIGIT2");
      project1 = createProject(organization1, "MIGP1");
      project2 = createProject(organization1, "MIGP2");
      project3 = createProject(organization2, "MIGP3");

      collection1 = createCollection(project1, "c1");
      linkType1 = createLinkType(project1, collection1);
      collection2 = createCollection(project2, "c2");
      collection3 = createCollection(project3, "c3");

      projectDao.setOrganization(organization1);
      workspaceKeeper.setWorkspaceIds(organization1.getId(), project1.getId());
      collectionDao.setProject(project1);
   }

   private Organization createOrganization(final String code) {
      final Organization organization = new Organization();
      organization.setCode(code);
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(user.getId(), Collections.singleton(new Role(RoleType.Read))));
      return organizationDao.createOrganization(organization);
   }

   private Project createProject(final Organization organization, final String code) {
      projectDao.setOrganization(organization);
      final Project project = new Project();
      project.setCode(code);
      final Permissions permissions = new Permissions();
      permissions.updateUserPermissions(Permission.buildWithRoles(user.getId(), Project.ROLES));
      project.setPermissions(permissions);
      return projectDao.createProject(project);
   }

   private static Rule blocklyRule(final String name, final Rule.RuleTiming timing, final String js) {
      final BlocklyRule rule = new BlocklyRule(new Rule(name, Rule.RuleType.BLOCKLY, timing, new DataDocument()));
      rule.setXml(XML);
      rule.setJs(js);
      rule.setDryRun(false);
      rule.setResultTimestamp(0);
      return rule.getRule();
   }

   private static Attribute attribute(final String id, final Function function) {
      return new Attribute(id, id, null, null, null, null, function, 0, null);
   }

   private Collection createCollection(final Project project, final String code) {
      collectionDao.setProject(project);
      final Collection collection = new Collection(code, code, "fa-cube", "#ff0000", null);
      collection.getPermissions().updateUserPermissions(Permission.buildWithRoles(user.getId(), Collection.ROLES));
      collection.updateAttribute("a1", attribute("a1", null));
      collection.updateAttribute("a2", attribute("a2", null));
      collection.updateAttribute("a3", attribute("a3", new Function(LEGACY_FUNCTION, XML, null, 0L, true)));
      final Map<String, Rule> rules = new HashMap<>();
      rules.put("legacy", blocklyRule("Legacy", Rule.RuleTiming.CREATE, LEGACY_RULE));
      rules.put("unknown", blocklyRule("Unknown", Rule.RuleTiming.UPDATE, UNKNOWN_RULE));
      rules.put("new", blocklyRule("New", Rule.RuleTiming.DELETE, NEW_RULE));
      collection.setRules(rules);
      return collectionDao.createCollection(collection);
   }

   private LinkType createLinkType(final Project project, final Collection collection) {
      linkTypeDao.setProject(project);
      final List<Attribute> attributes = new ArrayList<>(List.of(attribute("a1", null), attribute("a2", null), attribute("a3", new Function(LEGACY_LINK_FUNCTION, XML, null, 0L, true))));
      final Map<String, Rule> rules = new HashMap<>(Map.of("legacy", blocklyRule("Legacy link", Rule.RuleTiming.CREATE, LEGACY_LINK_RULE)));
      return linkTypeDao.createLinkType(new LinkType("lt", List.of(collection.getId(), collection.getId()), attributes, rules, null, null));
   }

   private Collection storedCollection(final Project project, final String id) {
      collectionDao.setProject(project);
      return collectionDao.getCollectionById(id);
   }

   private LinkType storedLinkType(final Project project, final String id) {
      linkTypeDao.setProject(project);
      return linkTypeDao.getLinkType(id);
   }

   private static String ruleJs(final Collection collection, final String ruleId) {
      return new BlocklyRule(collection.getRules().get(ruleId)).getJs();
   }

   private static String functionJs(final List<Attribute> attributes, final String attributeId) {
      return attributes.stream().filter(a -> a.getId().equals(attributeId)).findFirst().orElseThrow().getFunction().getJs();
   }

   private static List<String> items(final AutomationCodeMigrationReport report, final Project project) {
      return report.getItems().stream()
                   .filter(item -> item.getLocation().getProjectId().equals(project.getId()))
                   .map(item -> (item.getRuleId() != null ? "rule " + item.getRule() : "function " + item.getAttribute()) + " " + (item.getLocation().getLinkType() != null ? "of link type" : "of collection") + ": " + item.getStatus())
                   .sorted()
                   .collect(Collectors.toList());
   }

   private void assertUnchanged(final Project project, final Collection collection) {
      final Collection stored = storedCollection(project, collection.getId());
      assertThat(ruleJs(stored, "legacy")).isEqualTo(LEGACY_RULE);
      assertThat(ruleJs(stored, "unknown")).isEqualTo(UNKNOWN_RULE);
      assertThat(functionJs(new ArrayList<>(stored.getAttributes()), "a3")).isEqualTo(LEGACY_FUNCTION);
      assertThat(stored.getVersion()).isEqualTo(collection.getVersion());
   }

   @Test
   public void dryRunReportsWithoutWriting() {
      final AutomationCodeMigrationReport report = migrationFacade.dryRun(organization1.getId(), null);

      assertThat(report.getDryRun()).isTrue();
      assertThat(report.getOrganizations()).isEqualTo(1);
      assertThat(report.getProjects()).isEqualTo(2);
      assertThat(report.getErrors()).isEmpty();
      assertThat(items(report, project1)).containsExactly(
            "function a3 of collection: WOULD_MIGRATE",
            "function a3 of link type: WOULD_MIGRATE",
            "rule Legacy link of link type: WOULD_MIGRATE",
            "rule Legacy of collection: WOULD_MIGRATE",
            "rule Unknown of collection: NOT_MIGRATED");
      assertThat(items(report, project2)).hasSize(3);
      assertThat(items(report, project3)).isEmpty();

      final AutomationCodeMigrationReport.Item legacy = report.getItems().stream()
                                                             .filter(item -> item.getLocation().getProjectId().equals(project1.getId()) && "legacy".equals(item.getRuleId()) && item.getLocation().getCollectionId() != null)
                                                             .findFirst().orElseThrow();
      assertThat(legacy.getLocation().getOrganization()).isEqualTo("MIGIT1");
      assertThat(legacy.getLocation().getProject()).isEqualTo("MIGP1");
      assertThat(legacy.getLocation().getCollectionId()).isEqualTo(collection1.getId());
      assertThat(legacy.getReplacements()).hasSize(2);
      assertThat(legacy.getLegacyCallsLeft()).isEmpty();
      final AutomationCodeMigrationReport.Item unknown = report.getItems().stream().filter(item -> "unknown".equals(item.getRuleId()) && item.getLocation().getProjectId().equals(project1.getId())).findFirst().orElseThrow();
      assertThat(unknown.getLegacyCallsLeft()).containsExactly("moment");

      assertUnchanged(project1, collection1);
      assertUnchanged(project2, collection2);
      assertThat(ruleJs(storedCollection(project1, collection1.getId()), "new")).isEqualTo(NEW_RULE);
      final LinkType storedLinkType = storedLinkType(project1, linkType1.getId());
      assertThat(new BlocklyRule(storedLinkType.getRules().get("legacy")).getJs()).isEqualTo(LEGACY_LINK_RULE);
   }

   @Test
   public void dryRunCoversAllOrganizations() {
      final AutomationCodeMigrationReport report = migrationFacade.dryRun(null, null);

      assertThat(report.getErrors()).isEmpty();
      assertThat(items(report, project1)).hasSize(5);
      assertThat(items(report, project2)).hasSize(3);
      assertThat(items(report, project3)).hasSize(3);

      final AutomationCodeMigrationReport onlyProject = migrationFacade.dryRun(organization2.getId(), project3.getId());
      assertThat(onlyProject.getProjects()).isEqualTo(1);
      assertThat(onlyProject.getItems()).allMatch(item -> item.getLocation().getProjectId().equals(project3.getId()));
   }

   @Test
   public void migratesOnlyTheProject() {
      final AutomationCodeMigrationReport report = migrationFacade.migrateProject(organization1.getId(), project1.getId());

      assertThat(report.getDryRun()).isFalse();
      assertThat(report.getErrors()).isEmpty();
      assertThat(items(report, project1)).containsExactly(
            "function a3 of collection: MIGRATED",
            "function a3 of link type: MIGRATED",
            "rule Legacy link of link type: MIGRATED",
            "rule Legacy of collection: MIGRATED",
            "rule Unknown of collection: NOT_MIGRATED");
      assertThat(report.getItems()).allMatch(item -> item.getLocation().getProjectId().equals(project1.getId()));

      final Collection stored = storedCollection(project1, collection1.getId());
      assertThat(ruleJs(stored, "legacy")).isEqualTo(MIGRATED_RULE);
      assertThat(ruleJs(stored, "unknown")).isEqualTo(UNKNOWN_RULE);
      assertThat(ruleJs(stored, "new")).isEqualTo(NEW_RULE);
      assertThat(new BlocklyRule(stored.getRules().get("legacy")).getXml()).isEqualTo(XML);
      assertThat(stored.getRules().get("legacy").getName()).isEqualTo("Legacy");
      assertThat(stored.getRules().get("legacy").getTiming()).isEqualTo(Rule.RuleTiming.CREATE);
      assertThat(functionJs(new ArrayList<>(stored.getAttributes()), "a3")).isEqualTo(MIGRATED_FUNCTION);
      assertThat(stored.getVersion()).isEqualTo(collection1.getVersion() + 2);

      final LinkType storedLinkType = storedLinkType(project1, linkType1.getId());
      assertThat(new BlocklyRule(storedLinkType.getRules().get("legacy")).getJs()).isEqualTo(MIGRATED_LINK_RULE);
      assertThat(functionJs(storedLinkType.getAttributes(), "a3")).isEqualTo(MIGRATED_LINK_FUNCTION);

      // other projects, also in the same organization, stay
      assertUnchanged(project2, collection2);
      assertUnchanged(project3, collection3);

      // a second run finds nothing more to migrate
      final AutomationCodeMigrationReport again = migrationFacade.migrateProject(organization1.getId(), project1.getId());
      assertThat(items(again, project1)).containsExactly("rule Unknown of collection: NOT_MIGRATED");
      assertThat(storedCollection(project1, collection1.getId()).getVersion()).isEqualTo(stored.getVersion());
   }

   @Test
   public void migratedRuleGivesTheSameResult() throws InterruptedException {
      final String before = createDocumentAndRead();
      migrationFacade.migrateProject(organization1.getId(), project1.getId());
      final String after = createDocumentAndRead();

      assertThat(before).isEqualTo("2026-02-01 &lt;b&gt;");
      assertThat(after).isEqualTo(before);
   }

   private String createDocumentAndRead() throws InterruptedException {
      workspaceKeeper.setWorkspaceIds(organization1.getId(), project1.getId());
      final Document document = documentFacade.createDocument(collection1.getId(), new Document(new DataDocument("a1", "2026-01-31T10:00:00.000Z")));
      Object value = null;
      for (int i = 0; i < 20 && value == null; i++) {
         Thread.sleep(500);
         value = documentFacade.getDocument(collection1.getId(), document.getId()).getData().get("a2");
      }
      return value != null ? value.toString() : null;
   }

   @Test
   public void rejectsProjectsOutsideTheOrganization() {
      assertThatThrownBy(() -> migrationFacade.migrateProject(organization2.getId(), project1.getId())).isInstanceOf(ResourceNotFoundException.class);
      assertThatThrownBy(() -> migrationFacade.dryRun(organization2.getId(), project1.getId())).isInstanceOf(ResourceNotFoundException.class);
      assertThatThrownBy(() -> migrationFacade.dryRun(null, project1.getId())).isInstanceOf(BadFormatException.class);
      assertUnchanged(project1, collection1);
   }
}
