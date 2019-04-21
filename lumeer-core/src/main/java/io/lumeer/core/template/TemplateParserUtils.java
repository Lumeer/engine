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
package io.lumeer.core.template;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.common.WithId;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TemplateParserUtils {

   static List<Attribute> getAttributes(final JSONArray a) {
      final var attrs = new ArrayList<Attribute>();

      a.forEach(o -> {
         attrs.add(getAttribute((JSONObject) o));
      });

      return attrs;
   }

   private static Attribute getAttribute(final JSONObject o) {
      final var attr = new Attribute((String) o.get(Attribute.NAME));
      attr.setId(getId((JSONObject) o));

      if (o.get("constraint") != null) {
         attr.setConstraint(getAttributeConstraint((JSONObject) o.get("constraint")));
      }

      return attr;
   }

   private static Constraint getAttributeConstraint(final JSONObject o) {
      return new Constraint(ConstraintType.valueOf((String) o.get("type")), o.get("config"));
   }

   static String getId(final JSONObject o) {
      return (String) o.get("_id");
   }
}
