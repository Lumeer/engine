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

import io.lumeer.api.model.View;
import io.lumeer.core.exception.TemplateNotAvailableException;
import io.lumeer.core.facade.ViewFacade;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ViewCreator extends WithIdCreator {

   private final ViewFacade viewFacade;
   private final ObjectMapper mapper;

   private ViewCreator(final TemplateParser templateParser, final ViewFacade viewFacade) {
      super(templateParser);
      this.viewFacade = viewFacade;

      mapper = new ObjectMapper();
      AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
      AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
      AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
      mapper.setAnnotationIntrospector(pair);
   }

   public static void createViews(final TemplateParser templateParser, final ViewFacade viewFacade) {
      final ViewCreator creator = new ViewCreator(templateParser, viewFacade);
      creator.createViews();
   }

   private void createViews() {
      ((JSONArray) templateParser.getTemplate().get("views")).forEach(viewObj -> {
         try {
            var viewJson = (JSONObject) viewObj;
            var templateId = TemplateParserUtils.getId(viewJson);
            viewJson.remove("_id");
            var view = mapper.readValue(viewJson.toJSONString(), View.class);
            viewJson.put("_id", templateId);

            view = viewFacade.createView(view);
            templateParser.getDict().addView(templateId, view);
         } catch (IOException e) {
            throw new TemplateNotAvailableException(e);
         }
      });
   }
}
