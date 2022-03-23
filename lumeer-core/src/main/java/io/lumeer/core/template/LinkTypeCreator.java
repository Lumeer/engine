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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permissions;
import io.lumeer.core.facade.LinkTypeFacade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LinkTypeCreator extends WithIdCreator {

   private final LinkTypeFacade linkTypeFacade;
   private final ObjectMapper mapper;

   private LinkTypeCreator(final TemplateParser templateParser, final LinkTypeFacade linkTypeFacade) {
      super(templateParser);
      this.linkTypeFacade = linkTypeFacade;
      this.mapper = createObjectMapper();
   }

   public static void createLinkTypes(final TemplateParser templateParser, final LinkTypeFacade linkTypeFacade) {
      final LinkTypeCreator creator = new LinkTypeCreator(templateParser, linkTypeFacade);
      creator.createLinkTypes();
   }

   public void createLinkTypes() {
      final JSONArray collections = (JSONArray) templateParser.template.get("linkTypes");
      collections.forEach(o -> {
         final String templateId = TemplateParserUtils.getId((JSONObject) o);
         final LinkType linkType = getLinkType((JSONObject) o);
         final LinkType storedLinkType = linkTypeFacade.createLinkType(linkType);
         templateParser.getDict().addLinkType(templateId, storedLinkType);

         createAttributes(storedLinkType, (JSONObject) o);
      });
   }

   private void createAttributes(final LinkType linkType, final JSONObject o) {
      final List<Attribute> attributes = TemplateParserUtils.getAttributes((JSONArray) ((JSONObject) o).get("attributes"), mapper)
                                                            .stream().map(attribute -> TemplateParserUtils.mapAttributeConstraintConfig(templateParser, attribute))
                                                            .collect(Collectors.toList());
      linkTypeFacade.createLinkTypeAttributesWithoutPushNotification(linkType.getId(), attributes);
   }

   private LinkType getLinkType(final JSONObject o) {
      var collections = (List<String>) ((JSONArray) o.get(LinkType.COLLECTION_IDS)).stream().map(collectionId -> templateParser.getDict().getCollectionId((String) collectionId)).collect(Collectors.toList());

      return new LinkType(
            (String) o.get(LinkType.NAME),
            collections,
            new ArrayList<>(),
            null,
            new Permissions(),
            null
      );
   }
}
