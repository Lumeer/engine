package io.lumeer.core.facade;

import io.lumeer.api.model.AttributeFilter;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.OldQuery;
import io.lumeer.api.model.OldView;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.View;
import io.lumeer.core.util.FilterParser;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OldViewDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ViewConvertFacade extends AbstractFacade {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private OldViewDao oldViewDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private DocumentDao documentDao;

   public void convertViews() {
      List<Organization> organizations = organizationDao.getAllOrganizations();
      organizations.forEach(this::convertViewsForOrganization);
   }

   private void convertViewsForOrganization(Organization organization) {
      projectDao.setOrganization(organization);
      workspaceKeeper.setOrganization(organization.getCode());
      List<Project> projects = projectDao.getAllProjects();
      projects.forEach(this::convertViewsForProject);
   }

   private void convertViewsForProject(Project project) {
      setProject(project);
      List<OldView> oldViews = oldViewDao.getOldViews().stream().filter(view -> view.getQuery() != null).collect(Collectors.toList());
      oldViews.forEach(this::convertView);
   }

   private void setProject(Project project) {
      viewDao.setProject(project);
      oldViewDao.setProject(project);
      collectionDao.setProject(project);
      linkTypeDao.setProject(project);
      documentDao.setProject(project);
      workspaceKeeper.setProject(project.getCode());
   }

   private void convertView(OldView oldView) {
      OldQuery oldQuery = oldView.getQuery();
      List<AttributeFilter> filters = oldQuery.getFilters() != null ? oldQuery.getFilters().stream().map(FilterParser::parse).collect(Collectors.toList()) : Collections.emptyList();

      List<Document> documents = oldQuery.getDocumentIds() != null ? documentDao.getDocumentsByIds(oldQuery.getDocumentIds().toArray(new String[0])) : Collections.emptyList();
      Map<String, Document> documentMap = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));

      List<LinkType> linkTypes = oldQuery.getLinkTypeIds() != null ? linkTypeDao.getLinkTypesByIds(oldQuery.getLinkTypeIds()) : Collections.emptyList();
      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, Function.identity()));

      Set<String> collectionIds = oldQuery.getCollectionIds() != null ? oldQuery.getCollectionIds() : new HashSet<>();
      Set<String> allCollectionIds = new HashSet<>(collectionIds);
      allCollectionIds.addAll(documents.stream().map(Document::getCollectionId).collect(Collectors.toSet()));
      allCollectionIds.addAll(linkTypes.stream().map(LinkType::getCollectionIds).flatMap(java.util.Collection::stream).collect(Collectors.toSet()));
      allCollectionIds.addAll(filters.stream().map(AttributeFilter::getCollectionId).collect(Collectors.toSet()));

      Set<String> existingCollectionsIds = collectionDao.getCollectionsByIds(allCollectionIds).stream().map(Collection::getId).collect(Collectors.toSet());

      List<QueryStem> stems = convertOldQueryDataToStems(collectionIds, existingCollectionsIds, linkTypesMap, documentMap, filters);
      Set<String> fulltexts = oldQuery.getFulltext() != null ? Collections.singleton(oldQuery.getFulltext()) : Collections.emptySet();
      Query query = new Query(stems, fulltexts, oldQuery.getPage(), oldQuery.getPageSize());
      View view = new View(oldView.getCode(), oldView.getName(), oldView.getIcon(), oldView.getColor(), oldView.getDescription(), oldView.getPermissions(), query, oldView.getPerspective(), oldView.getConfig(), oldView.getAuthorId());
      viewDao.updateView(oldView.getId(), view);
   }

   private List<QueryStem> convertOldQueryDataToStems(Set<String> collectionIds, Set<String> existingCollectionsIds, Map<String, LinkType> linkTypesMap, Map<String, Document> documentMap, List<AttributeFilter> filters) {
      List<QueryStem> stems = new ArrayList<>();
      for (String collectionId : collectionIds) {
         if (!existingCollectionsIds.contains(collectionId)) {
            continue;
         }
         List<LinkType> linkTypesForStem = findLongestLinkTypeChainForCollection(collectionId, linkTypesMap.values(), existingCollectionsIds);
         linkTypesForStem.forEach(lt -> linkTypesMap.remove(lt.getId()));
         Set<String> linkTypesCollectionsIds = linkTypesForStem.stream().map(LinkType::getCollectionIds).flatMap(java.util.Collection::stream).collect(Collectors.toSet());
         linkTypesCollectionsIds.add(collectionId);

         List<Document> documentsForStem = documentMap.values().stream().filter(document -> linkTypesCollectionsIds.contains(document.getCollectionId())).collect(Collectors.toList());
         documentsForStem.forEach(document -> documentMap.remove(document.getId()));

         Set<AttributeFilter> filtersForStem = filters.stream().filter(filter -> linkTypesCollectionsIds.contains(filter.getCollectionId())).collect(Collectors.toSet());
         filtersForStem.forEach(filters::remove);

         List<String> linkTypeIds = linkTypesForStem.stream().map(LinkType::getId).collect(Collectors.toList());
         Set<String> documentIds = documentsForStem.stream().map(Document::getId).collect(Collectors.toSet());
         QueryStem stem = new QueryStem(collectionId, linkTypeIds, documentIds, filtersForStem);
         stems.add(stem);
      }
      return stems;
   }

   private List<LinkType> findLongestLinkTypeChainForCollection(String collectionId, java.util.Collection<LinkType> linkTypes, Set<String> existingCollectionsIds) {
      List<LinkType> chain = new LinkedList<>();
      String lastCollectionId = collectionId;
      for (int i = 0; i < linkTypes.size(); i++) {
         final String finalLastCollectionId = lastCollectionId;
         Optional<LinkType> optionalLinkType = linkTypes.stream().filter(lt -> lt.getCollectionIds().contains(finalLastCollectionId)).findFirst();
         if (!optionalLinkType.isPresent()) {
            return chain;
         }

         LinkType linkType = optionalLinkType.get();

         int collectionIdIndex = linkType.getCollectionIds().get(0).equals(lastCollectionId) ? 1 : 0;
         String currentCollectionId = linkType.getCollectionIds().get(collectionIdIndex);

         if (!existingCollectionsIds.contains(currentCollectionId)) {
            continue;
         }
         chain.add(linkType);
         lastCollectionId = currentCollectionId;
         linkTypes.remove(linkType);
      }
      return chain;
   }

}
