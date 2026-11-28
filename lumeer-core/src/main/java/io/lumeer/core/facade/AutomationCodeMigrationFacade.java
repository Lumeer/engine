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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.WorkspaceContext;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.js.AutomationCodeMigrationReport;
import io.lumeer.core.js.AutomationCodeMigrationReport.Failure;
import io.lumeer.core.js.AutomationCodeMigrationReport.Item;
import io.lumeer.core.js.AutomationCodeMigrationReport.Location;
import io.lumeer.core.js.AutomationCodeMigrationReport.Status;
import io.lumeer.core.js.JsEngineFactory;
import io.lumeer.core.js.LegacyCodeMigration;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.graalvm.polyglot.Context;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Migrates the automation code stored in rules and attribute functions from the legacy calls (moment, numbro, he, run
 * through LegacyScriptShim) to the lumeer bridge calls the blocks generate now, see {@link LegacyCodeMigration}.
 * The Blockly XML stays as it is. A dry run reports what would change in all organizations and projects; the migration
 * itself runs for one project.
 */
@RequestScoped
public class AutomationCodeMigrationFacade extends WorkspaceContext {

   private static final Logger log = Logger.getLogger(AutomationCodeMigrationFacade.class.getName());

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   /**
    * Reports what the migration would change, in all organizations and projects or only in the given ones.
    */
   public AutomationCodeMigrationReport dryRun(final String organizationId, final String projectId) {
      permissionsChecker.checkSystemPermission();
      if (projectId != null && organizationId == null) {
         throw new BadFormatException("organizationId is required with projectId");
      }

      final AutomationCodeMigrationReport report = new AutomationCodeMigrationReport(true);
      final List<Organization> organizations = organizationId != null ? List.of(organizationDao.getOrganizationById(organizationId)) : organizationDao.getAllOrganizations();
      try (Context context = parsingContext()) {
         organizations.forEach(organization -> processOrganization(report, context, organization, projectId));
      }
      return report;
   }

   /**
    * Migrates the code in one project.
    */
   public AutomationCodeMigrationReport migrateProject(final String organizationId, final String projectId) {
      permissionsChecker.checkSystemPermission();

      final AutomationCodeMigrationReport report = new AutomationCodeMigrationReport(false);
      final Organization organization = organizationDao.getOrganizationById(organizationId);
      try (Context context = parsingContext()) {
         processOrganization(report, context, organization, projectId);
      }
      log.info(String.format("Automation code migration in %s/%s: %d changes, %d legacy calls left, %d errors.",
            organization.getCode(), projectId, report.getChanges(), report.getLegacyCallsLeft(), report.getErrors().size()));
      return report;
   }

   private void processOrganization(final AutomationCodeMigrationReport report, final Context context, final Organization organization, final String projectId) {
      report.setOrganizations(report.getOrganizations() + 1);
      final DataStorage dataStorage;
      final List<Project> projects;
      try {
         dataStorage = getDataStorage(organization.getId());
         final DaoContextSnapshot organizationSnapshot = getDaoContextSnapshot(dataStorage, new Workspace(organization, null));
         // a project that is not in the organization fails the request (not found)
         projects = projectId != null ? List.of(organizationSnapshot.getProjectDao().getProjectById(projectId)) : organizationSnapshot.getProjectDao().getAllProjects();
      } catch (ResourceNotFoundException e) {
         throw e;
      } catch (Exception e) {
         log.log(Level.WARNING, "Automation code migration cannot read organization " + organization.getCode(), e);
         report.getErrors().add(new Failure(organization.getId(), organization.getCode(), null, null, String.valueOf(e.getMessage())));
         return;
      }

      for (final Project project : projects) {
         report.setProjects(report.getProjects() + 1);
         try {
            final DaoContextSnapshot dao = getDaoContextSnapshot(dataStorage, new Workspace(organization, project));
            new ProjectMigration(report, context, dao, organization, project).run();
         } catch (Exception e) {
            log.log(Level.WARNING, "Automation code migration failed in " + organization.getCode() + "/" + project.getCode(), e);
            report.getErrors().add(new Failure(organization.getId(), organization.getCode(), project.getId(), project.getCode(), String.valueOf(e.getMessage())));
         }
      }
   }

   private static Context parsingContext() {
      return Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build();
   }

   private static class ProjectMigration {
      private final AutomationCodeMigrationReport report;
      private final Context context;
      private final DaoContextSnapshot dao;
      private final Organization organization;
      private final Project project;

