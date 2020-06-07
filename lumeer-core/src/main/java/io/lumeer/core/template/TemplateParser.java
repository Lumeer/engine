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
package io.lumeer.core.template;

import io.lumeer.api.model.Language;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.core.exception.TemplateNotAvailableException;
import io.lumeer.engine.api.event.TemplateCreated;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Parses a project template from a JSON file.
 */
public class TemplateParser {

   final TemplateObjectsDictionary dict = new TemplateObjectsDictionary();
   final JSONObject template;

   public TemplateParser(final String templateType, final Language language) {
      final JSONParser parser = new JSONParser();
      final Object o;

      try (final BufferedReader br = new BufferedReader(new InputStreamReader(
            this.getClass().getResourceAsStream("/templates/" + templateType.toLowerCase() + "." + language.toString().toLowerCase() + ".json"), StandardCharsets.UTF_8))) {
         o = parser.parse(br);
         if (!(o instanceof JSONObject)) {
            throw new IOException("Template file does not contain a valid JSON object.");
         }
         template = (JSONObject) o;
      } catch (IOException | ParseException e) {
         throw new TemplateNotAvailableException(e);
      }
   }

   public TemplateParser(final ProjectContent projectContent) {
      final JSONParser parser = new JSONParser();
      final ObjectMapper mapper = new ObjectMapper();
      final Object o;
      try {
         final String json = mapper.writeValueAsString(projectContent);
         o = parser.parse(json);
         if (!(o instanceof JSONObject)) {
            throw new IOException("Template file does not contain a valid JSON object.");
         }
         template = (JSONObject) o;
      } catch (ParseException | IOException e) {
         throw new TemplateNotAvailableException(e);
      }
   }

   public TemplateObjectsDictionary getDict() {
      return dict;
   }

   public JSONObject getTemplate() {
      return template;
   }

   public TemplateCreated getReport(final Project project) {
      return new TemplateCreated(project, dict.getCollectionIds(), dict.getLinkTypeIds(), dict.getViewIds());
   }
}
