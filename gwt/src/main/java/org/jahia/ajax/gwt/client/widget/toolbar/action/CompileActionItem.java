/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.ajax.gwt.client.widget.toolbar.action;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.google.gwt.user.client.ui.HTML;

import org.jahia.ajax.gwt.client.core.BaseAsyncCallback;
import org.jahia.ajax.gwt.client.core.JahiaGWTParameters;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;

/**
 * Action item to create a new templates set
 */
@SuppressWarnings("serial")
public class CompileActionItem extends BaseActionItem {

    @Override public void onComponentSelection() {
        linker.loading(Messages.get("label.compilingModule", "Compile module..."));
        JahiaContentManagementService.App.getInstance().compileAndDeploy(JahiaGWTParameters.getSiteKey(), new BaseAsyncCallback<Object>() {
            public void onSuccess(Object result) {
                linker.loaded();
                Info.display(Messages.get("label.information", "Information"), Messages.get("message.moduleCompiled", "Module compiled"));
            }

            public void onApplicationFailure(Throwable caught) {
                linker.loaded();
                Info.display(Messages.get("label.error", "Error"), Messages.get("message.moduleCompilationFailed", "Module compilation failed"));
                StringBuilder sb = new StringBuilder();
                for (String line : caught.getMessage().split("\n")) {
                    if (line .startsWith("[ERROR]")) {
                        sb.append("<span style=\"color:red\">");
                    }
                    sb.append(line);
                    sb.append("</br>");
                    if (line .startsWith("[ERROR]")) {
                        sb.append("</span>");
                    }
                }
                final Dialog dl = new Dialog();
                dl.setModal(true);
                dl.setHeadingHtml(Messages.get("label.error", "Error"));
                dl.setHideOnButtonClick(true);
                dl.setLayout(new FlowLayout());
                dl.setWidth(500);
                dl.setScrollMode(Style.Scroll.AUTO);
                dl.add(new HTML(sb.toString()));
                dl.setHeight(500);
                dl.show();
            }
        });
    }

    @Override
    public void handleNewLinkerSelection() {
        GWTJahiaNode siteNode = JahiaGWTParameters.getSiteNode();
        if (siteNode.get("j:sourcesFolder") != null) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

}