      ProjectMigration(final AutomationCodeMigrationReport report, final Context context, final DaoContextSnapshot dao, final Organization organization, final Project project) {
         this.report = report;
         this.context = context;
         this.dao = dao;
         this.organization = organization;
         this.project = project;
      }

      /** Writes the migrated code of a rule or function (by its id), only when it still is the original code. */
      private interface CodeWriter {
         boolean write(String id, String originalJs, String js);
      }

      void run() {
         for (final Collection collection : dao.getCollectionDao().getAllCollections()) {
            final Location location = location(collection.getId(), collection.getName(), null, null);
            migrateRules(location, collection.getRules(), (ruleId, originalJs, js) -> dao.getCollectionDao().updateRuleJs(collection.getId(), ruleId, originalJs, js));
            migrateFunctions(location, collection.getAttributes(), (attributeId, originalJs, js) -> dao.getCollectionDao().updateFunctionJs(collection.getId(), attributeId, originalJs, js));
         }
         for (final LinkType linkType : dao.getLinkTypeDao().getAllLinkTypes()) {
            final Location location = location(null, null, linkType.getId(), linkType.getName());
            migrateRules(location, linkType.getRules(), (ruleId, originalJs, js) -> dao.getLinkTypeDao().updateRuleJs(linkType.getId(), ruleId, originalJs, js));
            migrateFunctions(location, linkType.getAttributes(), (attributeId, originalJs, js) -> dao.getLinkTypeDao().updateFunctionJs(linkType.getId(), attributeId, originalJs, js));
         }
      }

      private Location location(final String collectionId, final String collection, final String linkTypeId, final String linkType) {
         return new Location(organization.getId(), organization.getCode(), project.getId(), project.getCode(), collectionId, collection, linkTypeId, linkType);
      }

      private void migrateRules(final Location location, final Map<String, Rule> rules, final CodeWriter writer) {
         if (rules == null) {
            return;
         }
         rules.forEach((ruleId, rule) -> {
            final Object js = rule.getConfiguration() != null ? rule.getConfiguration().get(BlocklyRule.BLOCKLY_JS) : null;
            if (js instanceof String code) {
               final LegacyCodeMigration.Result result = LegacyCodeMigration.INSTANCE.migrate(code);
               final Status status = migrate(ruleId, code, result, writer);
               if (status != null) {
                  report.getItems().add(new Item(location, ruleId, rule.getName(), null, null, status, result.getReplacements(), legacyCallsLeft(code, status, result), result.getSkipReason()));
               }
            }
         });
      }

      private void migrateFunctions(final Location location, final java.util.Collection<Attribute> attributes, final CodeWriter writer) {
         if (attributes == null) {
            return;
         }
         for (final Attribute attribute : attributes) {
            final String js = attribute.getFunction() != null ? attribute.getFunction().getJs() : null;
            if (js != null) {
               final LegacyCodeMigration.Result result = LegacyCodeMigration.INSTANCE.migrate(js);
               final Status status = migrate(attribute.getId(), js, result, writer);
               if (status != null) {
                  report.getItems().add(new Item(location, null, null, attribute.getId(), attribute.getName(), status, result.getReplacements(), legacyCallsLeft(js, status, result), result.getSkipReason()));
               }
            }
         }
      }

      /** The legacy calls the stored code has after this run: the original code's when the migrated code was not written. */
      private static List<String> legacyCallsLeft(final String js, final Status status, final LegacyCodeMigration.Result result) {
         return status == Status.INVALID || status == Status.CONFLICT ? LegacyCodeMigration.INSTANCE.legacyCalls(js) : result.getLegacyCallsLeft();
      }

      /** Writes the migrated code (unless a dry run); the status to report, null for code without legacy calls. */
      private Status migrate(final String id, final String js, final LegacyCodeMigration.Result result, final CodeWriter writer) {
         if (!result.getChanged()) {
            return result.getLegacyCallsLeft().isEmpty() ? null : Status.NOT_MIGRATED;
         }
         if (!parses(result.getJs())) {
            return Status.INVALID;
         }
         if (report.getDryRun()) {
            return Status.WOULD_MIGRATE;
         }
         return writer.write(id, js, result.getJs()) ? Status.MIGRATED : Status.CONFLICT;
      }

      private boolean parses(final String js) {
         try {
            context.parse("js", js);
            return true;
         } catch (Exception e) {
            return false;
         }
      }
   }
}
