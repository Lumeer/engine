package io.lumeer.core.facade;

import io.lumeer.api.model.User;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RunWith(Arquillian.class)
public class FreshdeskFacadeIT extends IntegrationTestBase {

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Test
   @Ignore
   public void testFreshdeskTicket() {
      final User u = new User("123", "Alan Turing", "aturing@lumeer.io", null, null, false, null, true, false);
      freshdeskFacade.logTicket(u, "Tady je uživatel", "Který zalogoval ticket přímo z aplikace \" ' !@#$%^&*() \n abc : \"s");
   }

}