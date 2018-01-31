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
package io.lumeer.core.cache;

import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.storage.api.dao.UserDao;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UserCache {

   @Inject
   private CacheFactory cacheFactory;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   private Cache<User> userCache;

   @PostConstruct
   public void initCache() {
      userCache = cacheFactory.getCache();
   }

   public User getUser(String email) {
      return userCache.computeIfAbsent(email, this::getOrCreateUser);
   }

   private User getOrCreateUser(String email) {
      String organizationId = workspaceKeeper.getOrganization().get().getId(); // TODO how to be sure, that organization is set?
      Optional<User> userOptional = userDao.getUserByEmail(organizationId, email);
      if (userOptional.isPresent()) {
         return userOptional.get();
      }

      User user = new User(email);
      return userDao.createUser(organizationId, null, user); // TODO remove this for production
   }

   public void updateUser(String username, User user) {
      userCache.set(username, user);
   }

   public void remoteUser(String username) {
      userCache.remove(username);
   }

   public void clear() {
      userCache.clear();
   }

}
