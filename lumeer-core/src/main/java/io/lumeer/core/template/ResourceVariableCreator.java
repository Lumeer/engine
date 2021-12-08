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

import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.exception.TemplateNotAvailableException;
import io.lumeer.core.facade.ResourceVariableFacade;
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
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceVariableCreator extends WithIdCreator {

   private final ResourceVariableFacade resourceVariableFacade;
   private final String projectId;
   private final String organizationId;
   private final ObjectMapper mapper;
   private final ConstraintManager constraintManager;

   private ResourceVariableCreator(final TemplateParser templateParser, final ResourceVariableFacade resourceVariableFacade, final DefaultConfigurationProducer defaultConfigurationProducer, final String organizationId, final String projectId) {
      super(templateParser);
      this.resourceVariableFacade = resourceVariableFacade;
      this.constraintManager = ConstraintManager.getInstance(defaultConfigurationProducer);
      this.organizationId = organizationId;
      this.projectId = projectId;

      mapper = new ObjectMapper();
      AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
      AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
      AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
      mapper.setAnnotationIntrospector(pair);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   }

   public static void createVariables(final TemplateParser templateParser, final ResourceVariableFacade resourceVariableFacade, final DefaultConfigurationProducer defaultConfigurationProducer, final String organizationId, final String projectId) {
      final ResourceVariableCreator resourceCommentCreator = new ResourceVariableCreator(templateParser, resourceVariableFacade, defaultConfigurationProducer, organizationId, projectId);
      resourceCommentCreator.createComments();
   }

   private void createComments() {
      final JSONArray variables = (JSONArray) templateParser.template.get("variables");

      List<ResourceVariable> currentVariables = resourceVariableFacade.getInProject(projectId);
      Set<String> currentKeys = currentVariables.stream().map(ResourceVariable::getKey).collect(Collectors.toSet());;

      if (variables != null) {
         final List<ResourceVariable> result = new ArrayList<>();

         variables.forEach(variableObj -> {
            try {
               var variableJson = (JSONObject) variableObj;
               var resourceVariable = mapper.readValue(variableJson.toJSONString(), ResourceVariable.class);
               resourceVariable.setId(null);

               if(currentKeys.contains(resourceVariable.getKey()) || resourceVariable.getResourceType() == ResourceType.ORGANIZATION) {
                  return;
               }

               if (resourceVariable.getResourceType() == ResourceType.PROJECT) {
                  resourceVariable.setOrganizationId(organizationId);
                  resourceVariable.setProjectId(null);
                  resourceVariable.setResourceId(projectId);
               } else {
                  resourceVariable.setOrganizationId(organizationId);
                  resourceVariable.setProjectId(projectId);

                  var translatedResourceId = templateParser.translateString(resourceVariable.getResourceId(), constraintManager);
                  resourceVariable.setResourceId(translatedResourceId != null ? translatedResourceId.toString() : null);
               }

               currentKeys.add(resourceVariable.getKey());
               result.add(resourceVariable);
            } catch (IOException e) {
               throw new TemplateNotAvailableException(e);
            }
         });

         resourceVariableFacade.storeResourceVariables(result, organizationId, projectId);
      }
   }
}
