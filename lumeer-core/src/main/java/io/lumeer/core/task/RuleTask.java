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
package io.lumeer.core.task;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;
import io.lumeer.core.task.executor.AutoLinkRuleTaskExecutor;
import io.lumeer.core.task.executor.BlocklyRuleTaskExecutor;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.CronRuleTaskExecutor;
import io.lumeer.core.task.executor.ZapierRuleTaskExecutor;

import java.util.List;
import java.util.logging.Logger;

public class RuleTask extends AbstractContextualTask {

   private Logger log = Logger.getLogger(RuleTask.class.getName());

   private String ruleName;
   private Rule rule;
   private Collection collection;
   private LinkType linkType;
   private Document oldDocument;
   private Document newDocument;
   private LinkInstance oldLinkInstance;
   private LinkInstance newLinkInstance;
   private List<Document> documents;
   private String actionName;

   public void setRule(final String ruleName, final Rule rule, final Collection collection, final Document oldDocument, final Document newDocument) {
      this.ruleName = ruleName;
      this.rule = rule;
      this.collection = collection;
      this.linkType = null;
      this.oldDocument = oldDocument;
      this.newDocument = newDocument;
      this.oldLinkInstance = null;
      this.newLinkInstance = null;
      this.documents = null;
   }

   public void setRule(final String ruleName, final Rule rule, final LinkType linkType, final LinkInstance oldLinkInstance, final LinkInstance newLinkInstance) {
      this.ruleName = ruleName;
      this.rule = rule;
      this.collection = null;
      this.linkType = linkType;
      this.oldDocument = null;
      this.newDocument = null;
      this.oldLinkInstance = oldLinkInstance;
      this.newLinkInstance = newLinkInstance;
      this.documents = null;
   }

   public void setRule(final String ruleName, final Rule rule, final Collection collection, final List<Document> documents) {
      this.ruleName = ruleName;
      this.rule = rule;
      this.collection = collection;
      this.linkType = null;
      this.oldDocument = null;
      this.newDocument = null;
      this.oldLinkInstance = null;
      this.newLinkInstance = null;
      this.documents = documents;
   }

   public boolean isCollectionBased() {
      return collection != null;
   }

   @Override
   public void process(final TaskExecutor taskExecutor, final ChangesTracker changesTracker) {
      if (daoContextSnapshot.getSelectedWorkspace() != null && daoContextSnapshot.getSelectedWorkspace().getOrganization().isPresent() && daoContextSnapshot.getSelectedWorkspace().getProject().isPresent()) {
         log.info(
               String.format(
                     "Running rule task on %s/%s > Rule '%s', Resource '%s'.",
                     daoContextSnapshot.getSelectedWorkspace().getOrganization().get().getCode(),
                     daoContextSnapshot.getSelectedWorkspace().getProject().get().getCode(),
                     rule.getName(),
                     collection != null ? collection.getName() : linkType.getName()
               )
         );
      }
      if (rule.getType() == Rule.RuleType.BLOCKLY) {
         final BlocklyRuleTaskExecutor executor = new BlocklyRuleTaskExecutor(ruleName, this);
         changesTracker.merge(executor.execute(taskExecutor));
      } else if (rule.getType() == Rule.RuleType.AUTO_LINK) {
         final AutoLinkRuleTaskExecutor executor = new AutoLinkRuleTaskExecutor(ruleName, this);
         changesTracker.merge(executor.execute(taskExecutor));
      } else if (rule.getType() == Rule.RuleType.ZAPIER) {
         final ZapierRuleTaskExecutor executor = new ZapierRuleTaskExecutor(ruleName, this);
         executor.execute();
      } else if (rule.getType() == Rule.RuleType.CRON) {
         final CronRuleTaskExecutor executor = new CronRuleTaskExecutor(ruleName, this);
         executor.execute(taskExecutor);
      }

      if (parent != null) {
         parent.process(taskExecutor, changesTracker);
      }
   }

   public Rule getRule() {
      return rule;
   }

   public Collection getCollection() {
      return collection;
   }

   public LinkType getLinkType() {
      return linkType;
   }

   public Document getOldDocument() {
      return oldDocument;
   }

   public Document getNewDocument() {
      return newDocument;
   }

   public LinkInstance getOldLinkInstance() {
      return oldLinkInstance;
   }

   public LinkInstance getNewLinkInstance() {
      return newLinkInstance;
   }

   public List<Document> getDocuments() {
      return documents;
   }

   public String getActionName() {
      return actionName;
   }

   public void setActionName(final String actionName) {
      this.actionName = actionName;
   }

   @Override
   public String toString() {
      return "RuleTask{" +
            "ruleName='" + ruleName + '\'' +
            ", rule=" + rule +
            ", collection=" + (collection != null ? collection.getId() : null) +
            ", linkType=" + (linkType != null ? linkType.getId() : null) +
            ", oldDocument=" + oldDocument +
            ", newDocument=" + newDocument +
            ", oldLinkInstance=" + oldLinkInstance +
            ", newLinkInstance=" + newLinkInstance +
            ", documents=" + documents +
            ", actionName=" + actionName +
            '}';
   }

   @Override
   public void propagateChanges(final List<Document> documents, final List<LinkInstance> links) {
      if (documents != null && newDocument != null) {
         int idx = documents.indexOf(newDocument);
         if (idx >= 0) {
            this.newDocument = documents.get(idx);
         }
      }

      if (links != null && newLinkInstance != null) {
         int idx = links.indexOf(newLinkInstance);
         if (idx >= 0) {
            this.newLinkInstance = links.get(idx);
         }
      }

      super.propagateChanges(documents, links);
   }
}