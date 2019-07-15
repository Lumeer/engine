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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.FileAttachment;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.remote.rest.ServiceIntegrationTestBase;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

@RunWith(Arquillian.class)
public class FileAttachmentFacadeIT extends ServiceIntegrationTestBase {

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   private static FileAttachment fileAttachment1 = new FileAttachment("abc123", "def456", "ghi789", "jkl0ab", "a3", "můk super/$~@#ę€%=název.pdf");
   private static FileAttachment fileAttachment2 = new FileAttachment("abc123", "zxc456", "ghi789", "jkl0ab", "a3", "normal file name .doc");

   @Test
   public void testFileUpload() {
      // TODO setup workspace

      final FileAttachment createdFileAttachment = fileAttachmentFacade.createFileAttachment(fileAttachment1);

      assertThat(createdFileAttachment.getPresignedUrl()).isNotEmpty();

      final Entity entity = Entity.text("Hello world text file");
      client.target(createdFileAttachment.getPresignedUrl()).request(MediaType.APPLICATION_JSON).buildPut(entity).invoke();

      var result = fileAttachmentFacade.getAllFileAttachments(createdFileAttachment.getCollectionId());
      assertThat(result).containsExactly(createdFileAttachment);

      var listing = fileAttachmentFacade.listFileAttachments(createdFileAttachment.getCollectionId(), createdFileAttachment.getDocumentId(), createdFileAttachment.getAttributeId());
      assertThat(listing).containsExactly(createdFileAttachment);
      assertThat(listing.get(0).getSize()).isGreaterThan(0L);
   }
}
