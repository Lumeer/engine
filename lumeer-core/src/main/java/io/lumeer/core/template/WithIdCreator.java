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

import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import org.json.simple.JSONObject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WithIdCreator {

   protected final TemplateParser templateParser;

   protected WithIdCreator(final TemplateParser templateParser) {
      this.templateParser = templateParser;
   }

   protected DataDocument translateDataDocument(final WithId resource, final JSONObject o) {
      final DataDocument data = new DataDocument();

      o.forEach((k, v) -> {
         if (!"_id".equals(k)) {
            data.append((String) k, v);

            // add date function here
         }
      });

      return data;
   }
}
