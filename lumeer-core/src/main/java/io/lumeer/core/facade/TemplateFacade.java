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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.template.CollectionCreator;
import io.lumeer.core.template.DocumentCreator;
import io.lumeer.core.template.FunctionAndRuleCreator;
import io.lumeer.core.template.LinkInstanceCreator;
import io.lumeer.core.template.LinkTypeCreator;
import io.lumeer.core.template.TemplateParser;
import io.lumeer.core.template.ViewCreator;
import io.lumeer.engine.api.event.TemplateCreated;

import java.util.Date;
import javax.enterprise.event.Event;
import javax.inject.Inject;

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
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Event<TemplateCreated> templateCreatedEvent;

   public void installTemplate(final Project project, final ProjectContent projectContent, final Date relativeDate) {
      final TemplateParser templateParser = new TemplateParser(projectContent);

      installTemplate(project, templateParser);
   }

   private void installTemplate(final Project project, final TemplateParser templateParser) {
      CollectionCreator.createCollections(templateParser, collectionFacade);
      LinkTypeCreator.createLinkTypes(templateParser, linkTypeFacade);
      DocumentCreator.createDocuments(templateParser, documentFacade, authenticatedUser);
      LinkInstanceCreator.createLinkInstances(templateParser, linkInstanceFacade, authenticatedUser);
      ViewCreator.createViews(templateParser, viewFacade, defaultConfigurationProducer);
      FunctionAndRuleCreator.createFunctionAndRules(templateParser, collectionFacade, linkTypeFacade);

      if (templateCreatedEvent != null) {
         templateCreatedEvent.fire(templateParser.getReport(project));
      }
   }
}
