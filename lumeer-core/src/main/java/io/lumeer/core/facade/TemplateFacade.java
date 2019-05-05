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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.template.CollectionCreator;
import io.lumeer.core.template.DocumentCreator;
import io.lumeer.core.template.FunctionAndRuleCreator;
import io.lumeer.core.template.LinkInstanceCreator;
import io.lumeer.core.template.LinkTypeCreator;
import io.lumeer.core.template.TemplateParser;
import io.lumeer.core.template.TemplateType;
import io.lumeer.core.template.ViewCreator;

import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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

   public void installTemplate(final Organization organization, final Project project, final TemplateType templateType, final String language) {
      final TemplateParser templateParser = new TemplateParser(templateType, language);

      CollectionCreator.createCollections(templateParser, collectionFacade);
      LinkTypeCreator.createLinkTypes(templateParser, linkTypeFacade);
      DocumentCreator.createDocuments(templateParser, documentFacade, authenticatedUser);
      LinkInstanceCreator.createLinkInstances(templateParser, linkInstanceFacade, authenticatedUser);
      ViewCreator.createViews(templateParser, viewFacade, defaultConfigurationProducer);
      FunctionAndRuleCreator.createFunctionAndRules(templateParser, collectionFacade);
   }
}
