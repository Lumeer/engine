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
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class FunctionXmlParser {

   private static final String TYPE_ATTRIBUTE = "type";
   private static final String NAME_ATTRIBUTE = "name";
   private static final String DOCUMENT_ATTRIBUTE_VALUE = "DOCUMENT";
   private static final String ATTR_ATTRIBUTE_VALUE = "ATTR";
   private static final String GET_ATTRIBUTE_VALUE = "get_attribute";
   private static final String BLOCK_TAG = "block";
   private static final String FIELD_TAG = "field";
   private static final String VALUE_TAG = "value";
   private static final String LINK_SUFFIX = "_link";
   private static final String VARIABLE_PREFIX = "variables_get_";

   private FunctionXmlParser() {}

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

         if (GET_ATTRIBUTE_VALUE.equals(element.getAttribute(TYPE_ATTRIBUTE))) {
            final AttributeReference attributeReference = new AttributeReference();


            NodeList fields = element.getElementsByTagName(FIELD_TAG);
            nodeListIterator(fields, Node.ELEMENT_NODE, field -> {
               final Element fieldElement = (Element) field;
               if (ATTR_ATTRIBUTE_VALUE.equals(fieldElement.getAttribute(NAME_ATTRIBUTE))) {
                  attributeReference.setAttributeId(fieldElement.getTextContent());
               }
            });

            NodeList values = element.getElementsByTagName(VALUE_TAG);
            nodeListIterator(values, Node.ELEMENT_NODE, value -> {
               final Element valueElement = (Element) value;
               if (DOCUMENT_ATTRIBUTE_VALUE.equals(valueElement.getAttribute(NAME_ATTRIBUTE))) {

                  final NodeList childBlocks = element.getElementsByTagName(BLOCK_TAG);
                  nodeListIterator(childBlocks, Node.ELEMENT_NODE, childBlock -> {
                     final Element childBlockElement = (Element) childBlock;
                     final String type = childBlockElement.getAttribute(TYPE_ATTRIBUTE);

                     if (type != null) {

                        if (type.endsWith(LINK_SUFFIX)) {
                           attributeReference.setLinkTypeId(type.split("-")[0]);

                           NodeList childValues = childBlockElement.getElementsByTagName(VALUE_TAG);
                           nodeListIterator(childValues, Node.ELEMENT_NODE, childValue -> {
                              final Element childValueElement = (Element) childValue;
                              if (DOCUMENT_ATTRIBUTE_VALUE.equals(childValueElement.getAttribute(NAME_ATTRIBUTE))) {
                                 final NodeList variableBlocks = childValueElement.getElementsByTagName(BLOCK_TAG);
                                 nodeListIterator(variableBlocks, Node.ELEMENT_NODE, variable -> {
                                    final String variableType = ((Element) variable).getAttribute(TYPE_ATTRIBUTE);
                                    if (variableType != null && variableType.startsWith(VARIABLE_PREFIX)) {
                                       attributeReference.setCollectionId(variableType.split("_")[2]);
                                    }
                                 });
                              }
                           });

                        } else if (type.startsWith(VARIABLE_PREFIX)) {
                           attributeReference.setCollectionId(type.split("_")[2]);
                        }
                     }
                  });
               }
            });
            attributeReferences.add(attributeReference);
         }
      });

      return attributeReferences;
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
