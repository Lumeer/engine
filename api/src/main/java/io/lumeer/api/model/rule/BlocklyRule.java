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
package io.lumeer.api.model.rule;

import io.lumeer.api.model.Rule;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class BlocklyRule {

   public static final String BLOCKLY_XML = "blocklyXml";
   public static final String BLOCKLY_JS = "blocklyJs";
   public static final String BLOCKLY_ERROR = "blocklyError";
   public static final String BLOCKLY_RESULT_TIMESTAMP = "blocklyResultTimestamp";
   public static final String BLOCKLY_DRY_RUN = "blocklyDryRun";
   public static final String BLOCKLY_DRY_RUN_RESULT = "blocklyDryRunResult";

   private final Rule rule;

   public BlocklyRule(final Rule rule) {
      this.rule = rule;

      if (rule.getType() != Rule.RuleType.BLOCKLY) {
         throw new IllegalArgumentException("Cannot create Blockly Rule from a rule of type " + rule.getType());
      }
   }

   public Rule getRule() {
      return rule;
   }

   public String getXml() {
      return rule.getConfiguration().getString(BLOCKLY_XML);
   }

   public void setXml(final String xml) {
      rule.getConfiguration().put(BLOCKLY_XML, xml);
   }

   public String getJs() {
      return rule.getConfiguration().getString(BLOCKLY_JS);
   }

   public void setJs(final String js) {
      rule.getConfiguration().put(BLOCKLY_JS, js);
   }

   public String getError() {
      return rule.getConfiguration().getString(BLOCKLY_ERROR);
   }

   public void setError(final String error) {
      rule.getConfiguration().put(BLOCKLY_ERROR, error);
   }

   public long getResultTimestamp() {
      return rule.getConfiguration().getLong(BLOCKLY_RESULT_TIMESTAMP);
   }

   public void setResultTimestamp(final long timestamp) {
      rule.getConfiguration().put(BLOCKLY_RESULT_TIMESTAMP, timestamp);
   }

   public boolean isDryRun() {
      return rule.getConfiguration().getBoolean(BLOCKLY_DRY_RUN);
   }

   public void setDryRun(final boolean dryRun) {
      rule.getConfiguration().put(BLOCKLY_DRY_RUN, dryRun);
   }

   public String getDryRunResult() {
      return rule.getConfiguration().getString(BLOCKLY_DRY_RUN_RESULT);
   }

   public void setDryRunResult(final String dryRunResult) {
      rule.getConfiguration().put(BLOCKLY_DRY_RUN_RESULT, dryRunResult);
   }

   @Override
   public String toString() {
      return "BlocklyRule{" +
            "rule=" + rule +
            '}';
   }
}
