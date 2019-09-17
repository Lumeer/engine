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
package io.lumeer.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class FunctionXmlParser {

   private static final String TYPE_ATTRIBUTE = "type";
   private static final String NAME_ATTRIBUTE = "name";
   private static final String DOCUMENT_ATTRIBUTE_VALUE = "DOCUMENT";
   private static final String LINK_ATTRIBUTE_VALUE = "LINK";
   private static final String ATTR_ATTRIBUTE_VALUE = "ATTR";
   private static final String COLLECTION_ATTRIBUTE_VALUE = "COLLECTION";
   private static final String GET_ATTRIBUTE_VALUE = "get_attribute";
   private static final String GET_LINK_ATTRIBUTE_VALUE = "get_link_attribute";
   private static final String GET_LINK_DOCUMENT_TYPE = "get_link_document";
   private static final String BLOCK_TAG = "block";
   private static final String FIELD_TAG = "field";
   private static final String VALUE_TAG = "value";
   private static final String LINK_SUFFIX = "_link";
   private static final String LINK_INSTANCE_SUFFIX = "_linkinst";
   private static final String LINK_INSTANCE_SUFFIX2 = "_link_instance";
   private static final String VARIABLE_PREFIX = "variables_get_";

   private FunctionXmlParser() {
   }

   public static List<AttributeReference> parseFunctionXml(final String xml) throws IllegalStateException {
      final List<AttributeReference> attributeReferences = new ArrayList<>();
      final Document doc;

      if (xml == null || "".equals(xml)) {
         return attributeReferences;
      }

      try (ByteArrayInputStream baos = new ByteArrayInputStream(xml.getBytes("UTF-8"))) {
         doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(baos);
      } catch (ParserConfigurationException | IOException | SAXException e) {
         throw new IllegalStateException("Could not parse function xml: ", e);
      }

      doc.getDocumentElement().normalize();
      final NodeList blocks = doc.getElementsByTagName(BLOCK_TAG);

      nodeListIterator(blocks, Node.ELEMENT_NODE, node -> {
         final Element element = (Element) node;

         // <block type="get_attribute" ...>
         if (GET_ATTRIBUTE_VALUE.equals(element.getAttribute(TYPE_ATTRIBUTE))) {
            attributeReferences.add(parseAttributeGetter(element));
         }

         // <block type="get_link_attribute" ...>
         if (GET_LINK_ATTRIBUTE_VALUE.equals(element.getAttribute(TYPE_ATTRIBUTE))) {
            attributeReferences.add(parseLinkAttributeGetter(element));
         }
      });

      return attributeReferences;
   }

   private static AttributeReference parseLinkAttributeGetter(final Element element) {
      final AttributeReference attributeReference = new AttributeReference();

      // <field name="ATTR">a4</field>
      attributeReference.setAttributeId(parseAttributeId(element));

      Node value = element.getFirstChild();
      while (value != null) {
         if (value.getNodeType() == Node.ELEMENT_NODE && value.getNodeName().equals(VALUE_TAG)) {
            final Element valueElement = (Element) value;

            // <value name="LINK">
            if (LINK_ATTRIBUTE_VALUE.equals(valueElement.getAttribute(NAME_ATTRIBUTE))) {

               Node childBlock = value.getFirstChild();
               while (childBlock != null) {
                  if (childBlock.getNodeType() == Node.ELEMENT_NODE && childBlock.getNodeName().equals(BLOCK_TAG)) {

                     final Element childBlockElement = (Element) childBlock;
                     final String type = childBlockElement.getAttribute(TYPE_ATTRIBUTE);

                     if (type != null) {

                        // <block type="variables_get_5c5b6a73b9437f682e35d3ba_linkinst" ...>
                        if (type.endsWith(LINK_INSTANCE_SUFFIX)) {
                           final String linkTypeId = type.split("_")[2];
                           attributeReference.setLinkTypeId(linkTypeId);
                        } else if (type.endsWith(LINK_INSTANCE_SUFFIX2)) { // <block type="5d81646d652b2314cb092faa-5d571fcfa9013c4e0ac956a5_5d66eb96b08a0d090c2efee6_link_instance"
                           final String linkTypeId = type.split("-")[0];
                           attributeReference.setLinkTypeId(linkTypeId);
                        }
                     }

                  }
                  childBlock = childBlock.getNextSibling();
               }
            }
         }

         value = value.getNextSibling();
      }

      return attributeReference;
   }

   private static AttributeReference parseAttributeGetter(final Element element) {
      final AttributeReference attributeReference = new AttributeReference();

      // <field name="ATTR">a4</field>
      attributeReference.setAttributeId(parseAttributeId(element));

      Node value = element.getFirstChild();
      while (value != null) {
         if (value.getNodeType() == Node.ELEMENT_NODE && value.getNodeName().equals(VALUE_TAG)) {
            final Element valueElement = (Element) value;

            // <value name="DOCUMENT">
            if (DOCUMENT_ATTRIBUTE_VALUE.equals(valueElement.getAttribute(NAME_ATTRIBUTE))) {

               Node childBlock = value.getFirstChild();
               while (childBlock != null) {
                  if (childBlock.getNodeType() == Node.ELEMENT_NODE && childBlock.getNodeName().equals(BLOCK_TAG)) {

                     final Element childBlockElement = (Element) childBlock;
                     final String type = childBlockElement.getAttribute(TYPE_ATTRIBUTE);

                     if (type != null) {

                        // <block type="5c6f3b389e9ffa43fe6a2dcc-5becabd79e9ffa0a690775ea_5c6ea86e9e9ffa355dffe274_link" ...>
                        if (type.endsWith(LINK_SUFFIX)) {
                           final String[] typeSplit = type.split("-");
                           final String[] collectionTypes = typeSplit[1].split("_");
                           attributeReference.setLinkTypeId(typeSplit[0]);

                           NodeList childValues = childBlockElement.getElementsByTagName(VALUE_TAG);
                           nodeListIterator(childValues, Node.ELEMENT_NODE, childValue -> {
                              final Element childValueElement = (Element) childValue;
                              if (DOCUMENT_ATTRIBUTE_VALUE.equals(childValueElement.getAttribute(NAME_ATTRIBUTE))) {
                                 final NodeList variableBlocks = childValueElement.getElementsByTagName(BLOCK_TAG);
                                 nodeListIterator(variableBlocks, Node.ELEMENT_NODE, variable -> {
                                    final String variableType = ((Element) variable).getAttribute(TYPE_ATTRIBUTE);
                                    if (variableType != null && variableType.startsWith(VARIABLE_PREFIX)) {
                                       final String thisCollectionId = variableType.split("_")[2];
                                       attributeReference.setCollectionId(
                                             thisCollectionId.equals(collectionTypes[0]) ?
                                                   collectionTypes[1] :
                                                   collectionTypes[0]
                                       );
                                    }
                                 });
                              }
                           });


                        } else if (type.startsWith(VARIABLE_PREFIX)) { // <block type="variables_get_5c6ea86e9e9ffa355dffe274_document" ...>
                           attributeReference.setCollectionId(type.split("_")[2]);
                        } else if (type.equals(GET_LINK_DOCUMENT_TYPE)) { // <block type="get_link_document" ...>


                           NodeList childFields = childBlockElement.getElementsByTagName(FIELD_TAG);
                           nodeListIterator(childFields, Node.ELEMENT_NODE, childField -> {
                              final Element childFieldElement = (Element) childField;

                              // <field name="COLLECTION">5c5b3f01b9437f682e35d3b5</field>
                              if (COLLECTION_ATTRIBUTE_VALUE.equals(childFieldElement.getAttribute(NAME_ATTRIBUTE))) {
                                 attributeReference.setCollectionId(childFieldElement.getTextContent());
                              }
                           });

                           // <value name="LINK">
                           NodeList childValues = childBlockElement.getElementsByTagName(VALUE_TAG);
                           nodeListIterator(childValues, Node.ELEMENT_NODE, childValue -> {
                              final Element childValueElement = (Element) childValue;
                              if (LINK_ATTRIBUTE_VALUE.equals(childValueElement.getAttribute(NAME_ATTRIBUTE))) {

                                 // <block type="variables_get_5c5b6a73b9437f682e35d3ba_linkinst" ...>
                                 final NodeList variableBlocks = childValueElement.getElementsByTagName(BLOCK_TAG);
                                 nodeListIterator(variableBlocks, Node.ELEMENT_NODE, variable -> {
                                    final String variableType = ((Element) variable).getAttribute(TYPE_ATTRIBUTE);
                                    if (variableType != null && variableType.endsWith(LINK_INSTANCE_SUFFIX)) {
                                       final String thisLinkId = variableType.split("_")[2];
                                       attributeReference.setLinkTypeId(thisLinkId);
                                    }
                                 });
                              }
                           });
                        }
                     }
                  }

                  childBlock = childBlock.getNextSibling();
               }
            }
         }

         value = value.getNextSibling();
      }

      return attributeReference;
   }

   private static String parseAttributeId(final Element element) {
      final NodeList fields = element.getElementsByTagName(FIELD_TAG);
      final AtomicReference<String> atomicString = new AtomicReference<>("");

      nodeListIterator(fields, Node.ELEMENT_NODE, field -> {
         final Element fieldElement = (Element) field;
         if (ATTR_ATTRIBUTE_VALUE.equals(fieldElement.getAttribute(NAME_ATTRIBUTE))) {
            atomicString.set(fieldElement.getTextContent());
         }
      });

      return atomicString.get();
   }

   private static void nodeListIterator(final NodeList nodeList, final Short typeFilter, final Consumer<Node> consumer) {
      for (int i = 0; i < nodeList.getLength(); i++) {
         Node n = nodeList.item(i);
         if (typeFilter == null || n.getNodeType() == typeFilter) {
            consumer.accept(n);
         }
      }
   }

   public static class AttributeReference {
      private String attributeId;
      private String collectionId;
      private String linkTypeId;

      public AttributeReference() {
      }

      public AttributeReference(final String attributeId, final String collectionId, final String linkTypeId) {
         this.attributeId = attributeId;
         this.collectionId = collectionId;
         this.linkTypeId = linkTypeId;
      }

      public String getAttributeId() {
         return attributeId;
      }

      public String getCollectionId() {
         return collectionId;
      }

      public String getLinkTypeId() {
         return linkTypeId;
      }

      public void setAttributeId(final String attributeId) {
         this.attributeId = attributeId;
      }

      public void setCollectionId(final String collectionId) {
         this.collectionId = collectionId;
      }

      public void setLinkTypeId(final String linkTypeId) {
         this.linkTypeId = linkTypeId;
      }

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         final AttributeReference that = (AttributeReference) o;
         return attributeId.equals(that.attributeId) &&
               Objects.equals(collectionId, that.collectionId) &&
               Objects.equals(linkTypeId, that.linkTypeId);
      }

      @Override
      public int hashCode() {
         return Objects.hash(attributeId, collectionId, linkTypeId);
      }

      @Override
      public String toString() {
         return "AttributeReference{" +
               "attributeId='" + attributeId + '\'' +
               ", collectionId='" + collectionId + '\'' +
               ", linkTypeId='" + linkTypeId + '\'' +
               '}';
      }
   }
}
