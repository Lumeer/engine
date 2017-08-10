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
package io.lumeer.api.model;

import java.util.Set;

public interface Permissions {

   Set<Permission> getUserPermissions();

   void updateUserPermissions(Permission... userPermissions);

   void removeUserPermission(String user);

   Set<Permission> getGroupPermissions();

   void updateGroupPermissions(Permission... groupPermissions);

   void removeGroupPermission(String group);

   void clear();

}
