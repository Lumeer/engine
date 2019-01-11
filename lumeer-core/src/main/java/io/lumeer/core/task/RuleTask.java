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
package io.lumeer.core.task;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.core.task.executor.BlocklyRuleTaskExecutor;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RuleTask implements ContextualTask {

   private User initiator;
   private DaoContextSnapshot daoContextSnapshot;
   private PusherClient pusherClient;
   private Rule rule;
   private Collection collection;
   private Document oldDocument;
   private Document newDocument;

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;

      return this;
   }

   public void setRule(final Rule rule, final Collection collection, final Document oldDocument, final Document newDocument) {
      this.rule = rule;
      this.collection = collection;
      this.oldDocument = oldDocument;
      this.newDocument = newDocument;
   }

   @Override
   public void process() {
      if (rule.getType() == Rule.RuleType.BLOCKLY) {
         BlocklyRuleTaskExecutor.execute(this);
      }
   }

   public User getInitiator() {
      return initiator;
   }

   public DaoContextSnapshot getDaoContextSnapshot() {
      return daoContextSnapshot;
   }

   public PusherClient getPusherClient() {
      return pusherClient;
   }

   public Rule getRule() {
      return rule;
   }

   public Collection getCollection() {
      return collection;
   }

   public Document getOldDocument() {
      return oldDocument;
   }

   public Document getNewDocument() {
      return newDocument;
   }
}
