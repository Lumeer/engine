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
package io.lumeer.core.constraint;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.ConstraintType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoneToColorConverter extends AbstractTranslatingConverter {

   @Override
   @SuppressWarnings("unchecked")
   void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      this.ignoreMissing = true;

      if (isConstraintWithConfig(toAttribute)) {
         translations.put("black", "#000000");
         translations.put("silver", "#c0c0c0");
         translations.put("gray", "#808080");
         translations.put("white", "#ffffff");
         translations.put("maroon", "#800000");
         translations.put("red", "#ff0000");
         translations.put("purple", "#800080");
         translations.put("fuchsia", "#ff00ff");
         translations.put("green", "#008000");
         translations.put("lime", "#00ff00");
         translations.put("olive", "#808000");
         translations.put("yellow", "#ffff00");
         translations.put("navy", "#000080");
         translations.put("blue", "#0000ff");
         translations.put("teal", "#008080");
         translations.put("orange", "#ffa500");
         translations.put("aliceblue", "#f0f8ff");
         translations.put("antiquewhite", "#faebd7");
         translations.put("aquamarine", "#7fffd4");
         translations.put("azure", "#f0ffff");
         translations.put("beige", "#f5f5dc");
         translations.put("bisque", "#ffe4c4");
         translations.put("blanchedalmond", "#ffebcd");
         translations.put("blueviolet", "#8a2be2");
         translations.put("brown", "#a52a2a");
         translations.put("burlywood", "#deb887");
         translations.put("cadetblue", "#5f9ea0");
         translations.put("chartreuse", "#7fff00");
         translations.put("chocolate", "#d2691e");
         translations.put("coral", "#ff7f50");
         translations.put("cornflowerblue", "#6495ed");
         translations.put("cornsilk", "#fff8dc");
         translations.put("crimson", "#dc143c");
         translations.put("cyan", "#00ffff");
         translations.put("aqua", "#00ffff");
         translations.put("darkblue", "#00008b");
         translations.put("darkcyan", "#008b8b");
         translations.put("darkgoldenrod", "#b8860b");
         translations.put("darkgray", "#a9a9a9");
         translations.put("darkgreen", "#006400");
         translations.put("darkgrey", "#a9a9a9");
         translations.put("darkkhaki", "#bdb76b");
         translations.put("darkmagenta", "#8b008b");
         translations.put("darkolivegreen", "#556b2f");
         translations.put("darkorange", "#ff8c00");
         translations.put("darkorchid", "#9932cc");
         translations.put("darkred", "#8b0000");
         translations.put("darksalmon", "#e9967a");
         translations.put("darkseagreen", "#8fbc8f");
         translations.put("darkslateblue", "#483d8b");
         translations.put("darkslategray", "#2f4f4f");
         translations.put("darkslategrey", "#2f4f4f");
         translations.put("darkturquoise", "#00ced1");
         translations.put("darkviolet", "#9400d3");
         translations.put("deeppink", "#ff1493");
         translations.put("deepskyblue", "#00bfff");
         translations.put("dimgray", "#696969");
         translations.put("dimgrey", "#696969");
         translations.put("dodgerblue", "#1e90ff");
         translations.put("firebrick", "#b22222");
         translations.put("floralwhite", "#fffaf0");
         translations.put("forestgreen", "#228b22");
         translations.put("gainsboro", "#dcdcdc");
         translations.put("ghostwhite", "#f8f8ff");
         translations.put("gold", "#ffd700");
         translations.put("goldenrod", "#daa520");
         translations.put("greenyellow", "#adff2f");
         translations.put("grey", "#808080");
         translations.put("honeydew", "#f0fff0");
         translations.put("hotpink", "#ff69b4");
         translations.put("indianred", "#cd5c5c");
         translations.put("indigo", "#4b0082");
         translations.put("ivory", "#fffff0");
         translations.put("khaki", "#f0e68c");
         translations.put("lavender", "#e6e6fa");
         translations.put("lavenderblush", "#fff0f5");
         translations.put("lawngreen", "#7cfc00");
         translations.put("lemonchiffon", "#fffacd");
         translations.put("lightblue", "#add8e6");
         translations.put("lightcoral", "#f08080");
         translations.put("lightcyan", "#e0ffff");
         translations.put("lightgoldenrodyellow", "#fafad2");
         translations.put("lightgray", "#d3d3d3");
         translations.put("lightgreen", "#90ee90");
         translations.put("lightgrey", "#d3d3d3");
         translations.put("lightpink", "#ffb6c1");
         translations.put("lightsalmon", "#ffa07a");
         translations.put("lightseagreen", "#20b2aa");
         translations.put("lightskyblue", "#87cefa");
         translations.put("lightslategray", "#778899");
         translations.put("lightslategrey", "#778899");
         translations.put("lightsteelblue", "#b0c4de");
         translations.put("lightyellow", "#ffffe0");
         translations.put("limegreen", "#32cd32");
         translations.put("linen", "#faf0e6");
         translations.put("magenta", "#ff00ff");
         translations.put("mediumaquamarine", "#66cdaa");
         translations.put("mediumblue", "#0000cd");
         translations.put("mediumorchid", "#ba55d3");
         translations.put("mediumpurple", "#9370db");
         translations.put("mediumseagreen", "#3cb371");
         translations.put("mediumslateblue", "#7b68ee");
         translations.put("mediumspringgreen", "#00fa9a");
         translations.put("mediumturquoise", "#48d1cc");
         translations.put("mediumvioletred", "#c71585");
         translations.put("midnightblue", "#191970");
         translations.put("mintcream", "#f5fffa");
         translations.put("mistyrose", "#ffe4e1");
         translations.put("moccasin", "#ffe4b5");
         translations.put("navajowhite", "#ffdead");
         translations.put("oldlace", "#fdf5e6");
         translations.put("olivedrab", "#6b8e23");
         translations.put("orangered", "#ff4500");
         translations.put("orchid", "#da70d6");
         translations.put("palegoldenrod", "#eee8aa");
         translations.put("palegreen", "#98fb98");
         translations.put("paleturquoise", "#afeeee");
         translations.put("palevioletred", "#db7093");
         translations.put("papayawhip", "#ffefd5");
         translations.put("peachpuff", "#ffdab9");
         translations.put("peru", "#cd853f");
         translations.put("pink", "#ffc0cb");
         translations.put("plum", "#dda0dd");
         translations.put("powderblue", "#b0e0e6");
         translations.put("rosybrown", "#bc8f8f");
         translations.put("royalblue", "#4169e1");
         translations.put("saddlebrown", "#8b4513");
         translations.put("salmon", "#fa8072");
         translations.put("sandybrown", "#f4a460");
         translations.put("seagreen", "#2e8b57");
         translations.put("seashell", "#fff5ee");
         translations.put("sienna", "#a0522d");
         translations.put("skyblue", "#87ceeb");
         translations.put("slateblue", "#6a5acd");
         translations.put("slategray", "#708090");
         translations.put("slategrey", "#708090");
         translations.put("snow", "#fffafa");
         translations.put("springgreen", "#00ff7f");
         translations.put("steelblue", "#4682b4");
         translations.put("tan", "#d2b48c");
         translations.put("thistle", "#d8bfd8");
         translations.put("tomato", "#ff6347");
         translations.put("turquoise", "#40e0d0");
         translations.put("violet", "#ee82ee");
         translations.put("wheat", "#f5deb3");
         translations.put("whitesmoke", "#f5f5f5");
         translations.put("yellowgreen", "#9acd32");
         translations.put("rebeccapurple", "#663399");
      }
   }

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.None);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.Color);
   }
}
