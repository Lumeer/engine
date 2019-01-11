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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Rule;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.CollectionDao;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class RuleProcessingFacade {

   @Inject
   private TaskExecutor taskExecutor;

   @Inject
   private ContextualTaskFactory contextualTaskFactory;

   @Inject
   private CollectionDao collectionDao;

   public void onDocumentUpdate(@Observes final UpdateDocument updateDocument) {
      if (updateDocument.getOriginalDocument() != null && updateDocument.getDocument() != null) {
         final Collection collection = collectionDao.getCollectionById(updateDocument.getDocument().getCollectionId());

         if (collection.getRules() != null && collection.getRules().size() > 0) {
            collection.getRules().values().stream()
                      .filter(rule -> rule.getTiming() == Rule.RuleTiming.UPDATE || rule.getTiming() == Rule.RuleTiming.CREATE_UPDATE || rule.getTiming() == Rule.RuleTiming.UPDATE_DELETE || rule.getTiming() == Rule.RuleTiming.ALL)
                      .forEach(rule -> {
                         final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);

                         ruleTask.setRule(rule, collection, updateDocument.getOriginalDocument(), updateDocument.getDocument());
                         taskExecutor.submitTask(ruleTask);
                      });
         }
      }
   }

   public void onCreateDocument(@Observes final CreateDocument createDocument) {
      if (createDocument.getDocument() != null) {
         final Collection collection = collectionDao.getCollectionById(createDocument.getDocument().getCollectionId());

         if (collection.getRules() != null && collection.getRules().size() > 0) {
            collection.getRules().values().stream()
                      .filter(rule -> rule.getTiming() == Rule.RuleTiming.CREATE_UPDATE || rule.getTiming() == Rule.RuleTiming.CREATE || rule.getTiming() == Rule.RuleTiming.CREATE_DELETE || rule.getTiming() == Rule.RuleTiming.ALL)
                      .forEach(rule -> {
                         final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);

                         ruleTask.setRule(rule, collection, null, createDocument.getDocument());
                         taskExecutor.submitTask(ruleTask);
                      });
         }
      }
   }

   public void onRemoveDocument(@Observes final RemoveDocument removeDocument) {
      if (removeDocument.getDocument() != null) {
         final Collection collection = collectionDao.getCollectionById(removeDocument.getDocument().getCollectionId());

         if (collection.getRules() != null && collection.getRules().size() > 0) {
            collection.getRules().values().stream()
                      .filter(rule -> rule.getTiming() == Rule.RuleTiming.DELETE || rule.getTiming() == Rule.RuleTiming.CREATE_DELETE || rule.getTiming() == Rule.RuleTiming.UPDATE_DELETE || rule.getTiming() == Rule.RuleTiming.ALL)
                      .forEach(rule -> {
                         final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);

                         ruleTask.setRule(rule, collection, removeDocument.getDocument(), null);
                         taskExecutor.submitTask(ruleTask);
                      });
         }
      }
   }
}
