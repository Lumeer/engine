/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.core.cache;

import io.lumeer.api.model.User;
import io.lumeer.core.model.SimpleUser;
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

   private Cache<User> userCache;

   @PostConstruct
   public void initCache() {
      userCache = cacheFactory.getCache();
   }

   public User getUser(String username) {
      return userCache.computeIfAbsent(username, this::getOrCreateUser);
   }

   private User getOrCreateUser(String username) {
      Optional<User> userOptional = userDao.getUserByUsername(username);
      if (userOptional.isPresent()) {
         return userOptional.get();
      }

      User user = new SimpleUser(username);
      return userDao.createUser(user); // TODO remove this for production
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
