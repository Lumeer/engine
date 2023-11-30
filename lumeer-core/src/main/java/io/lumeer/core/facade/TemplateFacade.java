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

import io.lumeer.api.model.Language;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.TemplateData;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.provider.DataStorageProvider;
import io.lumeer.core.template.CollectionCreator;
import io.lumeer.core.template.DocumentCreator;
import io.lumeer.core.template.FavoriteItemsCreator;
import io.lumeer.core.template.FunctionAndRuleCreator;
import io.lumeer.core.template.LinkInstanceCreator;
import io.lumeer.core.template.LinkTypeCreator;
import io.lumeer.core.template.ResourceCommentCreator;
import io.lumeer.core.template.ResourceVariableCreator;
import io.lumeer.core.template.SelectionListCreator;
import io.lumeer.core.template.SequenceCreator;
import io.lumeer.core.template.TemplateMetadata;
import io.lumeer.core.template.TemplateParser;
import io.lumeer.core.template.ViewCreator;
import io.lumeer.engine.api.event.TemplateCreated;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshotFactory;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@RequestScoped
public class TemplateFacade extends AbstractFacade {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private SequenceFacade sequenceFacade;

   @Inject
   private EventLogFacade eventLogFacade;

   @Inject
   private SelectionListFacade selectionListFacade;

   @Inject
   private ResourceCommentFacade resourceCommentFacade;

   @Inject
   private ResourceVariableFacade resourceVariableFacade;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Event<TemplateCreated> templateCreatedEvent;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private DaoContextSnapshotFactory daoContextSnapshotFactory;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   public List<Project> getTemplates() {
      final Language language = requestDataKeeper.getUserLanguage();
      final String organizationId = getTemplateOrganizationId(language);

      if (StringUtils.isEmpty(organizationId)) {
         return List.of();
      }

      var organization = organizationDao.getOrganizationById(organizationId);
      workspaceKeeper.setOrganization(organization);
      projectDao.switchOrganization();
      return projectFacade.getPublicProjects().stream()
            .peek(project -> setProjectOrganizationId(project, organizationId))
            .collect(Collectors.toList());
   }

   public Project getTemplate(final Language language, final String templateCode) {
      final String organizationId = getTemplateOrganizationId(language);

      if (StringUtils.isEmpty(organizationId)) {
         throw new BadRequestException("Could not find template organization");
      }

      var organization = organizationDao.getOrganizationById(organizationId);
      workspaceKeeper.setOrganization(organization);
      var project = projectFacade.getPublicProject(templateCode);
      setProjectOrganizationId(project, organizationId);
      return project;
   }

   private void setProjectOrganizationId(Project project, String organizationId) {
      if (project.getTemplateMetadata() == null) {
         project.setTemplateMetadata(new io.lumeer.api.model.TemplateMetadata());
      }
      project.getTemplateMetadata().setOrganizationId(organizationId);
   }

