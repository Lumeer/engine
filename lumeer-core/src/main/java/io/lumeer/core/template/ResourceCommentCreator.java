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

import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.templateParse.ResourceCommentWrapper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.exception.TemplateNotAvailableException;
import io.lumeer.core.facade.ResourceCommentFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResourceCommentCreator extends WithIdCreator {

   private final ResourceCommentFacade resourceCommentFacade;
   private final ObjectMapper mapper;
   private final ConstraintManager constraintManager;

   private ResourceCommentCreator(final TemplateParser templateParser, final ResourceCommentFacade resourceCommentFacade, final DefaultConfigurationProducer defaultConfigurationProducer) {
      super(templateParser);
      this.resourceCommentFacade = resourceCommentFacade;
      this.constraintManager = ConstraintManager.getInstance(defaultConfigurationProducer);

      mapper = new ObjectMapper();
      AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
      AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
      AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
      mapper.setAnnotationIntrospector(pair);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   }

   public static void createComments(final TemplateParser templateParser, final ResourceCommentFacade resourceCommentFacade, final DefaultConfigurationProducer defaultConfigurationProducer) {
      final ResourceCommentCreator resourceCommentCreator = new ResourceCommentCreator(templateParser, resourceCommentFacade, defaultConfigurationProducer);
      resourceCommentCreator.createComments();
   }

   private void createComments() {
      final JSONArray comments = (JSONArray) templateParser.template.get("comments");

      if (comments != null) {
         final List<ResourceComment> result = new ArrayList<>();

         comments.forEach(commentObj -> {
            try {
               var commentJson = (JSONObject) commentObj;
               var resourceComment = mapper.readValue(commentJson.toJSONString(), ResourceCommentWrapper.class);

               var translatedParentId = templateParser.translateString(resourceComment.getParentId(), constraintManager);
               var translatedResourceId = templateParser.translateString(resourceComment.getResourceId(), constraintManager);

               if (translatedParentId != null && translatedResourceId != null) {
                  resourceComment.setParentId(translatedParentId.toString());
                  resourceComment.setResourceId(translatedResourceId.toString());

                  resourceComment.setId(null);

                  result.add(resourceComment.getResourceComment());
               }

            } catch (IOException e) {
               throw new TemplateNotAvailableException(e);
            }
         });

         resourceCommentFacade.storeResourceComments(result);
      }
   }
}
