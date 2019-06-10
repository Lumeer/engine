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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WithIdCreator {

   private DateTimeFormatter dateDecoder = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX", Locale.forLanguageTag("en_US"));

   protected final TemplateParser templateParser;

   protected WithIdCreator(final TemplateParser templateParser) {
      this.templateParser = templateParser;
   }

   protected DataDocument translateDataDocument(final WithId resource, final JSONObject o, final String defaultUser) {
      final DataDocument data = new DataDocument();

      o.forEach((k, v) -> {
         if (!"_id".equals(k)) {
            if ("$USER".equals(v)) {
               data.append((String) k, defaultUser);
            } else {
               var passed = false;
               if (v != null) {
                  try {
                     var accessor = dateDecoder.parse(v.toString());
                     data.append(
                           (String) k,
                           Date.from(ZonedDateTime.from(accessor).toInstant())
                     );
                     passed = true;
                  } catch (DateTimeParseException dtpe) {
                  }
               }

               if (!passed) {
                  data.append((String) k, v);
               }
            }

            // add date function here
         }
      });

      return data;
   }
}
