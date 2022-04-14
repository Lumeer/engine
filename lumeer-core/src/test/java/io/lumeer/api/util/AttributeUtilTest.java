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
package io.lumeer.api.util;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class AttributeUtilTest {

   @Test
   public void testInUTC() {
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY HH:mm", null)));
      Assert.assertTrue(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY HH:mm", true)));
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY HH:mm", false)));
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY", null)));
      Assert.assertTrue(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY HH:mm", true)));
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute("DD.MM.YYYY HH:mm", false)));
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute(null, null)));
      Assert.assertTrue(AttributeUtil.isUTC(getAttribute(null, true)));
      Assert.assertFalse(AttributeUtil.isUTC(getAttribute(null, false)));
   }

   private Attribute getAttribute(final String format, final Boolean asUtc) {
      var config = new HashMap<String, Object>();

      if (format != null) {
         config.put("format", format);
      }

      if (asUtc != null) {
         config.put("asUtc", asUtc);
      }

      return new Attribute("a0", "Due Date", "", new Constraint(ConstraintType.DateTime, config), null, null, 0, null);
   }
}
