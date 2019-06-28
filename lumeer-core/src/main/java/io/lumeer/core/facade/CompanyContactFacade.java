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

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class CompanyContactFacade extends AbstractFacade {

   @Inject
   private CompanyContactDao companyContactDao;

   public CompanyContact getCompanyContact(final Organization organization) {
      checkManagePermissions(organization);

      final CompanyContact companyContact = companyContactDao.getCompanyContact(organization);

      if (companyContact == null) {
         return new CompanyContact(null, organization.getId(), "", "", "", "",
               "", "", "", "", "", "", "", "", "");
      }

      return companyContact;
   }

   public CompanyContact setCompanyContact(final Organization organization, final CompanyContact companyContact) {
      checkManagePermissions(organization);

      return companyContactDao.setCompanyContact(organization, companyContact);
   }

   private void checkManagePermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, Role.MANAGE);
   }
}
