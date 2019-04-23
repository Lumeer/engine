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
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.engine.api.data.DataDocument;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionAndRuleCreator extends WithIdCreator {

   private final CollectionFacade collectionFacade;

   private FunctionAndRuleCreator(final TemplateParser templateParser, final CollectionFacade collectionFacade) {
      super(templateParser);
      this.collectionFacade = collectionFacade;
   }

   public static void createFunctionAndRules(final TemplateParser templateParser, final CollectionFacade collectionFacade) {
      final FunctionAndRuleCreator creator = new FunctionAndRuleCreator(templateParser, collectionFacade);
      creator.createFunctionAndRules();
   }

   private void createFunctionAndRules() {
      final JSONArray collections = (JSONArray) templateParser.template.get("collections");
      collections.forEach(o -> {
         final String templateId = TemplateParserUtils.getId((JSONObject) o);
         final String collectionId = templateParser.getDict().getCollectionId(templateId);
         final Collection collection = collectionFacade.getCollection(collectionId);

         var attrs = (JSONArray) ((JSONObject) o).get("attributes");
         attrs.forEach(attrObj -> {
            var attrJson = (JSONObject) attrObj;

            if (attrJson.get("function") != null) {
               var attr = getCollectionAttribute(collection, (String) attrJson.get("id"));
               var fce = getFunction(collection, (JSONObject) attrJson.get("function"));
               attr.setFunction(fce);
            }
         });

         var rules = (JSONObject) ((JSONObject) o).get("rules");
         var collectionRules = new HashMap<String, Rule>();
         rules.forEach((k, v) -> {
            collectionRules.put((String) k, getRule(collection, (JSONObject) v));
         });
      });

   }

   public Attribute getCollectionAttribute(final Collection collection, final String attributeTemplateId) {
      var attr = collection.getAttributes().stream().filter(a -> a.getId().equals(attributeTemplateId)).findFirst();
      return attr.orElse(null);
   }

   public Function getFunction(final Collection collection, final JSONObject o) {
      return  new Function(cureJs((String) o.get(Function.JS)), cureXml((String) o.get(Function.XML)), null, 0, (Boolean) o.get(Function.EDITABLE));
   }

   private Rule getRule(final Collection collection, final JSONObject o) {
      var type = Rule.RuleType.values()[(Integer) o.get(Rule.TYPE)];
      var timing = Rule.RuleTiming.values()[(Integer) o.get(Rule.TIMING)];
      var rule = new Rule(type, timing, new DataDocument((JSONObject) o.get("configuration")));

      if (type == Rule.RuleType.BLOCKLY) {
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_JS, cureJs(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_JS)));
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_XML, cureXml(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_XML)));
      }

      if (type == Rule.RuleType.AUTO_LINK) {
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_LINK_TYPE, templateParser.getDict().getLinkTypeId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_LINK_TYPE)));
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_COLLECTION1, templateParser.getDict().getCollectionId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_COLLECTION1)));
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_COLLECTION2, templateParser.getDict().getCollectionId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_COLLECTION2)));
      }

      return rule;
   }

   private String cureJs(final String js) {
      var res = TemplateParserUtils.replacer(js, ", '", "')", id -> {
         if (id.length() == 24) {
            var collId = templateParser.getDict().getCollectionId(id);
            if (collId != null) {
               return collId;
            }

            var linkId = templateParser.getDict().getLinkTypeId(id);
            if (linkId != null) {
               return linkId;
            }

            return id;
         } else {
            return id;
         }
      });

      return js;
   }

   private String cureXml(final String xml) {
      /*
        block type=
        variables_get_$collectionId_document
        $linktTypeId-$collectionId_$collectionId_link
        $linktTypeId-$collectionId_$collectionId_link_instance


        variable type=
        variabletype=
        $collectionId_document
        $collectionId_document_array
        $linkTypeId_linkinst
        $linkTypeId_link_array
       */

      var res = TemplateParserUtils.replacer(xml, "<block type=\"variables_get_", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<block type=\"", "-[0-9a-f]+_[0-9a-f]+_link", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "<block type=\"[0-9a-f]+-", "_[0-9a-f]+_link", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<block type=\"[0-9a-f]+-[0-9a-f]+_", "_link", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<variable type=\"", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<variable type=\"", "_link", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "variabletype=\"", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "variabletype=\"", "_link", templateParser.getDict()::getLinkTypeId);

      return res;
   }
}
