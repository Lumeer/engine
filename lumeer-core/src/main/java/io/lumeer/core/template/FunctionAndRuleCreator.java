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
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.api.model.rule.CronRule;
import io.lumeer.api.model.rule.WizardRule;
import io.lumeer.api.model.rule.ZapierRule;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.LinkTypeFacade;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.regex.Pattern;

public class FunctionAndRuleCreator extends WithIdCreator {

   private final CollectionFacade collectionFacade;
   private final LinkTypeFacade linkTypeFacade;
   private final boolean skipFceLimits;

   private FunctionAndRuleCreator(final TemplateParser templateParser, final CollectionFacade collectionFacade, final LinkTypeFacade linkTypeFacade, final boolean skipFceLimits) {
      super(templateParser);
      this.collectionFacade = collectionFacade;
      this.linkTypeFacade = linkTypeFacade;
      this.skipFceLimits = skipFceLimits;
   }

   public static void createFunctionAndRules(final TemplateParser templateParser, final CollectionFacade collectionFacade, final LinkTypeFacade linkTypeFacade, final boolean skipFceLimits) {
      final FunctionAndRuleCreator creator = new FunctionAndRuleCreator(templateParser, collectionFacade, linkTypeFacade, skipFceLimits);
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
               var fce = getFunction((JSONObject) attrJson.get("function"));
               attr.setFunction(fce);
               collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr, skipFceLimits);
            }
         });

         var rules = (JSONObject) ((JSONObject) o).get("rules");
         if (rules != null) {
            var collectionRules = new HashMap<String, Rule>();
            rules.forEach((k, v) -> {
               collectionRules.put((String) k, getRule((JSONObject) v));
            });
            collection.setRules(collectionRules);
            collectionFacade.updateCollection(collection.getId(), collection, skipFceLimits);
         }
      });

      final JSONArray linkTypes = (JSONArray) templateParser.template.get("linkTypes");
      linkTypes.forEach(o -> {
         final String templateId = TemplateParserUtils.getId((JSONObject) o);
         final String linkTypeId = templateParser.getDict().getLinkTypeId(templateId);
         final LinkType linkType = linkTypeFacade.getLinkType(linkTypeId);

         var attrs = (JSONArray) ((JSONObject) o).get("attributes");
         attrs.forEach(attrObj -> {
            var attrJson = (JSONObject) attrObj;

            if (attrJson.get("function") != null) {
               var attr = getLinkTypeAttribute(linkType, (String) attrJson.get("id"));
               var fce = getFunction((JSONObject) attrJson.get("function"));
               attr.setFunction(fce);
               linkTypeFacade.updateLinkTypeAttribute(linkType.getId(), attr.getId(), attr, skipFceLimits);
            }

            var rules = (JSONObject) ((JSONObject) o).get("rules");
            if (rules != null) {
               var linkTypeRules = new HashMap<String, Rule>();
               rules.forEach((k, v) -> {
                  linkTypeRules.put((String) k, getRule((JSONObject) v));
               });
               linkType.setRules(linkTypeRules);
               linkTypeFacade.updateLinkType(linkType.getId(), linkType, skipFceLimits);
            }
         });
      });
   }

   public Attribute getCollectionAttribute(final Collection collection, final String attributeTemplateId) {
      var attr = collection.getAttributes().stream().filter(a -> a.getId().equals(attributeTemplateId)).findFirst();
      return attr.orElse(null);
   }

   public Attribute getLinkTypeAttribute(final LinkType linkType, final String attributeTemplateId) {
      var attr = linkType.getAttributes().stream().filter(a -> a.getId().equals(attributeTemplateId)).findFirst();
      return attr.orElse(null);
   }

   public Function getFunction(final JSONObject o) {
      return new Function(cureJs((String) o.get(Function.JS)), cureXml((String) o.get(Function.XML)), null, 0, (Boolean) o.get(Function.EDITABLE));
   }

   private Rule getRule(final JSONObject o) {
      var name = (String) o.get(Rule.NAME);
      var type = Rule.RuleType.valueOf(o.get(Rule.TYPE).toString());
      var timing = Utils.computeIfNotNull(o.get(Rule.TIMING), t -> Rule.RuleTiming.valueOf(t.toString()));
      var rule = new Rule(name, type, timing, new DataDocument((JSONObject) o.get("configuration")));

      if (type == Rule.RuleType.BLOCKLY) {
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_JS, cureJs(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_JS)));
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_XML, cureXml(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_XML)));
      }

      if (type == Rule.RuleType.CRON) {
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_JS, cureJs(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_JS)));
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_XML, cureXml(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_XML)));
         rule.getConfiguration().put(CronRule.CRON_SINCE, rule.getConfiguration().getDate(CronRule.CRON_SINCE));
         rule.getConfiguration().put(CronRule.CRON_UNTIL, rule.getConfiguration().getDate(CronRule.CRON_UNTIL));
         rule.getConfiguration().put(CronRule.CRON_LAST_RUN, rule.getConfiguration().getDate(CronRule.CRON_LAST_RUN));
         rule.getConfiguration().put(CronRule.CRON_WHEN, rule.getConfiguration().getLong(CronRule.CRON_WHEN));
         rule.getConfiguration().put(CronRule.CRON_INTERVAL, rule.getConfiguration().getInteger(CronRule.CRON_INTERVAL));
         rule.getConfiguration().put(CronRule.CRON_UNIT, rule.getConfiguration().getString(CronRule.CRON_UNIT));
      }

      if (type == Rule.RuleType.WIZARD) {
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_JS, cureJs(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_JS)));
         rule.getConfiguration().put(BlocklyRule.BLOCKLY_XML, cureXml(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_XML)));
         rule.getConfiguration().put(WizardRule.WIZARD_CONFIGURATION, rule.getConfiguration().getString(WizardRule.WIZARD_CONFIGURATION));
      }

      if (type == Rule.RuleType.AUTO_LINK) {
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_LINK_TYPE, templateParser.getDict().getLinkTypeId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_LINK_TYPE)));
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_COLLECTION1, templateParser.getDict().getCollectionId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_COLLECTION1)));
         rule.getConfiguration().put(AutoLinkRule.AUTO_LINK_COLLECTION2, templateParser.getDict().getCollectionId(rule.getConfiguration().getString(AutoLinkRule.AUTO_LINK_COLLECTION2)));
      }

      if (type == Rule.RuleType.ZAPIER) {
         rule.getConfiguration().put(ZapierRule.HOOK_URL, templateParser.getDict().getLinkTypeId(rule.getConfiguration().getString(ZapierRule.HOOK_URL)));
         rule.getConfiguration().put(ZapierRule.SUBSCRIBE_ID, templateParser.getDict().getLinkTypeId(rule.getConfiguration().getString(ZapierRule.SUBSCRIBE_ID)));
      }

      return rule;
   }

   private String cureJs(final String js) {
      var res = TemplateParserUtils.replacer(js, ", '", Pattern.quote("')"), id -> {
         if (ObjectId.isValid(id)) {
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

      res = TemplateParserUtils.replacer(res, Pattern.quote("('"), Pattern.quote("')"), id -> {
         if (ObjectId.isValid(id)) {
            var collId = templateParser.getDict().getCollectionId(id);
            if (collId != null) {
               return collId;
            } else {
               var viewId = templateParser.getDict().getViewId(id);
               if (viewId != null) {
                  return viewId;
               }
            }
         }

         return id;
      });

      res = TemplateParserUtils.replacer(res, Pattern.quote("navigate('"), Pattern.quote("',"), id -> {
         if (ObjectId.isValid(id)) {
            var viewId = templateParser.getDict().getViewId(id);
            if (viewId != null) {
               return viewId;
            }
         }

         return id;
      });

      res = TemplateParserUtils.replacer(res, Pattern.quote("getSiblings('"), Pattern.quote("',"), id -> {
         if (id.length() == 24) {
            var linkId = templateParser.getDict().getLinkTypeId(id);
            if (linkId != null) {
               return linkId;
            }
         }

         return id;
      });

      return res;
   }

   private String cureXml(final String xml) {
      /*
        block type=
        variables_get_$collectionId_document
        variables_get_$linkTypeId_linkinst
        $linktTypeId-$collectionId_$collectionId_link
        $linktTypeId-$collectionId_$collectionId_link_instance


        variable type=
        variabletype=
        $collectionId_document
        $collectionId_document_array
        $linkTypeId_linkinst
        $linkTypeId_link_array

        <field name="COLLECTION">5cf6eb98857aba008f0655d8</field>
       */
      var res = TemplateParserUtils.replacer(xml, "<block type=\"variables_get_", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<block type=\"variables_get_", "_linkinst", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "<block type=\"", "-[0-9a-f]{24}_[0-9a-f]{24}_link", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "<block type=\"[0-9a-f]{24}-", "_[0-9a-f]{24}_link", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<block type=\"[0-9a-f]{24}-[0-9a-f]{24}_", "_link", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<variable type=\"", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<variable type=\"", "_link", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "variabletype=\"", "_document", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "variabletype=\"", "_link", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "<field name=\"COLLECTION\">", "</field>", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<field name=\"COLLECTION_ID\">", "</field>", templateParser.getDict()::getCollectionId);
      res = TemplateParserUtils.replacer(res, "<field name=\"LINKTYPE\">", "</field>", templateParser.getDict()::getLinkTypeId);
      res = TemplateParserUtils.replacer(res, "<field name=\"VIEW_ID\">", "</field>", templateParser.getDict()::getViewId);

      return res;
   }
}
