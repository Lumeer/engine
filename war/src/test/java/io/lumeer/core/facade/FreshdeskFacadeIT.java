package io.lumeer.core.facade;

import io.lumeer.api.model.User;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class FreshdeskFacadeIT extends IntegrationTestBase {

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Test
   @Disabled
   public void testFreshdeskTicket() {
      final User u = new User("123", "Alan Turing", "aturing@lumeer.io", null, null, false, null, true, false, null, null, null);
      freshdeskFacade.logTicket(u, "Tady je uživatel", "Který zalogoval ticket přímo z aplikace \" ' !@#$%^&*() \n abc : \"s");
   }

}
