/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.render.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;

/**
 * A render filter for forcing the usage of the UI (user preferred) locale in templates, by just setting a request attribute with the locale
 * to use.
 * 
 * @author Sergiy Shyrkov
 */
public class ForceUILocaleFilter extends AbstractFilter {

    public static final String RENDERING_FORCED_LOCALE = "org.jahia.rendering.forcedLocale";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String prepare(RenderContext context, Resource resource, RenderChain chain)
            throws Exception {
        Locale forcedLocale = (Locale) context.getRequest().getAttribute(RENDERING_FORCED_LOCALE);
        if (forcedLocale == null) {
            forcedLocale = context.getUILocale();
            if (!forcedLocale.equals(resource.getLocale())) {
                chain.pushAttribute(context.getRequest(), RENDERING_FORCED_LOCALE, forcedLocale);
            } else {
                forcedLocale = null;
            }
        }
        if (context.isLiveMode() && forcedLocale != null) {
            List l = (List) context.getRequest().getAttribute("module.cache.additional.key");
            if (l == null) {
                l = new LinkedList();
                context.getRequest().setAttribute("module.cache.additional.key", l);
            }

            l.add(forcedLocale.toString());
        }

        return null;
    }
}