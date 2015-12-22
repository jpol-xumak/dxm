/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.client.widget.toolbar;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.data.toolbar.GWTEditConfiguration;
import org.jahia.ajax.gwt.client.data.toolbar.GWTJahiaToolbar;
import org.jahia.ajax.gwt.client.data.toolbar.GWTJahiaToolbarItem;
import org.jahia.ajax.gwt.client.widget.Linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action toolbar container widget.
 * User: ktlili
 * Date: Sep 4, 2009
 * Time: 4:17:57 PM
 */
public class ActionToolbarLayoutContainer extends LayoutContainer {
    private List<ActionToolbar> actionToolbars = new ArrayList<ActionToolbar>();
    private Linker linker;
    private List<GWTJahiaToolbar> toolbarSet;

    public ActionToolbarLayoutContainer(GWTJahiaToolbar toolbar) {
        super();
        this.toolbarSet = new ArrayList<GWTJahiaToolbar>();
        this.toolbarSet.add(toolbar);
        setLayout(new RowLayout());
    }

    public ActionToolbarLayoutContainer(List<GWTJahiaToolbar> toolbarSet) {
        super();
        setLayout(new RowLayout());
        this.toolbarSet = toolbarSet;
    }

    public void setToolbarSet(List<GWTJahiaToolbar> toolbarSet) {
        removeAll();
        this.toolbarSet = toolbarSet;
        this.actionToolbars = new ArrayList<ActionToolbar>();
    }

    /**
     * Create Toolbar UI
     *
     */
    private void createToolbarUI(List<GWTJahiaToolbar> toolbarList) {
        if (toolbarList != null && !toolbarList.isEmpty()) {
            Log.debug(toolbarList.size() + " toolbar(s).");
            for (int i = 0; i < toolbarList.size(); i++) {
                GWTJahiaToolbar gwtToolbar = toolbarList.get(i);
                List<GWTJahiaToolbarItem> toolbarItemsGroups = gwtToolbar.getGwtToolbarItems();
                if (toolbarItemsGroups != null && !toolbarItemsGroups.isEmpty()) {
                    addActionToolbar(gwtToolbar, i==0);
                }
            }

            Log.debug("-- all tool bars added.");
        } else {
            Log.debug("There is no toolbar");
        }

        // no node is selected
        handleNewLinkerSelection();
    }

    /**
     * Add a toolbar widget
     *
     * @param gwtToolbar
     */
    public void addActionToolbar(GWTJahiaToolbar gwtToolbar, boolean first) {
        ActionToolbar actionToolbar = new ActionToolbar(gwtToolbar, linker);
        actionToolbar.createToolBar();
        if (first) {
            actionToolbar.addStyleName("x-toolbar-first");
        }
        // add to widget
        add(actionToolbar);
        layout();
        // add to toolbars list
        actionToolbars.add(actionToolbar);

    }

    /**
     * Handle module selection
     */
    public void handleNewLinkerSelection() {
        for (ActionToolbar actionToolbar : actionToolbars) {
            actionToolbar.handleNewLinkerSelection();
        }
    }

    public void handleNewMainNodeLoaded(GWTJahiaNode node) {
        for (ActionToolbar actionToolbar : actionToolbars) {
            actionToolbar.handleNewMainNodeLoaded(node);
        }
    }

    public void initWithLinker(Linker linker) {
        this.linker = linker;
        if (toolbarSet != null) {
            createToolbarUI(toolbarSet);
        }
    }

    public void setConfig(GWTEditConfiguration config) {
        setToolbarSet(Arrays.asList(config.getTopToolbar()));
    }

}
