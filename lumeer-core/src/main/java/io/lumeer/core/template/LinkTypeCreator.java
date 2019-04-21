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
import io.lumeer.api.model.LinkType;
import io.lumeer.core.facade.LinkTypeFacade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class LinkTypeCreator extends WithIdCreator {

   private final LinkTypeFacade linkTypeFacade;

   private LinkTypeCreator(final TemplateParser templateParser, final LinkTypeFacade linkTypeFacade) {
      super(templateParser);
      this.linkTypeFacade = linkTypeFacade;
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
      final java.util.Collection<Attribute> storedAttributes = linkTypeFacade.createLinkTypeAttributes(linkType.getId(), TemplateParserUtils.getAttributes((JSONArray) ((JSONObject) o).get("attributes")));
      final List<Attribute> templateAttributes = TemplateParserUtils.getAttributes((JSONArray) ((JSONObject) o).get("attributes"));
      registerAttributes(linkType, storedAttributes, templateAttributes);
   }

   private LinkType getLinkType(final JSONObject o) {
      return new LinkType(
            (String) o.get(LinkType.NAME),
            (JSONArray) o.get(LinkType.COLLECTION_IDS),
            null
      );
   }

}
