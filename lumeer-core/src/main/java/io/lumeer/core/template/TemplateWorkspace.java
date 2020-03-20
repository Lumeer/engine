package io.lumeer.core.template;

import java.util.Calendar;
import java.util.Date;

public class TemplateWorkspace {

   public static TemplateData getTemplateData(TemplateType templateType, String language) {
      return new TemplateData("XXX", "SUPPL", new Date(2020, Calendar.JANUARY, 1));
   }

}
