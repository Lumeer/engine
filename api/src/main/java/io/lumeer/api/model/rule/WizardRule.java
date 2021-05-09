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
package io.lumeer.api.model.rule;

import io.lumeer.api.model.Rule;
import io.lumeer.engine.api.data.DataDocument;

public class WizardRule extends BlocklyRule {

   public static final String WIZARD_CONFIGURATION = "wizardConfiguration";

   WizardRule(final Rule rule) {
      super(rule, Rule.RuleType.WIZARD);
   }

   public DataDocument getWizardConfiguration() {
      return rule.getConfiguration().getDataDocument(WIZARD_CONFIGURATION);
   }

   public void setWizardConfiguration(final DataDocument wizardConfiguration) {
      rule.getConfiguration().put(WIZARD_CONFIGURATION, wizardConfiguration);
   }
}
