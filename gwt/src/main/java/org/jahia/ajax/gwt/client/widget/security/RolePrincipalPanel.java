package org.jahia.ajax.gwt.client.widget.security;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.core.XTemplate;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jahia.ajax.gwt.client.core.BaseAsyncCallback;
import org.jahia.ajax.gwt.client.data.GWTJahiaPermission;
import org.jahia.ajax.gwt.client.data.GWTJahiaRole;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementServiceAsync;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ktlili
 * Date: Feb 2, 2010
 * Time: 2:51:36 PM
 */
public class RolePrincipalPanel extends LayoutContainer {
    private List<GWTJahiaRole> roles = new ArrayList<GWTJahiaRole>();
    private String principalKey;
    private String siteKey;
    private boolean isGroup;
    private final JahiaContentManagementServiceAsync contentService = JahiaContentManagementService.App.getInstance();

    public RolePrincipalPanel(String siteKey, boolean isGroup, String principalKey) {
        this.siteKey = siteKey != null && siteKey.length() > 0 ? siteKey : null;
        this.isGroup = isGroup;
        this.principalKey = principalKey;
    }

    public RolePrincipalPanel() {
    }

    @Override
    protected void onRender(Element parent, int index) {
        super.onRender(parent, index);
        //setLayout(new FitLayout());
        Log.debug("Get role for site: " + siteKey);
        JahiaContentManagementService.App.getInstance().getRoles(siteKey, isGroup, principalKey, new BaseAsyncCallback<List<GWTJahiaRole>>() {
            public void onSuccess(List<GWTJahiaRole> gwtRoles) {
                roles = gwtRoles;
                updateUI();
            }

            public void onApplicationFailure(Throwable throwable) {
                Log.error("Error while retrieving roles", throwable);
            }
        });


    }

    /**
     * Update uui
     */
    private void updateUI() {
        final ListStore<GWTJahiaRole> store = new ListStore<GWTJahiaRole>();
        // prepare data
        for (GWTJahiaRole r : roles) {
            // prepare data
            List<GWTJahiaPermission> permissions = r.getPermissions();
            String permLabels = "";
            for (int j = 0; j < permissions.size(); j++) {
                permLabels += permissions.get(j).getName();
                if (j < permissions.size()) {
                    permLabels += "<br/>";
                }
            }
            r.set("permLabels", permLabels);

        }


        store.add(roles);
        final XTemplate tpl = XTemplate.create("<p><b>Permissions:</b> <br/><p>{permLabels}</p>");
        final RowExpander expander = new RowExpander();
        expander.setTemplate(tpl);
        final List<ColumnConfig> configs = createColumnsConfig(expander);
        if (configs != null) {

            final ColumnModel cm = new ColumnModel(configs);
            ContentPanel cp = new ContentPanel();
            cp.setBorders(false);
            cp.setBodyBorder(false);
            cp.setHeading("Roles mapping");
            cp.setButtonAlign(Style.HorizontalAlignment.CENTER);
            cp.setLayout(new FitLayout());
            cp.setSize(700, 300);

            Grid<GWTJahiaRole> grid = new Grid<GWTJahiaRole>(store, cm);
            grid.setStyleAttribute("borderTop", "none");
            grid.addPlugin(expander);
            grid.setBorders(false);
            cp.add(grid);

            add(cp);
        } else {
            add(new Label("There is no defined roles"));
        }
        layout();
    }

    /**
     * Create columns from roles
     *
     * @return
     */
    private List<ColumnConfig> createColumnsConfig(RowExpander expander) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        final List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
        configs.add(expander);

        ColumnConfig column = new ColumnConfig();
        column.setId("name");
        column.setHeader(Messages.get("label.roleName", "Role name"));
        column.setWidth(475);
        configs.add(column);

        column = new ColumnConfig();
        column.setId("site");
        column.setHeader(Messages.get("label.site", "Site"));
        column.setWidth(100);
        configs.add(column);

        final GridCellRenderer<GWTJahiaRole> rolePermissionRenderer = new GridCellRenderer<GWTJahiaRole>() {
            public Object render(final GWTJahiaRole currentRole, String property, ColumnData config, final int rowIndex,
                                 final int colIndex, ListStore<GWTJahiaRole> store, Grid<GWTJahiaRole> grid) {

                final CheckBox checkbox = new CheckBox();
                final GWTJahiaRole role = roles.get(rowIndex);
                checkbox.setValue(role.get("grant") != null);
                checkbox.setToolTip(currentRole.getName());
                checkbox.addListener(Events.Change, new Listener<ComponentEvent>() {
                    public void handleEvent(ComponentEvent event) {
                        if (checkbox.getValue()) {
                            contentService.grantRoleToUser(role,isGroup, principalKey, new BaseAsyncCallback() {
                                public void onSuccess(Object o) {
                                    Log.debug("role granted");
                                }

                                public void onApplicationFailure(Throwable throwable) {
                                    Log.error("Error while granting role to user", throwable);
                                }
                            });
                        } else {
                            contentService.removeRoleToPrincipal(role, isGroup, principalKey, new BaseAsyncCallback() {
                                public void onSuccess(Object o) {
                                    Log.debug("role revoked");
                                }

                                public void onApplicationFailure(Throwable throwable) {
                                    Log.error("Error while removing role from principal " + principalKey, throwable);
                                }
                            });
                        }
                    }

                });
                return checkbox;
            }
        };

        column = new ColumnConfig();
        column.setRenderer(rolePermissionRenderer);
        column.setId("user");
        column.setHeader(Messages.get("label.grant", "Grant"));
        column.setWidth(100);
        configs.add(column);

        return configs;
    }


}

