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
import io.lumeer.core.task.executor.ZapierRuleTaskExecutor;

public class RuleTask extends AbstractContextualTask {

   private String ruleName;
   private Rule rule;
   private Collection collection;
   private LinkType linkType;
   private Document oldDocument;
   private Document newDocument;
   private LinkInstance oldLinkInstance;
   private LinkInstance newLinkInstance;
   private AbstractContextualTask parent;

   public void setRule(final String ruleName, final Rule rule, final Collection collection, final Document oldDocument, final Document newDocument) {
      this.ruleName = ruleName;
      this.rule = rule;
      this.collection = collection;
      this.linkType = null;
      this.oldDocument = oldDocument;
      this.newDocument = newDocument;
      this.oldLinkInstance = null;
      this.newLinkInstance = null;
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
   }

   public boolean isCollectionBased() {
      return collection != null;
   }

   @Override
   public void process() {
      if (rule.getType() == Rule.RuleType.BLOCKLY) {
         final BlocklyRuleTaskExecutor executor = new BlocklyRuleTaskExecutor(ruleName, this);
         executor.execute();
      } else if (rule.getType() == Rule.RuleType.AUTO_LINK) {
         final AutoLinkRuleTaskExecutor executor = new AutoLinkRuleTaskExecutor(ruleName, this);
         executor.execute();
      } else if (rule.getType() == Rule.RuleType.ZAPIER) {
         final ZapierRuleTaskExecutor executor = new ZapierRuleTaskExecutor(ruleName, this);
         executor.execute();
      }

      if (parent != null) {
         parent.process();
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
}
