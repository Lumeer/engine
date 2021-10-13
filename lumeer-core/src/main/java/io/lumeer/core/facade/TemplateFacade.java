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
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.template.CollectionCreator;
import io.lumeer.core.template.DocumentCreator;
import io.lumeer.core.template.FavoriteItemsCreator;
import io.lumeer.core.template.FunctionAndRuleCreator;
import io.lumeer.core.template.LinkInstanceCreator;
import io.lumeer.core.template.LinkTypeCreator;
import io.lumeer.core.template.ResourceCommentCreator;
import io.lumeer.core.template.SelectionListCreator;
import io.lumeer.core.template.SequenceCreator;
import io.lumeer.core.template.TemplateMetadata;
import io.lumeer.core.template.TemplateParser;
import io.lumeer.core.template.ViewCreator;
import io.lumeer.engine.api.event.TemplateCreated;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class TemplateFacade extends AbstractFacade {

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
   private SelectionListFacade selectionListFacade;

   @Inject
   private ResourceCommentFacade resourceCommentFacade;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Event<TemplateCreated> templateCreatedEvent;

   @Inject
   private PermissionsChecker permissionsChecker;

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

   public void installTemplate(final Project project, final String templateType, final Language language) {
      final TemplateParser templateParser = new TemplateParser(templateType, language);

      installTemplate(project, templateParser, createTemplateMetadata(new Date()), true);
   }

   public void installTemplate(final Project project, final String organizationId, final ProjectContent projectContent, final Date relativeDate) {
      final TemplateParser templateParser = new TemplateParser(projectContent);
      final boolean originalLumeerTemplate = getAllTemplateOrganizationIds().contains(organizationId);

      installTemplate(project, templateParser, createTemplateMetadata(relativeDate), originalLumeerTemplate);
   }

   private TemplateMetadata createTemplateMetadata(final Date relativeDate) {
      long dateAddition = 0;
      if (relativeDate != null) {
         dateAddition = new Date().getTime() - relativeDate.getTime();
      }

      return new TemplateMetadata(dateAddition);
   }

   private void installTemplate(final Project project, final TemplateParser templateParser, final TemplateMetadata templateMetadata, final boolean originalLumeerTemplate) {
      SelectionListCreator.createLists(templateParser, project, selectionListFacade);
      CollectionCreator.createCollections(templateParser, collectionFacade, defaultConfigurationProducer);
      LinkTypeCreator.createLinkTypes(templateParser, linkTypeFacade);
      DocumentCreator.createDocuments(templateParser, documentFacade, authenticatedUser, templateMetadata, permissionsChecker.getDocumentLimits());
      LinkInstanceCreator.createLinkInstances(templateParser, linkInstanceFacade, authenticatedUser, templateMetadata);
      ViewCreator.createViews(templateParser, viewFacade, defaultConfigurationProducer);
      FunctionAndRuleCreator.createFunctionAndRules(templateParser, collectionFacade, linkTypeFacade, originalLumeerTemplate);
      FavoriteItemsCreator.createFavoriteItems(templateParser, collectionFacade, viewFacade);
      SequenceCreator.createSequences(templateParser, sequenceFacade);
      ResourceCommentCreator.createComments(templateParser, resourceCommentFacade, defaultConfigurationProducer);

      if (templateCreatedEvent != null) {
         templateCreatedEvent.fire(templateParser.getReport(project));
      }
   }
}
