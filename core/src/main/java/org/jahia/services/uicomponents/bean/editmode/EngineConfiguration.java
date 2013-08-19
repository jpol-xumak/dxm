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

package org.jahia.services.uicomponents.bean.editmode;

import org.jahia.ajax.gwt.client.widget.contentengine.ButtonItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Core part of engine configuration
 * see GWTEngineConfiguration for gwt part
 * see {@link org.jahia.ajax.gwt.helper.UIConfigHelper} for link
 */

public class EngineConfiguration implements Serializable {

    private static final long serialVersionUID = -5991528610464460659L;

    private List<EngineTab> engineTabs;

    private List<ButtonItem> creationButtons = new ArrayList<ButtonItem>();
    private List<ButtonItem> editionButtons = new ArrayList<ButtonItem>();
    private List<ButtonItem> commonButtons = new ArrayList<ButtonItem>();

    /***
     * @return list of engine tabs
     */
    public List<EngineTab> getEngineTabs() {
        return engineTabs;
    }

    public void setEngineTabs(List<EngineTab> engineTabs) {
        this.engineTabs = engineTabs;
    }

    /**
     * @return List of button to display for create engine
     */
    public List<ButtonItem> getCreationButtons() {
        return creationButtons;
    }

    public void setCreationButtons(List<ButtonItem> creationButtons) {
        this.creationButtons = creationButtons;
    }

    /**
     * @return List of ButtonItem for edit engine
     */
    public List<ButtonItem> getEditionButtons() {
        return editionButtons;
    }

    public void setEditionButtons(List<ButtonItem> editionButtons) {
        this.editionButtons = editionButtons;
    }

    /**
     * @return List of button for both create and edit (like cancel)
     */
    public List<ButtonItem> getCommonButtons() {
        return commonButtons;
    }

    public void setCommonButtons(List<ButtonItem> commonButtons) {
        this.commonButtons = commonButtons;
    }

}