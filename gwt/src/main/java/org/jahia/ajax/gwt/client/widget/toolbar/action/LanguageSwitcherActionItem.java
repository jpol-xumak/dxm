package org.jahia.ajax.gwt.client.widget.toolbar.action;

import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jahia.ajax.gwt.client.core.JahiaGWTParameters;
import org.jahia.ajax.gwt.client.data.GWTJahiaLanguage;
import org.jahia.ajax.gwt.client.data.toolbar.GWTJahiaToolbarItem;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.client.widget.Linker;
import org.jahia.ajax.gwt.client.widget.edit.EditLinker;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: toto
 * Date: Feb 4, 2010
 * Time: 4:19:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class LanguageSwitcherActionItem extends BaseActionItem {
    private transient ComboBox<GWTJahiaLanguage> mainComponent;
    private List<GWTJahiaLanguage> gwtJahiaLanguages;
    private GWTJahiaLanguage selectedLang;
    private String siteKey;

    public LanguageSwitcherActionItem() {

    }

    public void setLanguages(List<GWTJahiaLanguage> gwtJahiaLanguages) {
        this.gwtJahiaLanguages = gwtJahiaLanguages;
    }


    public void setSelectedLang(GWTJahiaLanguage selectedLang) {
        this.selectedLang = selectedLang;
    }

    @Override
    public void init(GWTJahiaToolbarItem gwtToolbarItem, Linker linker) {
        super.init(gwtToolbarItem, linker);
        initMainComponent();
    }

    @Override
    public void handleNewLinkerSelection() {
        super.handleNewLinkerSelection();
        if (linker.getMainNode()!= null && !linker.getMainNode().getSiteUUID().equalsIgnoreCase(siteKey)) {
            siteKey = linker.getMainNode().getSiteUUID();
            JahiaContentManagementService.App.getInstance().getSiteLanguages(new AsyncCallback<List<GWTJahiaLanguage>>() {
                public void onSuccess(List<GWTJahiaLanguage> languages) {
                    mainComponent.getStore().removeAll();
                    mainComponent.getStore().add(languages);
                }

                public void onFailure(Throwable throwable) {
                    mainComponent.getStore().removeAll();
                }
            });
        }

    }

    /**
     * init main component
     */
    private void initMainComponent() {
        siteKey = JahiaGWTParameters.getSiteUUID();        
        mainComponent = new ComboBox<GWTJahiaLanguage>();
        mainComponent.setStore(new ListStore<GWTJahiaLanguage>());
        mainComponent.getStore().add(gwtJahiaLanguages);
        mainComponent.setDisplayField("displayName");
        mainComponent.setTemplate(getLangSwitchingTemplate());
        mainComponent.setTypeAhead(true);
        mainComponent.setTriggerAction(ComboBox.TriggerAction.ALL);
        mainComponent.setForceSelection(true);
        mainComponent.setValue(selectedLang);
        mainComponent.addSelectionChangedListener(new SelectionChangedListener<GWTJahiaLanguage>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GWTJahiaLanguage> event) {
                ((EditLinker) linker).getMainModule().switchLanguage(event.getSelectedItem().getLanguage());
            }
        });
        setEnabled(true);
    }


    @Override
    public Component getCustomItem() {
        return mainComponent;
    }


    @Override
    public void setEnabled(boolean enabled) {
        mainComponent.setEnabled(enabled);
    }

    /**
     * LangSwithcing template
     *
     * @return
     */
    private static native String getLangSwitchingTemplate()  /*-{
    return  [
    '<tpl for=".">',
    '<div class="x-combo-list-item"><img src="{image}"/> {displayName}</div>',
    '</tpl>'
    ].join("");
  }-*/;


}