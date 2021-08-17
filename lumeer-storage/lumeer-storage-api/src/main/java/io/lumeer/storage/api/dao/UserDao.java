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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.User;

import java.util.List;
import java.util.Set;

public interface UserDao {

   User createUser(User user);

   User updateUser(String userId, User user);

   User updateUserTimeZone(final String id, final String timeZone);

   void deleteUser(String userId);

   User getUserByEmail(String email);

   List<User> getUsersByEmails(Set<String> email);

   User getUserByAuthId(String authId);

   User getUserById(String id);

   List<User> getUserByIds(Set<String> ids);

   List<User> getAllUsers(String organizationId);

   long getReferralsCount(final String referral);

   long getAllUsersCount(String organizationId);
}
