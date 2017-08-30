package io.lumeer.storage.mongodb.model.embedded;

import io.lumeer.api.model.Query;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Embedded
public class MorphiaQuery implements Query {

   public static final String COLLECTION_CODES = "collections";
   public static final String FILTERS = "filters";
   public static final String FULLTEXT = "fulltext";
   public static final String PAGE = "page";
   public static final String PAGE_SIZE = "pageSize";

   @Property(COLLECTION_CODES)
   private Set<String> collectionCodes = Collections.emptySet();

   @Property(FILTERS)
   private Set<String> filters = Collections.emptySet();

   @Property(FULLTEXT)
   private String fulltext;

   @Property(PAGE)
   private Integer page;

   @Property(PAGE_SIZE)
   private Integer pageSize;

   public MorphiaQuery() {
   }

   public MorphiaQuery(Query query) {
      collectionCodes = new HashSet<>(query.getCollectionCodes());
      filters = new HashSet<>(query.getFilters());
      fulltext = query.getFulltext();
      page = query.getPage();
      pageSize = query.getPageSize();
   }

   @Override
   public Set<String> getCollectionCodes() {
      return collectionCodes;
   }

   @Override
   public Set<String> getFilters() {
      return filters;
   }

   @Override
   public String getFulltext() {
      return fulltext;
   }

   @Override
   public Integer getPage() {
      return page;
   }

   @Override
   public Integer getPageSize() {
      return pageSize;
   }

   public void setCollectionCodes(final Set<String> collectionCodes) {
      this.collectionCodes = collectionCodes;
   }

   public void setFilters(final Set<String> filters) {
      this.filters = filters;
   }

   public void setFulltext(final String fulltext) {
      this.fulltext = fulltext;
   }

   public void setPage(final Integer page) {
      this.page = page;
   }

   public void setPageSize(final Integer pageSize) {
      this.pageSize = pageSize;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Query)) {
         return false;
      }

      final Query that = (Query) o;

      if (getCollectionCodes() != null ? !getCollectionCodes().equals(that.getCollectionCodes()) : that.getCollectionCodes() != null) {
         return false;
      }
      if (getFilters() != null ? !getFilters().equals(that.getFilters()) : that.getFilters() != null) {
         return false;
      }
      if (getFulltext() != null ? !getFulltext().equals(that.getFulltext()) : that.getFulltext() != null) {
         return false;
      }
      if (getPage() != null ? !getPage().equals(that.getPage()) : that.getPage() != null) {
         return false;
      }
      return getPageSize() != null ? getPageSize().equals(that.getPageSize()) : that.getPageSize() == null;
   }

   @Override
   public int hashCode() {
      int result = getCollectionCodes() != null ? getCollectionCodes().hashCode() : 0;
      result = 31 * result + (getFilters() != null ? getFilters().hashCode() : 0);
      result = 31 * result + (getFulltext() != null ? getFulltext().hashCode() : 0);
      result = 31 * result + (getPage() != null ? getPage().hashCode() : 0);
      result = 31 * result + (getPageSize() != null ? getPageSize().hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "MongoQuery{" +
            "collectionCodes=" + collectionCodes +
            ", filters=" + filters +
            ", fulltext='" + fulltext + '\'' +
            ", page=" + page +
            ", pageSize=" + pageSize +
            '}';
   }
}
