/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller;

import static io.lumeer.engine.api.LumeerConst.*;

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.rest.dao.LinkInstance;
import io.lumeer.engine.rest.dao.LinkType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;

/**
 * Manipulates with documents links
 */
@SessionScoped
public class LinkingFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private ProjectFacade projectFacade;

   public void onDropDocument(@Observes(notifyObserver = Reception.IF_EXISTS) final DropDocument dropDocument) {
      dropLinksForDocument(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, Linking.LinkDirection.FROM);
      dropLinksForDocument(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, Linking.LinkDirection.TO);
   }

   @PostConstruct
   public void init(){
      if (!dataStorage.hasCollection(Linking.Type.NAME)) {
         dataStorage.createCollection(Linking.Type.NAME);
         dataStorage.createIndex(Linking.Type.NAME, new DataDocument(Linking.Type.ATTR_PROJECT, Index.ASCENDING)
               .append(Linking.Type.ATTR_FROM_COLLECTION, Index.ASCENDING)
               .append(Linking.Type.ATTR_TO_COLLECTION, Index.ASCENDING)
               .append(Linking.Type.ATTR_ROLE, Index.ASCENDING), true);
         dataStorage.createIndex(Linking.Type.NAME, new DataDocument(Linking.Type.ATTR_PROJECT, Index.ASCENDING)
               .append(Linking.Type.ATTR_TO_COLLECTION, Index.ASCENDING)
               .append(Linking.Type.ATTR_FROM_COLLECTION, Index.ASCENDING)
               .append(Linking.Type.ATTR_ROLE, Index.ASCENDING), true);
      }
   }

   /**
    * Read all link types for selected collection.
    *
    * @param collectionName
    *       The name of the collection.
    * @param linkDirection
    *       Direction of link.
    * @return List of all link types.
    */
   public List<LinkType> readLinkTypesForCollection(final String collectionName, final Linking.LinkDirection linkDirection) {
      return readLinkingTypesForCollection(collectionName, null, linkDirection)
            .stream()
            .map(LinkType::new)
            .collect(Collectors.toList());
   }

   /**
    * Read all links for selected collection.
    *
    * @param collectionName
    *       The name of the collection.
    * @param linkDirection
    *       Direction of link.
    * @param role
    *       Role name.
    * @return List of all links.
    */
   public List<LinkInstance> readLinkInstancesForCollection(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingTypesForCollection(collectionName, role, linkDirection);

      List<LinkInstance> linkInstances = new ArrayList<>();
      String linkingCollectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         LinkType linkType = new LinkType(lt);

         List<DataDocument> ls = dataStorage.search(linkingCollectionName, dataStorageDialect.fieldValueFilter(Linking.Instance.ATTR_TYPE_ID, lt.getId()), null, 0, 0);
         for (DataDocument doc : ls) {
            String fromDocumentId = linkDirection == Linking.LinkDirection.FROM ? doc.getString(Linking.Instance.ATTR_FROM_ID) : doc.getString(Linking.Instance.ATTR_TO_ID);
            String toDocumentId = linkDirection == Linking.LinkDirection.FROM ? doc.getString(Linking.Instance.ATTR_TO_ID) : doc.getString(Linking.Instance.ATTR_FROM_ID);
            linkInstances.add(new LinkInstance(linkType, fromDocumentId, toDocumentId, doc.getDataDocument(Linking.Instance.ATTR_ATTRIBUTES)));
         }
      }
      return linkInstances;
   }

   /**
    * Read all links between two documents
    *
    * @param firstCollectionName
    *       The name of the first document's collection.
    * @param firstDocumentId
    *       The id of the first document.
    * @param secondCollectionName
    *       The name of the second document's collection.
    * @param secondDocumentId
    *       The id of the second document.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    * @return List of all links.
    */
   public List<LinkInstance> readLinkInstancesBetweenDocuments(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingTypesBetweenCollections(firstCollectionName, secondCollectionName, role, linkDirection);

      List<LinkInstance> linkInstances = new ArrayList<>();
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         LinkType linkType = new LinkType(lt);

         DataDocument dc = dataStorage.readDocument(collectionName, filterLinkingInstanceBetweenDocuments(lt.getId(), firstDocumentId, secondDocumentId, linkDirection));
         if (dc != null) {
            String fromId = linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId;
            String toId = linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId;

            linkInstances.add(new LinkInstance(linkType, fromId, toId, dc.getDataDocument(Linking.Instance.ATTR_ATTRIBUTES)));
         }
      }

      return linkInstances;
   }

   /**
    * Read all linking documents for specified document.
    *
    * @param collectionName
    *       The name of the document's collection.
    * @param documentId
    *       The id of the document to search for links.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    * @return List of all linked documents.
    */
   public List<DataDocument> readLinkedDocumentsForDocument(final String collectionName, final String documentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingTypesForCollection(collectionName, role, linkDirection);

      return readDocumentsFromLinkInstances(linkingDocs, documentId, linkDirection);
   }

   /**
    * Read all linking documents for specified document and collection.
    *
    * @param firstCollectionName
    *       The name of the document's collection.
    * @param firstDocumentId
    *       The id of the document to search for links.
    * @param secondCollectionName
    *       The name of the collection to search for linking documents.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    * @return List of all linked documents.
    */
   public List<DataDocument> readLinkedDocumentsBetweenDocumentAndCollection(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingTypesBetweenCollections(firstCollectionName, secondCollectionName, role, linkDirection);

      return readDocumentsFromLinkInstances(linkingDocs, firstDocumentId, linkDirection);
   }

   /**
    * Drop all links for specified document
    *
    * @param collectionName
    *       The name of the document's collection.
    * @param documentId
    *       The id of the document to drop links.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void dropLinksForDocument(final String collectionName, final String documentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingTypesForCollection(collectionName, role, linkDirection);
      dropLinksForDocument(linkingDocs, documentId, linkDirection);
   }

   /**
    * Drop all links for specified collection.
    *
    * @param collectionName
    *       the name of the collection to drop links.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void dropLinksForCollection(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingTypes = readLinkingTypesForCollection(collectionName, role, linkDirection);
      String linkingCollectionName = buildCollectionName();
      for (DataDocument lt : linkingTypes) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(linkingCollectionName, filterLinkingInstance(id));
         dataStorage.dropDocument(Linking.Type.NAME, dataStorageDialect.documentIdFilter(id));
      }
   }

   /**
    * Drop link between two documents.
    *
    * @param firstCollectionName
    *       The name of the first document's collection.
    * @param firstDocumentId
    *       The id of the first document.
    * @param secondCollectionName
    *       The name of the second document's collection.
    * @param secondDocumentId
    *       The id of the second document.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void dropLinksBetweenDocuments(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingTypes = readLinkingTypesBetweenCollections(firstCollectionName, secondCollectionName, role, linkDirection);
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingTypes) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(collectionName, filterLinkingInstanceBetweenDocuments(id, firstDocumentId, secondDocumentId, linkDirection));
         if (linkTypeIsEmpty(id)) {
            dataStorage.dropDocument(Linking.Type.NAME, dataStorageDialect.documentIdFilter(id));
         }
      }
   }

   /**
    * Drop link between document and collection.
    *
    * @param firstCollectionName
    *       The name of the document's collection.
    * @param firstDocumentId
    *       The id of the document.
    * @param secondCollectionName
    *       The name of the collection to drop links.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void dropLinksBetweenDocumentAndCollection(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingTypes = readLinkingTypesBetweenCollections(firstCollectionName, secondCollectionName, role, linkDirection);
      dropLinksForDocument(linkingTypes, firstDocumentId, linkDirection);
   }

   /**
    * Create link between two documents.
    *
    * @param firstCollectionName
    *       The name of the first document's collection.
    * @param firstDocumentId
    *       The id of the first document.
    * @param secondCollectionName
    *       The name of the second document's collection.
    * @param secondDocumentId
    *       The id of the second document.
    * @param attributes
    *       Attributes of link.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void createLinkInstanceBetweenDocuments(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final DataDocument attributes, final String role, final Linking.LinkDirection linkDirection) {
      String typeId = createNewLinkingTypeIfNecessary(firstCollectionName, secondCollectionName, role, linkDirection);

      DataDocument dataDocument = new DataDocument(Linking.Instance.ATTR_TYPE_ID, typeId)
            .append(Linking.Instance.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId)
            .append(Linking.Instance.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId)
            .append(Linking.Instance.ATTR_ATTRIBUTES, attributes);
      dataStorage.createDocument(buildCollectionName(), dataDocument);
   }

   /**
    * Create link from document to many documents.
    *
    * @param firstCollectionName
    *       The name of the first document's collection.
    * @param firstDocumentId
    *       The id of the first document.
    * @param secondCollectionName
    *       The name of the second document's collection.
    * @param secondDocumentsIds
    *       The ids of documents to create link.
    * @param attributesList
    *       Attributes of links.
    * @param role
    *       Role name.
    * @param linkDirection
    *       Direction of link.
    */
   public void createLinkInstancesBetweenDocumentAndCollection(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final List<String> secondDocumentsIds, final List<DataDocument> attributesList, final String role, final Linking.LinkDirection linkDirection) {
      String typeId = createNewLinkingTypeIfNecessary(firstCollectionName, secondCollectionName, role, linkDirection);

      List<DataDocument> dataDocuments = new LinkedList<>();
      for (int i = 0; i < secondDocumentsIds.size(); i++) {
         dataDocuments.add(new DataDocument(Linking.Instance.ATTR_TYPE_ID, typeId)
               .append(Linking.Instance.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentsIds.get(i))
               .append(Linking.Instance.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentsIds.get(i) : firstDocumentId)
               .append(Linking.Instance.ATTR_ATTRIBUTES, attributesList.get(i))
         );
      }
      dataStorage.createDocuments(buildCollectionName(), dataDocuments);
   }

   private List<DataDocument> readDocumentsFromLinkInstances(final List<DataDocument> linkingDocs, final String documentId, final Linking.LinkDirection linkDirection) {
      List<DataDocument> links = new ArrayList<>();
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String readCollectionName = linkDirection == Linking.LinkDirection.FROM ? lt.getString(Linking.Type.ATTR_TO_COLLECTION) : lt.getString(Linking.Type.ATTR_FROM_COLLECTION);
         String param = linkDirection == Linking.LinkDirection.FROM ? Linking.Instance.ATTR_TO_ID : Linking.Instance.ATTR_FROM_ID;
         List<DataDocument> docs = dataStorage.search(collectionName, filterLinkingInstanceForDocument(lt.getId(), documentId, linkDirection), null, 0, 0);
         for (DataDocument dc : docs) {
            String id = dc.getString(param);
            DataDocument doc = dataStorage.readDocument(readCollectionName, dataStorageDialect.documentIdFilter(id));
            if (doc != null) {
               links.add(doc);
            }
         }
      }
      return links;
   }

   private void dropLinksForDocument(final List<DataDocument> linkingDocs, final String documentId, final Linking.LinkDirection linkDirection) {
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(collectionName, filterLinkingInstanceForDocument(id, documentId, linkDirection));
         if (linkTypeIsEmpty(id)) {
            dataStorage.dropDocument(Linking.Type.NAME, dataStorageDialect.documentIdFilter(id));
         }
      }
   }

   private boolean linkTypeIsEmpty(final String id) {
      return dataStorage.search(buildCollectionName(), dataStorageDialect.fieldValueFilter(Linking.Instance.ATTR_TYPE_ID, id), null, 0, 1).isEmpty();
   }

   private List<DataDocument> readLinkingTypesForCollection(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      String param = linkDirection == Linking.LinkDirection.FROM ? Linking.Type.ATTR_FROM_COLLECTION : Linking.Type.ATTR_TO_COLLECTION;
      return dataStorage.search(Linking.Type.NAME, filterLinkingTypeForCollection(param, collectionName, role), null, 0, 0);
   }

   private List<DataDocument> readLinkingTypesBetweenCollections(final String firstCollectionName, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      String fromCollectionName = linkDirection == Linking.LinkDirection.FROM ? firstCollectionName : secondCollectionName;
      String toCollectionName = linkDirection == Linking.LinkDirection.FROM ? secondCollectionName : firstCollectionName;
      return dataStorage.search(Linking.Type.NAME, filterLinkingTypeBetweenCollections(fromCollectionName, toCollectionName, role), null, 0, 0);
   }

   private String createNewLinkingTypeIfNecessary(final String firstCollectionName, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      String fromCollectionName = linkDirection == Linking.LinkDirection.FROM ? firstCollectionName : secondCollectionName;
      String toCollectionName = linkDirection == Linking.LinkDirection.FROM ? secondCollectionName : firstCollectionName;

      DataDocument linkingType = dataStorage.readDocument(Linking.Type.NAME, filterLinkingTypeBetweenCollections(fromCollectionName, toCollectionName, role));
      if (linkingType != null) { // if linking type already exists, we return it
         return linkingType.getId();
      }

      //otherwise we create linking type and also collection for link if necessary
      DataDocument doc = new DataDocument();
      doc.put(Linking.Type.ATTR_FROM_COLLECTION, fromCollectionName);
      doc.put(Linking.Type.ATTR_TO_COLLECTION, toCollectionName);
      doc.put(Linking.Type.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      doc.put(Linking.Type.ATTR_ROLE, role);

      String typeId = dataStorage.createDocument(Linking.Type.NAME, doc);

      String linkingCollectionName = buildCollectionName();
      if (!dataStorage.hasCollection(linkingCollectionName)) {
         dataStorage.createCollection(linkingCollectionName);
         dataStorage.createIndex(linkingCollectionName, new DataDocument(Linking.Instance.ATTR_TYPE_ID, Index.ASCENDING)
               .append(Linking.Instance.ATTR_FROM_ID, Index.ASCENDING)
               .append(Linking.Instance.ATTR_TO_ID, Index.ASCENDING), true);
         dataStorage.createIndex(linkingCollectionName, new DataDocument(Linking.Instance.ATTR_TYPE_ID, Index.ASCENDING)
               .append(Linking.Instance.ATTR_TO_ID, Index.ASCENDING)
               .append(Linking.Instance.ATTR_FROM_ID, Index.ASCENDING), true);
      }

      return typeId;
   }

   private DataFilter filterLinkingInstance(final String typeId) {
      return dataStorageDialect.fieldValueFilter(Linking.Instance.ATTR_TYPE_ID, typeId);
   }

   private DataFilter filterLinkingInstanceForDocument(final String typeId, final String documentId, Linking.LinkDirection linkDirection) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.Instance.ATTR_TYPE_ID, typeId);
      fields.put(linkDirection == Linking.LinkDirection.FROM ? Linking.Instance.ATTR_FROM_ID : Linking.Instance.ATTR_TO_ID, documentId);
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter filterLinkingInstanceBetweenDocuments(final String typeId, final String firstDocumentId, final String secondDocumentId, Linking.LinkDirection linkDirection) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.Instance.ATTR_TYPE_ID, typeId);
      fields.put(Linking.Instance.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId);
      fields.put(Linking.Instance.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId);
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter filterLinkingTypeForCollection(final String param, final String collectionName, final String role) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.Type.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      fields.put(param, collectionName);
      if (role != null) {
         fields.put(Linking.Type.ATTR_ROLE, role);
      }
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter filterLinkingTypeBetweenCollections(final String fromCollectionName, final String toCollectionName, final String role) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.Type.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      fields.put(Linking.Type.ATTR_FROM_COLLECTION, fromCollectionName);
      fields.put(Linking.Type.ATTR_TO_COLLECTION, toCollectionName);
      if (role != null) {
         fields.put(Linking.Type.ATTR_ROLE, role);
      }
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private String buildCollectionName() {
      return Linking.PREFIX + "_" + projectFacade.getCurrentProjectId();
   }

}
