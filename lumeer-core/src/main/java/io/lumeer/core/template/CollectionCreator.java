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
import io.lumeer.api.model.Collection;
import io.lumeer.core.facade.CollectionFacade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CollectionCreator {

   private final TemplateParser templateParser;
   private final CollectionFacade collectionFacade;

   private CollectionCreator(final TemplateParser templateParser, final CollectionFacade collectionFacade) {
      this.templateParser = templateParser;
      this.collectionFacade = collectionFacade;
   }

   public void createCollections(final TemplateParser templateParser, final CollectionFacade collectionFacade) {
      final CollectionCreator creator = new CollectionCreator(templateParser, collectionFacade);
      creator.createCollections();
   }

   private void createCollections() {
      final JSONArray collections = (JSONArray) templateParser.template.get("collections");
      collections.forEach(o -> {
         final String templateId = TemplateParserUtils.getId((JSONObject) o);
         final Collection collection = getCollection((JSONObject) o);
         final Collection storedCollection = collectionFacade.createCollection(collection);
         templateParser.getDict().addCollection(templateId, storedCollection);

         createAttributes(storedCollection, (JSONObject) o);
         setDefaultAttributeId(storedCollection, (JSONObject) o);
      });
   }

   private void setDefaultAttributeId(final Collection storedCollection, final JSONObject o) {
      final String defaultAttributeId = (String) o.get("defaultAttributeId");
      if (defaultAttributeId != null) {
         storedCollection.setDefaultAttributeId(templateParser.getDict().getAttributeId(storedCollection, defaultAttributeId));
      }

      collectionFacade.setDefaultAttribute(storedCollection.getId(), storedCollection.getDefaultAttributeId());
   }

   private void createAttributes(final Collection collection, final JSONObject o) {
      final java.util.Collection<Attribute> storedAttributes = collectionFacade.createCollectionAttributes(collection.getId(), TemplateParserUtils.getAttributes((JSONArray) ((JSONObject) o).get("attributes")));
      final List<Attribute> templateAttributes = TemplateParserUtils.getAttributes((JSONArray) ((JSONObject) o).get("attributes"));
      TemplateParserUtils.registerAttributes(templateParser, collection, storedAttributes, templateAttributes);
   }

   private Collection getCollection(final JSONObject o) {
      return new Collection(
            (String) o.get(Collection.CODE),
            (String) o.get(Collection.NAME),
            (String) o.get(Collection.ICON),
            (String) o.get(Collection.COLOR),
            null
      );
   }

}
