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
package io.lumeer.core.facade;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.AttributesResource;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.api.model.rule.CronRule;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.facade.conversion.ConversionFacade;
import io.lumeer.core.task.AutoLinkBatchTask;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DefaultViewConfigDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class CollectionFacade extends AbstractFacade {

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @Inject
   private ConversionFacade conversionFacade;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private SelectionListDao selectionListDao;

   @Inject
   private DefaultViewConfigDao defaultViewConfigDao;

   @Inject
   private ContextualTaskFactory taskFactory;

   @Inject
   private TaskExecutor taskExecutor;

   private CollectionAdapter adapter;
   private ResourceAdapter resourceAdapter;

   @PostConstruct
   public void init() {
      adapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
      resourceAdapter = new ResourceAdapter(permissionsChecker.getPermissionAdapter(), collectionDao, linkTypeDao, viewDao, userDao);
   }

   public Collection createCollection(Collection collection) {
      return createCollection(collection, false);
   }

   public Collection createCollection(Collection collection, final boolean skipLimits) {
      checkProjectRole(RoleType.CollectionContribute);
      long collectionsCount = collectionDao.getCollectionsCount();

      if (!skipLimits) {
         permissionsChecker.checkCreationLimits(collection, collectionsCount);
         permissionsChecker.checkRulesLimit(collection);
         permissionsChecker.checkFunctionsLimit(collection);
      }
      permissionsChecker.checkRulesPermissions(collection.getRules());

      final Set<Attribute> attributes = collection.getAttributes();

      collection.setAttributes(null);
      Collection storedCollection = createCollectionMetadata(collection);
      dataDao.createDataRepository(storedCollection.getId());

      if (attributes.size() > 0 && permissionsChecker.hasRole(storedCollection, RoleType.AttributeEdit)) {
         storedCollection.setAttributes(createCollectionAttributes(storedCollection, attributes).getAttributes());
      }

      return storedCollection;
   }

   public Collection updateCollection(final String collectionId, final Collection collection) {
      return updateCollection(collectionId, collection, false);
   }

   public Collection updateCollection(final String collectionId, final Collection collection, final boolean skipFceLimits) {
      final Collection storedCollection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(storedCollection, RoleType.Read);

      if (!skipFceLimits) {
         if (collection.someRuleChangedOrAdded(storedCollection)) {
            permissionsChecker.checkRulesLimit(collection);
         }
         if (collection.someFunctionChangedOrAdded(storedCollection)) {
            permissionsChecker.checkFunctionsLimit(collection);
         }
      }
      permissionsChecker.checkRulesPermissions(collection.getRules());
      permissionsChecker.checkAttributesFunctionAccess(collection.getAttributes());

      Collection updatingCollection = storedCollection.copy();
      updatingCollection.patch(collection, permissionsChecker.getActualRoles(storedCollection));
      updatingCollection.setLastTimeUsed(ZonedDateTime.now());
      keepUnmodifiableFields(updatingCollection, storedCollection);
      mapResourceUpdateValues(collection);

      return mapCollection(collectionDao.updateCollection(storedCollection.getId(), updatingCollection, storedCollection));
   }

   public Collection updateRules(final String collectionId, final Collection collection, final boolean skipFceLimits) {
      final Collection storedCollection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(storedCollection, RoleType.TechConfig);
      permissionsChecker.checkRulesPermissions(collection.getRules());

      Collection updatingCollection = storedCollection.copy();
      updatingCollection.setRules(collection.getRules());
      if (!skipFceLimits && updatingCollection.someRuleChangedOrAdded(storedCollection)) {
         permissionsChecker.checkRulesLimit(collection);
      }

      keepUnmodifiableFields(updatingCollection, storedCollection);
      mapResourceUpdateValues(collection);

      return mapCollection(collectionDao.updateCollection(storedCollection.getId(), updatingCollection, storedCollection));
   }

   private Collection mapCollection(Collection collection) {
      return mapResource(adapter.mapCollectionComputedProperties(mapResource(collection), getCurrentUserId(), workspaceKeeper.getProjectId()));
   }

   private void keepUnmodifiableFields(Collection collection, Collection storedCollection) {
      collection.setAttributes(storedCollection.getAttributes());
      collection.setLastAttributeNum(storedCollection.getLastAttributeNum());
   }

   public Collection upsertRule(final String collectionId, final String ruleId, final Rule rule) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.TechConfig);

      final Collection originalCollection = collection.copy();

      Map<String, Rule> rules = Objects.requireNonNullElse(collection.getRules(), new HashMap<>());

      Rule originalRule = rules.get(ruleId);
      rule.checkConfiguration(originalRule);

      permissionsChecker.checkRulePermissions(rule);

      rules.put(ruleId, rule);
      collection.setRules(rules);
      mapResourceUpdateValues(collection);

      final Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection, originalCollection);
      return mapCollection(updatedCollection);
   }

   public Collection updatePurpose(final String collectionId, final CollectionPurpose purpose) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.TechConfig);

      final Collection originalCollection = collection.copy();

      collection.setPurpose(purpose);
      mapResourceUpdateValues(collection);

      final Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection, originalCollection);
      return mapCollection(updatedCollection);
   }

   public void deleteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkCanDelete(collection);

      collectionDao.deleteCollection(collectionId);

      deleteCollectionBasedData(collectionId);

      fileAttachmentFacade.removeAllFileAttachments(collectionId, FileAttachment.AttachmentType.DOCUMENT);
   }

   private void deleteCollectionBasedData(final String collectionId) {
      var documentIds = documentDao.getDocumentsIdsByCollection(collectionId);
      resourceCommentDao.deleteComments(ResourceType.DOCUMENT, documentIds);

      documentDao.deleteDocuments(collectionId);
      dataDao.deleteDataRepository(collectionId);

      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByCollectionId(collectionId);
      if (!linkTypes.isEmpty()) {
         linkTypeDao.deleteLinkTypesByCollectionId(collectionId);
         linkInstanceDao.deleteLinkInstancesByLinkTypesIds(linkTypes.stream().map(LinkType::getId).collect(Collectors.toSet()));
      }

      favoriteItemDao.removeFavoriteCollectionFromUsers(getCurrentProject().getId(), collectionId);
      favoriteItemDao.removeFavoriteDocumentsByCollectionFromUsers(getCurrentProject().getId(), collectionId);
      defaultViewConfigDao.deleteByCollection(collectionId);
   }

   public Collection getCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      if (permissionsChecker.hasRoleInCollectionWithView(collection, RoleType.Read)) {
         return mapCollection(collection);
      }

      var userIdsInViews = resourceAdapter.getCollectionTransitiveReaders(getOrganization(), getProject(), collectionId);
      if (userIdsInViews.contains(getCurrentUserId())) {
         return mapCollection(collection);
      }

      throw new NoResourcePermissionException(collection);
   }

   public List<Collection> getCollections() {
      return mapCollectionsData(resourceAdapter.getCollections(getOrganization(), getProject(), getCurrentUserId())
                                               .stream()
                                               .map(this::mapResource)
                                               .collect(Collectors.toList()));
   }

   public List<Collection> getAllCollections() {
      return mapCollectionsData(resourceAdapter.getAllCollections(getOrganization(), getProject(), getCurrentUserId())
                                               .stream()
                                               .map(this::mapResource)
                                               .collect(Collectors.toList()));
   }

   public List<Collection> getPublicCollections() {
      if (permissionsChecker.isPublic()) {
         return mapCollectionsData(collectionDao.getAllCollections());
      }

      return List.of();
   }

   private List<Collection> mapCollectionsData(List<Collection> collections) {
      return adapter.mapCollectionsComputedProperties(collections, getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public void addFavoriteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.Read);

      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();
      favoriteItemDao.addFavoriteCollection(userId, projectId, collectionId);
   }

   public void removeFavoriteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.Read);

      String userId = getCurrentUser().getId();
      favoriteItemDao.removeFavoriteCollection(userId, collectionId);
   }

   public boolean isFavorite(String collectionId) {
      return isFavorite(collectionId, getCurrentUser().getId());
   }

   public boolean isFavorite(String collectionId, String userId) {
      return getFavoriteCollectionsIds(userId).contains(collectionId);
   }

   public Set<String> getFavoriteCollectionsIds() {
      return getFavoriteCollectionsIds(getCurrentUser().getId());
   }

   public Set<String> getFavoriteCollectionsIds(String userId) {
      String projectId = getCurrentProject().getId();

      return adapter.getFavoriteCollectionIds(userId, projectId);
   }

   public long getDocumentsCountInAllCollections() {
      return adapter.getDocumentsCount();
   }

   public java.util.Collection<Attribute> createCollectionAttributes(final String collectionId, final java.util.Collection<Attribute> attributes) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      return createCollectionAttributes(collection, attributes).getAttributes();
   }

   public Collection createCollectionAttributes(final Collection collection, final java.util.Collection<Attribute> attributes) {
      permissionsChecker.checkRole(collection, RoleType.AttributeEdit);
      permissionsChecker.checkAttributesFunctionAccess(attributes);

      final Collection bookedAttributesCollection = collectionDao.bookAttributesNum(collection.getId(), collection, attributes.size());

      int lastAttributeNum = bookedAttributesCollection.getLastAttributeNum() - attributes.size() + 1;
      final Set<RoleType> actualRoles = permissionsChecker.getActualRoles(collection);

      for (Attribute attribute : attributes) {
         attribute.setId(AttributesResource.ATTRIBUTE_PREFIX + lastAttributeNum++);
         attribute.patchCreation(actualRoles);
         bookedAttributesCollection.createAttribute(attribute);

         if (attribute.isFunctionDefined()) {
            permissionsChecker.checkFunctionsLimit(bookedAttributesCollection);
         }
      }

      mapResourceUpdateValues(bookedAttributesCollection);
      bookedAttributesCollection.setLastTimeUsed(ZonedDateTime.now());
      return collectionDao.updateCollection(collection.getId(), bookedAttributesCollection, collection);
   }

   public java.util.Collection<Attribute> createCollectionAttributesWithoutPushNotification(final String collectionId, final java.util.Collection<Attribute> attributes, boolean skipLimits) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Collection originalCollection = collection.copy();
      permissionsChecker.checkRole(collection, RoleType.AttributeEdit);
      permissionsChecker.checkAttributesFunctionAccess(attributes);

      final Set<RoleType> actualRoles = permissionsChecker.getActualRoles(collection);

      var sortedAttributes = attributes.stream().sorted((a, b) -> a.getId().compareToIgnoreCase(b.getId())).collect(Collectors.toList());
      for (Attribute attribute : sortedAttributes) {
         attribute.patchCreation(actualRoles);
         collection.createAttribute(attribute);

         if (!skipLimits && attribute.isFunctionDefined()) {
            permissionsChecker.checkFunctionsLimit(collection);
         }
      }

      collection.setLastTimeUsed(ZonedDateTime.now());
      collectionDao.updateCollection(collection.getId(), collection, originalCollection, false);

      return attributes;
   }

   public Attribute updateCollectionAttribute(final String collectionId, final String attributeId, final Attribute attribute) {
      return updateCollectionAttribute(collectionId, attributeId, attribute, false);
   }

   public Attribute updateCollectionAttribute(final String collectionId, final String attributeId, final Attribute attribute, final boolean skipFceLimits) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Attribute originalAttribute = ResourceUtils.findAttribute(collection.getAttributes(), attributeId);
      if (originalAttribute == null) {
         return attribute;
      }

      final Collection originalCollection = collection.copy();
      permissionsChecker.checkAnyRole(collection, Set.of(RoleType.AttributeEdit, RoleType.TechConfig));

      Attribute updatingAttribute = originalAttribute.copy();
      updatingAttribute.patch(attribute, permissionsChecker.getActualRoles(collection));

      if (updatingAttribute.isFunctionDefined()) {
         permissionsChecker.checkFunctionRuleAccess(updatingAttribute.getFunction().getJs(), RoleType.Read);
      } else {
         updatingAttribute.setFunction(null);
      }

      collection.updateAttribute(attributeId, updatingAttribute);
      collection.setLastTimeUsed(ZonedDateTime.now());
      mapResourceUpdateValues(collection);

      if (!skipFceLimits && updatingAttribute.functionChangedOrAdded(originalAttribute)) {
         permissionsChecker.checkFunctionsLimit(collection);
      }

      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
      conversionFacade.convertStoredDocuments(collection, originalAttribute, updatingAttribute);

      return updatingAttribute;
   }

   public void deleteCollectionAttribute(final String collectionId, final String attributeId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Collection originalCollection = collection.copy();
      permissionsChecker.checkRole(collection, RoleType.AttributeEdit);

      dataDao.deleteAttribute(collectionId, attributeId);

      collection.deleteAttribute(attributeId);
      if (collection.getDefaultAttributeId() != null && collection.getDefaultAttributeId().equals(attributeId)) {
         collection.setDefaultAttributeId(null);
      }

      final CollectionPurpose purpose = collection.getPurpose();
      if (attributeId.equals(purpose.getStateAttributeId())) {
         purpose.clearStateAttributeId();
      }
      if (attributeId.equals(purpose.getDueDateAttributeId())) {
         purpose.clearDueDateAttributeId();
      }
      if (attributeId.equals(purpose.getAssigneeAttributeId())) {
         purpose.clearAssigneeAttributeId();
      }
      if (attributeId.equals(purpose.getObserverAttributeId())) {
         purpose.clearObserverAttributeId();
         purpose.clearFinalStatesList();
      }

      collection.setLastTimeUsed(ZonedDateTime.now());
      mapResourceUpdateValues(collection);
      filterAutoLinkRulesByAttribute(collection, collectionId, attributeId);
      collectionDao.updateCollection(collection.getId(), collection, originalCollection);

      deleteAutoLinkRulesByAttribute(collectionId, attributeId);

      fileAttachmentFacade.removeAllFileAttachments(collectionId, attributeId, FileAttachment.AttachmentType.DOCUMENT);
   }

   private void filterAutoLinkRulesByAttribute(final Collection collection, final String collectionId, final String attributeId) {
      collection.setRules(collection.getRules().entrySet()
                                    .stream().filter(entry -> !CollectionUtil.containsAutoLinkRuleAttribute(entry.getValue(), collectionId, attributeId))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
   }

   private void deleteAutoLinkRulesByAttribute(final String collectionId, final String attributeId) {
      collectionDao.getAllCollections().stream()
                   .filter(collection -> !collection.getId().equals(collectionId) && CollectionUtil.containsAutoLinkRuleAttribute(collection, collectionId, attributeId))
                   .collect(Collectors.toList())
                   .forEach(collection -> {
                      final Collection originalCollection = collection.copy();
                      filterAutoLinkRulesByAttribute(collection, collectionId, attributeId);
                      mapResourceUpdateValues(collection);
                      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
                   });
   }

   public void setDefaultAttribute(final String collectionId, final String attributeId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.AttributeEdit);

      final Collection originalCollection = collection.copy();
      boolean containsAttribute = collection.getAttributes().stream()
                                            .anyMatch(attribute -> attribute.getId().equals(attributeId));
      if (containsAttribute) {
         collection.setDefaultAttributeId(attributeId);
         collectionDao.updateCollection(collection.getId(), collection, originalCollection);
      }
   }

   public Permissions getCollectionPermissions(final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.UserConfig);

      return collection.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String collectionId, final Set<Permission> userPermissions) {
      final Collection updatedCollection = collectionTreat(collectionId, collection -> {
         collection.getPermissions().updateUserPermissions(userPermissions);
         return collection;
      });

      return updatedCollection.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String collectionId, final String userId) {
      collectionTreat(collectionId, collection -> {
         collection.getPermissions().removeUserPermission(userId);
         return collection;
      });
   }

   public Set<Permission> updateGroupPermissions(final String collectionId, final Set<Permission> groupPermissions) {
      final Collection updatedCollection = collectionTreat(collectionId, collection -> {
         collection.getPermissions().updateGroupPermissions(groupPermissions);
         return collection;
      });

      return updatedCollection.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String collectionId, final String groupId) {
      collectionTreat(collectionId, collection -> {
         collection.getPermissions().removeGroupPermission(groupId);
         return collection;
      });
   }

   private Collection collectionTreat(final String collectionId, Function<Collection, Collection> handler) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Collection originalCollection = collection.copy();
      permissionsChecker.checkRole(collection, RoleType.UserConfig);
      mapResourceUpdateValues(collection);

      final Collection updatedCollection = handler.apply(collection);

      return collectionDao.updateCollection(updatedCollection.getId(), collection, originalCollection);
   }

   private void checkProjectRole(RoleType role) {
      Project project = getCurrentProject();
      permissionsChecker.checkRole(project, role);
   }

   private Project getCurrentProject() {
      if (workspaceKeeper.getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return workspaceKeeper.getProject().get();
   }

   private User getCurrentUser() {
      return authenticatedUser.getCurrentUser();
   }

   private Collection createCollectionMetadata(Collection collection) {
      if (collection.getCode() == null || collection.getCode().isEmpty()) {
         collection.setCode(generateCollectionCode(collection.getName()));
      }

      collection.setLastAttributeNum(0);
      collection.setLastTimeUsed(ZonedDateTime.now());

      Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), Collection.ROLES);
      collection.getPermissions().updateUserPermissions(defaultUserPermission);
      mapResourceCreationValues(collection);

      return collectionDao.createCollection(collection);
   }

   private String generateCollectionCode(String collectionName) {
      Set<String> existingCodes = collectionDao.getAllCollectionCodes();
      return CodeGenerator.generateHash(existingCodes, collectionName);
   }

   public void runRule(final Collection collection, final String ruleId) {
      if (adapter.getDocumentsCountByCollection(collection.getId()) > 2_000) {
         throw new UnsuccessfulOperationException("Too many documents in the source collection");
      }

      final Rule rule = collection.getRules().get(ruleId);
      if (rule != null && rule.getType() == Rule.RuleType.AUTO_LINK) {
         final AutoLinkRule autoLinkRule = new AutoLinkRule(rule);
         final String otherCollectionId = autoLinkRule.getCollection2().equals(collection.getId()) ? autoLinkRule.getCollection1() : autoLinkRule.getCollection2();
         final String attributeId = autoLinkRule.getCollection1().equals(collection.getId()) ? autoLinkRule.getAttribute1() : autoLinkRule.getAttribute2();
         final Attribute attribute = collection.getAttributes().stream().filter(a -> a.getId().equals(attributeId)).findFirst().orElse(null);
         final Collection otherCollection = getCollection(otherCollectionId);
         final String otherAttributeId = autoLinkRule.getCollection2().equals(collection.getId()) ? autoLinkRule.getAttribute1() : autoLinkRule.getAttribute2();
         final Attribute otherAttribute = otherCollection.getAttributes().stream().filter(a -> a.getId().equals(otherAttributeId)).findFirst().orElse(null);
         final Map<String, AllowedPermissions> permissions = permissionsChecker.getCollectionsPermissions(List.of(collection, otherCollection));

         if (adapter.getDocumentsCountByCollection(otherCollectionId) > 10_000) {
            throw new UnsuccessfulOperationException("Too many documents in the target collection");
         }

         final LinkType linkType = linkTypeDao.getLinkType(autoLinkRule.getLinkType());

         final AutoLinkBatchTask task = taskFactory.getInstance(AutoLinkBatchTask.class);
         task.setupBatch(autoLinkRule, linkType, collection, attribute, otherCollection, otherAttribute, getCurrentUser(), permissions);

         taskExecutor.submitTask(task);
      } else if (rule != null && rule.getType() == Rule.RuleType.CRON) {
         final CronRule cronRule = new CronRule(rule);

         List<Document> documents = new ArrayList<>();
         if (cronRule.getViewId() != null) {
            try {
               final View view = viewDao.getViewById(cronRule.getViewId());
               final User user = AuthenticatedUser.getMachineUser();
               final AllowedPermissions allowedPermissions = AllowedPermissions.allAllowed();

               documents = DocumentUtils.getDocuments(collectionDao, documentDao, dataDao, userDao, groupDao, selectionListDao, getOrganization(), getProject(), view.getQuery(), user, cronRule.getLanguage(), allowedPermissions, null);
               documents = documents.stream().filter(document -> document.getCollectionId().equals(collection.getId())).collect(Collectors.toList());
            } catch (ResourceNotFoundException ignore) {

            }
         }

         RuleTask task = taskFactory.getInstance(RuleTask.class);
         task.setRule(rule.getName(), rule, collection, documents);

         taskExecutor.submitTask(task);
      }
   }
}
