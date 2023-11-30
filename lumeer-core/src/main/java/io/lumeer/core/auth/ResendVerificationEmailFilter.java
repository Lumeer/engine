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
package io.lumeer.core.auth;

import io.lumeer.api.model.User;
import io.lumeer.core.facade.EventLogFacade;

import com.auth0.exception.Auth0Exception;

import java.io.IOException;
import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SessionScoped
public class ResendVerificationEmailFilter implements AuthFilter, Serializable {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private EventLogFacade eventLogFacade;

   @Inject
   private UserAuth0Utils userAuth0Utils;

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {
   }

   @Override
   public FilterResult doFilter(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
      if (req.getPathInfo() != null && req.getPathInfo().startsWith("/users/current/resend")) {
         logResendEvent(authenticatedUser);

         if (authenticatedUser != null && authenticatedUser.getAuthUserInfo() != null) {
            if (authenticatedUser.getAuthUserInfo().user != null &&
               !authenticatedUser.getAuthUserInfo().user.isEmailVerified() && authenticatedUser.getAuthUserInfo().user.getAuthIds() != null &&
               authenticatedUser.getAuthUserInfo().user.getAuthIds().size() > 0) {
               final String authId = authenticatedUser.getAuthUserInfo().user.getAuthIds().iterator().next();

               try {
                  userAuth0Utils.resendVerificationEmail(authId);
               } catch (Auth0Exception ae) {
                  res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ae.getLocalizedMessage());
                  return FilterResult.BREAK;
               }

               res.setStatus(200);
               return FilterResult.BREAK;
            }

            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return FilterResult.BREAK;
         }
      }

      return FilterResult.CONTINUE;
   }

   private void logResendEvent(final AuthenticatedUser authenticatedUser) {
      final User user =
            authenticatedUser == null || authenticatedUser.getAuthUserInfo() == null || authenticatedUser.getAuthUserInfo().user == null ?
            null : authenticatedUser.getCurrentUser();

      eventLogFacade.logEvent(user, "Requested resend of verification email.");
   }
}
