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
package io.lumeer.core.task.executor;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.bridge.DocumentBridge;
import io.lumeer.core.task.executor.bridge.LinkBridge;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlocklyRuleTaskExecutor {

   private static Logger log = Logger.getLogger(BlocklyRuleTaskExecutor.class.getName());

   private String ruleName;
   private BlocklyRule rule;
   protected RuleTask ruleTask;
   private ChangesTracker tracker;

   public BlocklyRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      this.ruleName = ruleName;
      this.rule = new BlocklyRule(ruleTask.getRule());
      this.ruleTask = ruleTask;
   }

   protected Map<String, Object> getBindings() {
      final Map<String, Object> bindings = new HashMap<>();

      if (ruleTask.isCollectionBased()) {
         bindings.put("oldRecord", new DocumentBridge(ruleTask.getOldDocument()));
         bindings.put("oldDocument", new DocumentBridge(ruleTask.getOldDocument()));
         bindings.put("newRecord", new DocumentBridge(ruleTask.getNewDocument()));
         bindings.put("newDocument", new DocumentBridge(ruleTask.getNewDocument()));
      } else {
         bindings.put("oldLink", new LinkBridge(ruleTask.getOldLinkInstance()));
         bindings.put("newLink", new LinkBridge(ruleTask.getNewLinkInstance()));
      }

      return bindings;
   }

   public ChangesTracker execute(final TaskExecutor taskExecutor) {
      tracker = new ChangesTracker();
      final Map<String, Object> bindings = getBindings();
      final JsExecutor jsExecutor = new JsExecutor();
      jsExecutor.setDryRun(rule.isDryRun());

      try {
         jsExecutor.execute(bindings, ruleTask, ruleTask.getCollection(), rule.getJs());

         if (!rule.isDryRun()) {
            tracker = jsExecutor.commitOperations(taskExecutor);
         } else {
            writeDryRunResults(jsExecutor.getOperationsDescription());
            tracker = jsExecutor.commitDryRunOperations(taskExecutor);
         }

         checkErrorErasure();
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to execute Blockly Rule on document change: ", e);
         writeTaskError(e);
      }

      return tracker;
   }

   private void checkErrorErasure() {
      if (rule.getError() != null && rule.getError().length() > 0 && System.currentTimeMillis() - rule.getResultTimestamp() > 3600_000) {
         rule.setError("");
         updateRule();
      }
   }

   private void writeTaskError(final Exception e) {
      try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
         e.printStackTrace(pw);

         rule.setError(sw.toString());
         updateRule();

         if (ruleTask.isCollectionBased()) {
            tracker.updateCollectionsMap(Map.of(ruleTask.getCollection().getId(), ruleTask.getCollection()));
            tracker.addCollections(Set.of(ruleTask.getCollection()));
         } else {
            tracker.updateLinkTypesMap(Map.of(ruleTask.getLinkType().getId(), ruleTask.getLinkType()));
            tracker.addLinkTypes(Set.of(ruleTask.getLinkType()));
         }
      } catch (IOException ioe) {
         // we tried, cannot do more
      }
   }

   private void writeDryRunResults(final String results) {
      rule.setDryRunResult(results);
      updateRule();

      if (ruleTask.isCollectionBased()) {
         tracker.updateCollectionsMap(Map.of(ruleTask.getCollection().getId(), ruleTask.getCollection()));
         tracker.addCollections(Set.of(ruleTask.getCollection()));
      } else {
         tracker.updateLinkTypesMap(Map.of(ruleTask.getLinkType().getId(), ruleTask.getLinkType()));
         tracker.addLinkTypes(Set.of(ruleTask.getLinkType()));
      }
   }

   private void updateRule() {
      rule.setResultTimestamp(System.currentTimeMillis());

      if (ruleTask.isCollectionBased()) {
         final Collection originalCollection = ruleTask.getCollection().copy();
         ruleTask.getCollection().getRules().put(rule.getRule().getId(), rule.getRule());
         ruleTask.getDaoContextSnapshot().getCollectionDao().updateCollection(ruleTask.getCollection().getId(), ruleTask.getCollection(), originalCollection);
      } else {
         final LinkType originalLinkType = new LinkType(ruleTask.getLinkType());
         ruleTask.getLinkType().getRules().put(rule.getRule().getId(), rule.getRule());
         ruleTask.getDaoContextSnapshot().getLinkTypeDao().updateLinkType(ruleTask.getCollection().getId(), ruleTask.getLinkType(), originalLinkType);
      }
   }
}