   public String getTemplateOrganizationId(final Language language) {
      switch (language) {
         case CS:
            return Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.TEMPLATE_ORG_CS)).orElse("");
         default:
            return Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.TEMPLATE_ORG_EN)).orElse("");
      }
   }

   public String getSampleDataOrganizationId(final Language language) {
      switch (language) {
         case CS:
            return Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SAMPLE_DATA_ORG_CS)).orElse("");
         default:
            return Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SAMPLE_DATA_ORG_EN)).orElse("");
      }
   }

   public List<String> getAllTemplateOrganizationIds() {
      final List<String> result = new ArrayList<>();

      final String csOrg = defaultConfigurationProducer.get(DefaultConfigurationProducer.TEMPLATE_ORG_CS);
      final String enOrg = defaultConfigurationProducer.get(DefaultConfigurationProducer.TEMPLATE_ORG_EN);

      if (csOrg != null) {
         result.add(csOrg);
      }

      if (enOrg != null) {
         result.add(enOrg);
      }

      return result;
   }

   public void installTemplate(final Organization organization, final Project project, final String templateType, final Language language) {
      checkProjectContribute(project);

      final TemplateParser templateParser = new TemplateParser(templateType, language);

      installTemplate(organization.getId(), project, templateParser, createTemplateMetadata(new Date()), true);
   }

   public void installTemplate(final Organization organization, final Project project, final String originalOrganizationId, final TemplateData templateData) {
      checkProjectContribute(project);

      final TemplateParser templateParser = new TemplateParser(templateData.getContent());
      final boolean originalLumeerTemplate = getAllTemplateOrganizationIds().contains(originalOrganizationId);

      var relativeDateMillis = templateData.getMetadata() != null ? templateData.getMetadata().getRelativeDate() : null;
      var relativeDate = relativeDateMillis != null ? new Date(relativeDateMillis) : null;

      installTemplate(organization.getId(), project, templateParser, createTemplateMetadata(relativeDate), originalLumeerTemplate);
   }

   public void installProjectContent(final Organization organization, final Project project, final ProjectContent projectContent) {
      checkProjectContribute(project);

      final TemplateParser templateParser = new TemplateParser(projectContent);

      eventLogFacade.logEvent(authenticatedUser.getCurrentUser(), "Imported to project: " + organization.getCode() + " / " + project.getCode());

      installTemplate(organization.getId(), project, templateParser, createTemplateMetadata(new Date()), false);
   }

   private TemplateMetadata createTemplateMetadata(final Date relativeDate) {
      long dateAddition = 0;
      if (relativeDate != null) {
         dateAddition = new Date().getTime() - relativeDate.getTime();
      }

      return new TemplateMetadata(dateAddition);
   }

   private void installTemplate(final String organizationId, final Project project, final TemplateParser templateParser, final TemplateMetadata templateMetadata, final boolean originalLumeerTemplate) {
      SelectionListCreator.createLists(templateParser, project, selectionListFacade);
      CollectionCreator.createCollections(templateParser, collectionFacade, defaultConfigurationProducer, originalLumeerTemplate);
      LinkTypeCreator.createLinkTypes(templateParser, linkTypeFacade, originalLumeerTemplate);
      DocumentCreator.createDocuments(templateParser, documentFacade, authenticatedUser, templateMetadata, permissionsChecker.getDocumentLimits());
      LinkInstanceCreator.createLinkInstances(templateParser, linkInstanceFacade, authenticatedUser, templateMetadata);
      ViewCreator.createViews(templateParser, viewFacade, defaultConfigurationProducer);
      FunctionAndRuleCreator.createFunctionAndRules(templateParser, collectionFacade, linkTypeFacade, originalLumeerTemplate);
      FavoriteItemsCreator.createFavoriteItems(templateParser, collectionFacade, viewFacade);
      SequenceCreator.createSequences(templateParser, sequenceFacade);
      ResourceCommentCreator.createComments(templateParser, resourceCommentFacade, defaultConfigurationProducer);
      ResourceVariableCreator.createVariables(templateParser, resourceVariableFacade, defaultConfigurationProducer, organizationId, project.getId());

      if (templateCreatedEvent != null) {
         templateCreatedEvent.fire(templateParser.getReport(project));
      }
   }

   public TemplateData getTemplateData(String organizationId, String projectId) {
      return getTemplateData(organizationId, dao -> dao.getProjectById(projectId));
   }

   public TemplateData getTemplateDataByCode(String organizationId, String projectCode) {
      return getTemplateData(organizationId, dao -> dao.getProjectByCode(projectCode));
   }

   public TemplateData getTemplateData(String organizationId,  java.util.function.Function<ProjectDao, Project> projectFunction) {
      final StringBuilder sb = new StringBuilder();
      var fromOrganization = organizationDao.getOrganizationById(organizationId);
      workspaceKeeper.push();
      workspaceKeeper.setOrganization(fromOrganization);

      var storage = dataStorageProvider.getUserStorage();
      var contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var fromProject = projectFunction.apply(contextSnapshot.getProjectDao());
      workspaceKeeper.setWorkspace(fromOrganization, fromProject);
      sb.append("Copied project from ").append(fromOrganization.getCode()).append("/").append(fromProject.getCode());

      storage = dataStorageProvider.getUserStorage();
      contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var facade = new ProjectFacade();
      facade.init(authenticatedUser, contextSnapshot, workspaceKeeper);
      var content = facade.getRawProjectContent(fromProject.getId());

      workspaceKeeper.pop();

      eventLogFacade.logEvent(authenticatedUser.getCurrentUser(), sb.toString());

      return new TemplateData(fromProject.getTemplateMetadata(), content);
   }

   private void checkProjectContribute(Project project) {
      permissionsChecker.checkAllRoles(project, Set.of(RoleType.Read, RoleType.LinkContribute, RoleType.ViewContribute, RoleType.CollectionContribute));
   }
}
