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
public class BlocklyRule extends Rule {

   public static final String BLOCKLY_XML = "blocklyXml";
   public static final String BLOCKLY_JS = "blocklyJs";
   public static final String BLOCKLY_ERROR = "blocklyError";
   public static final String BLOCKLY_ERROR_DATE = "blocklyErrorDate";
   public static final String BLOCKLY_DRY_RUN = "blocklyDryRun";
   public static final String BLOCKLY_DRY_RUN_RESULT = "blocklyDryRunResult";

   public BlocklyRule(final Rule rule) {
      super(RuleType.BLOCKLY, rule.getTiming(), rule.getConfiguration());

      if (rule.getType() != RuleType.BLOCKLY) {
         throw new IllegalArgumentException("Cannot create Blockly Rule from a rule of type " + rule.getType());
      }
   }

   public String getXml() {
      return configuration.getString(BLOCKLY_XML);
   }

   public void setXml(final String xml) {
      configuration.put(BLOCKLY_XML, xml);
   }

   public String getJs() {
      return configuration.getString(BLOCKLY_JS);
   }

   public void setJs(final String js) {
      configuration.put(BLOCKLY_DRY_RUN_RESULT, js);
   }

   public String getError() {
      return configuration.getString(BLOCKLY_ERROR);
   }

   public void setError(final String error) {
      configuration.put(BLOCKLY_DRY_RUN_RESULT, error);
   }

   public long getErrorDate() {
      return configuration.getLong(BLOCKLY_ERROR_DATE);
   }

   public void setErrorDate(final long errorDate) {
      configuration.put(BLOCKLY_DRY_RUN_RESULT, errorDate);
   }

   public boolean isDryRun() {
      return configuration.getBoolean(BLOCKLY_DRY_RUN);
   }

   public void setDryRun(final boolean dryRun) {
      configuration.put(BLOCKLY_DRY_RUN_RESULT, dryRun);
   }

   public String getDryRunResult() {
      return configuration.getString(BLOCKLY_DRY_RUN_RESULT);
   }

   public void setDryRunResult(final String dryRunResult) {
      configuration.put(BLOCKLY_DRY_RUN_RESULT, dryRunResult);
   }
}
