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
package org.jahia.ajax.gwt.client.widget.edit.mainarea;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.core.XTemplate;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Point;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.ToolButton;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.extjs.gxt.ui.client.widget.tips.ToolTipConfig;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.http.client.URL;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import org.jahia.ajax.gwt.client.core.JahiaGWTParameters;
import org.jahia.ajax.gwt.client.data.GWTJahiaChannel;
import org.jahia.ajax.gwt.client.data.GWTJahiaLanguage;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.data.publication.GWTJahiaPublicationInfo;
import org.jahia.ajax.gwt.client.data.toolbar.GWTEditConfiguration;
import org.jahia.ajax.gwt.client.data.toolbar.GWTJahiaToolbarItem;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.util.icons.ToolbarIconProvider;
import org.jahia.ajax.gwt.client.widget.Linker;
import org.jahia.ajax.gwt.client.widget.contentengine.EditContentEnginePopupListener;
import org.jahia.ajax.gwt.client.widget.contentengine.EngineLoader;
import org.jahia.ajax.gwt.client.widget.edit.EditLinker;
import org.jahia.ajax.gwt.client.widget.edit.InfoLayers;
import org.jahia.ajax.gwt.client.widget.edit.ToolbarHeader;
import org.jahia.ajax.gwt.client.widget.toolbar.ActionContextMenu;

import java.util.*;

/**
 * First module of any rendered element.
 * Sub content will be created as ListModule or SimpleModule.
 */
public class MainModule extends Module {

    private static MainModule module;

    private EditLinker editLinker;
    private Storage storage;
    private ActionContextMenu contextMenu;
    private GWTEditConfiguration config;
    private Map<String, GWTEditConfiguration> configs = new HashMap<String, GWTEditConfiguration>();

    private InfoLayers infoLayers = new InfoLayers();
    private Map<String, Boolean> activeLayers = new HashMap<String, Boolean>();
    private boolean ctrlActive = false;
    private Map<Module, Selection> selections = new HashMap<Module, Selection>();

    private boolean needParseAfterLayout = false;
    private Map<Element, Module> moduleMap;
    protected LayoutContainer scrollContainer;
    protected LayoutContainer center;
    protected EditFrame frame;
    private Element mainModuleElement;
    protected String frameError = null;
    protected boolean frameErrorRedirect = false;

    private final LayoutContainer headContainer;
    private String newLocation = null;
    private boolean firstLoad = true;
    protected String template;

    private GWTJahiaChannel activeChannel;
    private String activeChannelVariant;
    private static boolean globalSelectionDisabled = false;

    public MainModule(final String path, final String template, String nodeTypes, GWTEditConfiguration config) {
        super("main", path, nodeTypes, new BorderLayout());
        setScrollMode(Style.Scroll.NONE);

        this.id = "main";
        this.path = path;
        this.template = template;
        this.config = config;
        configs.put(this.config.getName(), this.config);

        this.depth = 0;

        storage = Storage.getSessionStorageIfSupported();
        if (storage != null) {
            String user = storage.getItem("currentUser");
            if (user != null && !user.equals(JahiaGWTParameters.getCurrentUser())) {
                storage.clear();
            }
            storage.setItem("currentUser",JahiaGWTParameters.getCurrentUser());
        }
        head = new ToolbarHeader();
        head.addStyleName("x-panel-header");
        head.setStyleAttribute("z-index", "999");
        head.setStyleAttribute("position", "relative");
        headContainer = new LayoutContainer(new FitLayout());
        headContainer.add(head);
        add(headContainer, new BorderLayoutData(Style.LayoutRegion.NORTH, 32));

        if (config.getMainModuleToolbar() == null || config.getMainModuleToolbar().getGwtToolbarItems().isEmpty()) {
            headContainer.hide();
        }

        frame = new EditFrame();
        frame.setWidth("100%");
        frame.setHeight("100%");
        frame.addStyleName("x-noshim");
        frame.sinkEvents(Event.ONLOAD /*+ Event.ONCLICK + Event.ONCONTEXTMENU + Event.ONMOUSEOVER + Event.ONMOUSEOUT*/);


        scrollContainer = new LayoutContainer(new FlowLayout()) {
            @Override
            protected void onRightClick(ComponentEvent ce) {
                if (contextMenu != null && fireEvent(Events.ContextMenu, ce)) {
                    Point position = MainModule.getInstance().getContainer().getPosition(false);
                    final int x = ce.getClientX() + position.x;
                    final int y = ce.getClientY() + position.y;
                    ce.stopEvent();
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            onShowContextMenu(x, y);
                        }
                    });
                }
            }
        };
        scrollContainer.addStyleName("gwt-body-edit");
        scrollContainer.setStyleAttribute("position", "relative");
        scrollContainer.addListener(Events.OnClick, new Listener<BaseEvent>() {
            public void handleEvent(BaseEvent be) {
                if (contextMenu != null && contextMenu.isVisible()) {
                    contextMenu.hide();
                }
            }
        });
        scrollContainer.add(frame);

        center = new LayoutContainer(new FitLayout());
        center.setScrollMode(Style.Scroll.NONE);

        add(center, new BorderLayoutData(Style.LayoutRegion.CENTER));

        Hover.getInstance().setMainModule(this);

        module = this;
        exportStaticMethod();
    }

    boolean inframe = false;

    public boolean isInframe() {
        return inframe;
    }

    public static boolean isGlobalSelectionDisabled() {
        return globalSelectionDisabled;
    }

    public Map<Module, Selection> getSelections() {
        return selections;
    }

    public void initWithLinker(EditLinker linker) {
        this.editLinker = linker;

        ((ToolbarHeader) head).removeAllTools();
        if (config.getMainModuleToolbar() != null && !config.getMainModuleToolbar().getGwtToolbarItems().isEmpty()) {
            for (GWTJahiaToolbarItem item : config.getMainModuleToolbar().getGwtToolbarItems()) {
                ((ToolbarHeader) head).addItem(linker, item);
            }
            head.addTool(new ToolButton("x-tool-refresh", new SelectionListener<IconButtonEvent>() {
                public void componentSelected(IconButtonEvent event) {
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put(Linker.REFRESH_MAIN, true);
                    refresh(data);
                }
            }));

            ((ToolbarHeader) head).attachTools();

            headContainer.add(head);
            headContainer.show();
        } else {
            headContainer.hide();
        }


        String location = newLocation;
        newLocation = null;
        String hash = Window.Location.getHash();
        if (location == null && !hash.equals("") && hash.contains("|")) {
            location = hash.substring(hash.indexOf('|') + 1);
        }
        if (location == null) {
            location = Window.Location.getPath();
        }
        if (location.contains("://")) {
            location = location.substring(location.indexOf("://") + 3);
            location = location.substring(location.indexOf("/"));
        }
        if (location.startsWith(JahiaGWTParameters.getContextPath() + config.getDefaultUrlMapping() + "/")) {
            location = location.replaceFirst(config.getDefaultUrlMapping(), config.getDefaultUrlMapping() + "frame");
        }
        if (location.contains("frame/") && !isValidUrl(location)) {
            String start = location.substring(0, location.indexOf("frame/"));
            start = start.substring(JahiaGWTParameters.getContextPath().length());
            location = location.replaceFirst(start + "frame/", config.getDefaultUrlMapping() + "frame/");
        }

        location = URL.decode(location);
        goToUrl(location, true, true, true);

//        scrollContainer.sinkEvents();

        Listener<ComponentEvent> listener = new Listener<ComponentEvent>() {
            public void handleEvent(ComponentEvent ce) {
                setCtrlActive(ce);
                makeSelected();
            }
        };

        // on click listener
        scrollContainer.addListener(Events.OnClick, listener);

        // on double click listener
        scrollContainer.addListener(Events.OnDoubleClick, new EditContentEnginePopupListener(this, editLinker));

        if (config.getContextMenu() != null) {
            // contextMenu
            contextMenu = new ActionContextMenu(config.getContextMenu(), editLinker) {
                @Override
                public boolean beforeShow() {
                    makeSelected();

                    if (editLinker.getSelectionContext().getSingleSelection() == editLinker.getSelectionContext().getMainNode()) {
                        Module selectedModule = editLinker.getSelectedModule();
                        return selectedModule != null && selectedModule instanceof SimpleModule && super.beforeShow();
                    }
                    return super.beforeShow();
                }
            };
            scrollContainer.setContextMenu(contextMenu);

        }

        infoLayers.initWithLinker(linker);
    }

    private boolean isValidUrl(String url) {
        return url.startsWith(JahiaGWTParameters.getContextPath() + config.getDefaultUrlMapping() + "frame/");
    }

    private void layoutChannel() {
        center.removeAll();


        if (activeChannel == null || "generic".equals(activeChannel.getValue()) || !config.isSupportChannelsDisplay()) {
            scrollContainer.setPosition(0, 0);
            center.setLayout(new FitLayout());
            center.setScrollMode(Style.Scroll.NONE);
            center.add(scrollContainer);
        } else {
            int variantIndex = activeChannel.getVariants().indexOf(activeChannelVariant);
            if (variantIndex == -1) {
                variantIndex = 0;
            }

            // first let setup the device decorator layout container
            LayoutContainer deviceDecoratorContainer = new LayoutContainer(new AbsoluteLayout());
            deviceDecoratorContainer.setBorders(false);
            int[] decoratorImageSize = activeChannel.getVariantDecoratorImageSize(variantIndex);
            if (decoratorImageSize.length == 0) {
                decoratorImageSize = new int[]{-1, -1};
            }
            deviceDecoratorContainer.setSize(decoratorImageSize[0], decoratorImageSize[1]);
            deviceDecoratorContainer.setStyleAttribute("margin-left", "auto");
            deviceDecoratorContainer.setStyleAttribute("margin-right", "auto");
            AbsoluteData deviceOuterData = new AbsoluteData(0, 0);
            deviceOuterData.setMargins(new Margins(0, 0, 0, 0));
            if (activeChannel.getVariantDecoratorImage(variantIndex) != null) {
                deviceDecoratorContainer.add(new Image(JahiaGWTParameters.getContextPath() + activeChannel.getVariantDecoratorImage(variantIndex)), deviceOuterData);
            }

            int[] usableResolution = getUsableDeviceResolution(activeChannel, variantIndex);
            scrollContainer.setSize(usableResolution[0], usableResolution[1]);
            scrollContainer.setScrollMode(Style.Scroll.NONE);

            int[] screenPosition = null;
            screenPosition = activeChannel.getVariantDecoratorScreenPosition(variantIndex);
            if (screenPosition == null || screenPosition.length == 0) {
                screenPosition = new int[]{0, 0};
            }

            AbsoluteData deviceData = new AbsoluteData(screenPosition[0], screenPosition[1]);
            deviceData.setMargins(new Margins(0, 0, 0, 0));
            deviceDecoratorContainer.add(scrollContainer, deviceData);

            center.setLayout(new FlowLayout());
            center.setScrollMode(Style.Scroll.AUTO);
            center.add(deviceDecoratorContainer);
        }

//        layout();

    }

    private int[] getUsableDeviceResolution(GWTJahiaChannel activeChannel, int activeChannelIndex) {
        int[] usableResolution;
        if (activeChannel != null) {
            usableResolution = activeChannel.getVariantUsableResolution(activeChannelIndex);
            if (usableResolution.length == 0) {
                usableResolution = new int[]{
                        -1,
                        -1
                };
            }
        } else {
            usableResolution = new int[]{
                    getWidth(),
                    getHeight() - (head != null ? head.getOffsetHeight() : 0)
            };
        }
        return usableResolution;
    }

    /**
     * select current module
     */
    public void makeSelected() {
        if (selectable && !MainModule.isGlobalSelectionDisabled()) {
            editLinker.onModuleSelection(null);
        }
    }

    /**
     * Returns an instance of the edit linker.
     *
     * @return an instance of the edit linker
     */
    public EditLinker getEditLinker() {
        return editLinker;
    }

    /**
     * Performs refresh of the main module, based on the provided data.
     *
     * @param data the refresh data
     */
    public void refresh(Map<String, Object> data) {
        if (data != null && (data.containsKey(Linker.REFRESH_ALL) || data.containsKey(Linker.REFRESH_MAIN) || needRefresh(data))) {
            boolean forceCssRefresh = false;
            boolean forceJavascriptRefresh = false;
            if (data.containsKey("node")) {
                GWTJahiaNode n = (GWTJahiaNode) data.get("node");
                if (n != null) {
                    forceCssRefresh = n.getNodeTypes().contains("jnt:cssFile");
                    forceJavascriptRefresh = n.getNodeTypes().contains("jnt:javascriptFile");
                }
            }
            final String url = getUrl(path, template, activeChannel != null ? activeChannel.getValue() : null, activeChannelVariant);
            goToUrl(url, data.containsKey("forceImageRefresh"), forceCssRefresh, forceJavascriptRefresh);
        }
    }

    public boolean needRefresh(Map<String, Object> data) {
        List<String> paths = (List<String>) data.get("publishedNodes");
        if (paths != null) {
            for (String s : paths) {
                List<Module> modules = ModuleHelper.getModulesByPath().get(s);
                if (modules != null && !modules.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Changes the frame URL to the provided one.
     *
     * @param url                    the URL to be used for the frame
     * @param forceImageRefresh      should we force refresh of the images?
     * @param forceCssRefresh        should we force refresh of the CSS files?
     * @param forceJavascriptRefresh should we force refresh of the JavaScript files?
     */
    public void goToUrl(final String url, final boolean forceImageRefresh, boolean forceCssRefresh, boolean forceJavascriptRefresh) {
        mask(Messages.get("label.loading", "Loading..."), "x-mask-loading");
        setChannelFromUrl(url);
        layoutChannel();
        frame.setForceImageRefresh(forceImageRefresh);
        frame.setForceCssRefresh(forceCssRefresh);
        frame.setForceJavascriptRefresh(forceJavascriptRefresh);
        frame.setUrl(url);
        center.layout(true);
    }

    private void setChannelFromUrl(String url) {
        if (config.isSupportChannelsDisplay()) {
            activeChannel = editLinker.getActiveChannel();
            activeChannelVariant = editLinker.getActiveChannelVariant();

            Map<String,String> params = getParamsFromUrl(url);
            if (params.containsKey("channel")) {
                String channelName = params.get("channel");
                for (GWTJahiaChannel gwtJahiaChannel : JahiaGWTParameters.getChannels()) {
                    if (gwtJahiaChannel.getValue().equals(channelName)) {
                        this.activeChannel = gwtJahiaChannel;
                        this.activeChannelVariant = null;
                        break;
                    }
                }

                if (params.containsKey("variant")) {
                    activeChannelVariant = params.get("variant");
                }
            }
        } else {
            activeChannel = null;
            activeChannelVariant = null;
        }
    }

    private Map<String,String> getParamsFromUrl(String url) {
        Map<String,String> m = new HashMap<String,String>();
        if (url.contains("?")) {
            String[] params = url.substring(url.indexOf('?')+1).split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String[] v = param.split("=");
                    m.put(v[0],v[1]);
                }
            }
        }
        return m;
    }

    public static void waitingMask(String text) {
        getInstance().mask(text, "x-mask-loading");
    }

    public static void hideMask() {
        getInstance().unmask();
    }

    public static void editContent(String path) {
        List<Module> modules = ModuleHelper.getModulesByPath().get(path);
        EngineLoader.showEditEngine(getInstance().getEditLinker(),modules.get(0).getNode(), null);
    }

    public String getUrl(String path, String template, String channel, String variant) {
        if (template != null && "default".equals(template)) {
            template = null;
        }
        StringBuilder url = new StringBuilder(getBaseUrl() + path + (template != null && !template.equals("") ? ("." + template) : "") + ".html");
        // add channel parameters if needed
        if (channel != null && channel.equals("generic") && (editLinker.getActiveChannel() == null || editLinker.getActiveChannel().getValue().equals("generic"))) {
            channel = null;
            variant = null;
        } else if (channel == null && editLinker.getActiveChannel() != null && !editLinker.getActiveChannel().getValue().equals("generic")) {
            channel = editLinker.getActiveChannel().getValue();
            variant = editLinker.getActiveChannelVariant();
        }

        if (channel != null && channel.length() > 0) {
            url.append("?channel=").append(channel);
            if (variant != null && variant.length() > 0) {
                url.append("&variant=").append(variant);
            }
        }
        return url.toString();
    }

    /**
     * Computes the base URL for the main module frame.
     *
     * @return the base URL for the main module frame
     */
    public String getBaseUrl() {
        String baseUrl = JahiaGWTParameters.getBaseUrl();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf("/" + JahiaGWTParameters.getWorkspace() + "/"));
        baseUrl += "frame/" + JahiaGWTParameters.getWorkspace();
        baseUrl += "/" + (editLinker.getLocale() == null ? JahiaGWTParameters.getLanguage() : editLinker.getLocale());
        return baseUrl;
    }

    @Override
    public Element getInnerElement() {
        if (frame != null) {
            IFrameElement iframe = IFrameElement.as(frame.getElement());
            com.google.gwt.dom.client.Element body = iframe.getContentDocument().getElementsByTagName("body").getItem(0);
            return (Element) body;
        }
        return super.getInnerElement();
    }

    public static native void setDocumentTitle(String title) /*-{
        $doc.title = title;
    }-*/;

    private void refreshImages(Element element) {
        NodeList<com.google.gwt.dom.client.Element> elementsByTagName = element
                .getElementsByTagName("img");
        if (elementsByTagName == null) {
            return;
        }
        String base = JahiaGWTParameters.getContextPath() + "/files/default/";
        String suffix = "tst=" + System.currentTimeMillis();
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            com.google.gwt.dom.client.Element el = elementsByTagName.getItem(i);
            String url = el.getAttribute("src");
            if (url != null && url.startsWith(base)) {
                el.setAttribute("src", url + (url.indexOf('?') == -1 ? "?" : "&") + suffix);
            }
        }
    }

    private void refreshCSS(Element element) {
        NodeList<com.google.gwt.dom.client.Element> elementsByTagName = element
                .getElementsByTagName("link");
        if (elementsByTagName == null) {
            return;
        }
        String base = JahiaGWTParameters.getContextPath() + "/modules/";
        String suffix = "tst=" + System.currentTimeMillis();
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            com.google.gwt.dom.client.Element el = elementsByTagName.getItem(i);
            String url = el.getAttribute("href");
            String type = el.getAttribute("type");
            if (type != null && type.equals("text/css") && url != null && url.startsWith(base)) {
                el.setAttribute("href", url + (url.indexOf('?') == -1 ? "?" : "&") + suffix);
            }
        }
    }

    private void refreshScripts(Element element) {
        NodeList<com.google.gwt.dom.client.Element> elementsByTagName = element
                .getElementsByTagName("script");
        if (elementsByTagName == null) {
            return;
        }
        String base = JahiaGWTParameters.getContextPath() + "/modules/";
        String suffix = "tst=" + System.currentTimeMillis();
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            com.google.gwt.dom.client.Element el = elementsByTagName.getItem(i);
            String url = el.getAttribute("src");
            String type = el.getAttribute("type");
            if (type != null && type.equals("text/javascript") && url != null && url.startsWith(base)) {
                el.setAttribute("src", url + (url.indexOf('?') == -1 ? "?" : "&") + suffix);
            }
        }
    }

    @Override
    protected void onAfterLayout() {
        super.onAfterLayout();
        if (needParseAfterLayout) {
            if (moduleMap != null) {
                ModuleHelper.move(moduleMap);
            }
            scrollContainer.setHeight(getHeight() - (head != null ? head.getOffsetHeight() : 0));
            scrollContainer.setWidth(getWidth());

            if (activeChannel != null && !"default".equals(activeChannel.getValue())) {
                int[] usableResolution = getUsableDeviceResolution(activeChannel, editLinker.getActiveChannelVariantIndex());
                scrollContainer.setSize(usableResolution[0], usableResolution[1]);
            }
            needParseAfterLayout = false;
        }
    }

    protected void onResize(int width, int height) {
        super.onResize(width, height);
        scrollContainer.setHeight(getHeight() - (head != null ? head.getOffsetHeight() : 0));
        scrollContainer.setWidth(getWidth());

        if (activeChannel != null && !"default".equals(activeChannel.getValue())) {
            int[] usableResolution = getUsableDeviceResolution(activeChannel, editLinker.getActiveChannelVariantIndex());
            scrollContainer.setSize(usableResolution[0], usableResolution[1]);
        }

        if (editLinker.getSelectedModule() != null) {
            for (Selection s : selections.values()) {
                s.hide();
                s.show();
            }
        }
    }

    public LayoutContainer getContainer() {
        return scrollContainer;
    }

    public boolean isCtrlActive() {
        return ctrlActive;
    }

    public void setCtrlActive(DomEvent event) {
        this.ctrlActive = Window.Navigator.getPlatform().toLowerCase().contains("mac") ? DOM.eventGetMetaKey(event.getEvent()) : event.isControlKey();
    }

    public void parse(List<Element> el) {
        if (moduleMap != null) {
            ModuleHelper.deleteAll(moduleMap);
            List<Component> l = new ArrayList<Component>(scrollContainer.getItems());
            for (Component component : l) {
                if (component instanceof Module) {
                    component.removeFromParent();
                }
            }
        }
        moduleMap = ModuleHelper.parse(this, null, el);
    }

    public String getModuleId() {
        return "main";
    }

    public static void staticGoTo(String path, String template) {
        module.goTo(path, template, null, null);
    }

    public static void staticGoTo(String path, String template, String channel, String variant) {
        module.goTo(path, template, channel, variant);
    }

    public static void staticGoToUrl(String path) {
        module.goToUrl(path, false, false, false);
    }

    public static void popState(String location, String config) {
        MainModule m = getInstance();
        if (!config.equals(m.getConfig().getName())) {
            m.getEditLinker().switchConfig(m.configs.get(config), location + "##", true, true, null);
        } else {
            m.goToUrl(location + "##", false, false, false);
        }
    }

    public void goTo(String path, String template, String channel, String variant) {
        goToUrl(getUrl(path, template, channel, variant), false, false, false);
    }

    private void setHashMarker(String path) {
        String currentHref = Window.Location.getHref();
        if (currentHref.endsWith(path)) {
            return;
        }

        if (supportPushState()) {
            if (!path.endsWith("##")) {
                String pathWithoutFrame = path.replaceFirst("frame/", "/");
                if (Window.Location.getQueryString().contains("gwt.codesvr")) {
                    Map<String,String> m = getParamsFromUrl(Window.Location.getQueryString());
                    pathWithoutFrame += (pathWithoutFrame.contains("?") ? '&': '?') + "gwt.codesvr="+m.get("gwt.codesvr");
                }
                if (!pathWithoutFrame.equals(currentHref) || firstLoad) {
                    firstLoad = false;
                    pushState(pathWithoutFrame, path, config.getName());
                }
            }
        } else {
            int hashPosition = currentHref.indexOf('#');
            if (hashPosition > 0) {
                currentHref = currentHref.substring(0, hashPosition);
            }
            Window.Location.assign(currentHref + "#" + MainModule.getInstance().getConfig().getName() + "|" + URL.encode(path));
        }
        path = path.substring(path.indexOf(JahiaGWTParameters.getContextPath() + JahiaGWTParameters.getServletPath()));

        if (storage != null) {
            String storedPath = path;
            if (storedPath.endsWith("##")) {
                storedPath = storedPath.substring(0, storedPath.length() - 2);
            }
            storage.setItem(MainModule.getInstance().getConfig().getName() + "_path", storedPath);
        }
    }

    public void switchLanguage(GWTJahiaLanguage language) {
        if (!language.getLanguage().equals(JahiaGWTParameters.getLanguage())) {
            editLinker.setLocale(language);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(Linker.REFRESH_MAIN, true);
            data.put("event", "languageChanged");
            editLinker.refresh(data);
        }
    }

    public void switchChannel(GWTJahiaChannel channel, String variant) {
        editLinker.setActiveChannelVariant(variant);
        editLinker.setActiveChannel(channel);
        if (!editLinker.isInSettingsPage()) {
            goTo(path, template, channel.getValue(), variant);
        }
    }


    public void setNodeTypes(String nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    public void setReferenceTypes(String referenceTypes) {
        this.referenceTypes = referenceTypes;
    }

    public void setMainModuleElement(Element mainModuleElement) {
        this.mainModuleElement = mainModuleElement;
    }

    @Override
    public void setNode(GWTJahiaNode node) {
        mainModule = this;
        super.setNode(node);
        if (node.getNodeTypes().contains("jmix:markedForDeletion")) {
            overlayLabel = new HTML(Messages.get("label.deleted", "Deleted"));
            overlayLabel.setStyleName("deleted-overlay");
            overlayColorText = "#f00";
            opacity = "0.4";
        }
        if (node.isShared()) {
            this.setToolTip(new ToolTipConfig(Messages.get("info_important", "Important"),
                    Messages.get("info_sharednode", "This is a shared node")));
        }

        final List<String> types = node.getNodeTypes();
        final List<String> inheritedTypes = node.getInheritedNodeTypes();
        editLinker.setInSettingsPage(!types.contains("jnt:page") && !types.contains("jnt:template") && !types.contains("jnt:content") &&
                        !inheritedTypes.contains("jnt:page") && !inheritedTypes.contains("jnt:template") && !inheritedTypes.contains("jnt:content"));

        setDocumentTitle(Messages.get("label." + config.getName().substring(0, config.getName().length() - 4), config.getName()) + " - " + node.getDisplayName());

        if (head != null) {
            ((ToolbarHeader) head).handleNewMainNodeLoaded(node);
        }

        editLinker.handleNewMainNodeLoaded();
        if (head != null && head instanceof ToolbarHeader) {
            ((ToolbarHeader) head).handleNewMainNodeLoaded(node);
        }
        if (overlayLabel != null) {
            DOM.setStyleAttribute(mainModuleElement,"position","relative");
            mainModuleElement.getParentElement().insertFirst(overlayLabel.getElement());
            overlayLabel.setHeight("100%");
            overlayLabel.setWidth("100%");

            DOM.setStyleAttribute(mainModuleElement, "opacity", opacity);
            layout();
        }
    }

    public GWTEditConfiguration getConfig() {
        return config;
    }

    /**
     * Set edit mode configuration and location.
     * A path can be explicitly specified.
     * It can also be stored and recalled from the HTML5 web storage, if available.
     *
     * @param config
     * @param newPath
     */
    public void setConfig(GWTEditConfiguration config, String newPath) {
        JahiaGWTParameters.changeServletMapping(this.config.getDefaultUrlMapping(), config.getDefaultUrlMapping());

        activeChannel = null;
        activeChannelVariant = null;
        if (newPath != null) {
            newLocation = newPath;
        } else {
            newLocation = frame.getCurrentFrameUrl().replaceFirst(this.config.getDefaultUrlMapping(), config.getDefaultUrlMapping());
        }

        this.config = config;

        configs.put(this.config.getName(), this.config);
    }

    public void handleNewModuleSelection(Module selectedModule) {
        Selection l = new Selection(selectedModule);
        if (!ctrlActive || selectedModule == null) {
            for (Selection s : selections.values()) {
                s.hide();
            }
            selections.clear();
        }
        if (selectedModule != null && !(selectedModule instanceof MainModule)) {
            selections.put(selectedModule, l);
            l.select();
            l.show();
        }
        if (head != null) {
            ((ToolbarHeader) head).handleNewLinkerSelection();
        }

        l.layout();
    }

    public void handleNewMainSelection(String path, String template) {
        if (storage != null) {
            storage.setItem(MainModule.getInstance().getConfig().getName() + "_nodePath",path);
        }

        Map<String, List<String>> params = null;
        if ((this.path != null ? this.path.equals(path) : path == null) &&
                (this.template != null ? this.template.equals(template) : template == null) &&
                (this.moduleParams != null ? this.moduleParams.equals(params) : params == null)) {
            return;
        }

        this.path = path;
        this.template = template;

        moduleParams = params;

        if (head != null) {
            ((ToolbarHeader) head).handleNewLinkerSelection();
        }
    }

    public void setInfoLayer(String key, boolean value) {
        if (value) {
            activeLayers.put(key, value);
        } else {
            activeLayers.remove(key);
        }
        refreshInfoLayer();
    }

    public void refreshInfoLayer() {
        infoLayers.removeAll();
        if (!activeLayers.isEmpty()) {
            infoLayers.setMainModule(this);

            List<Module> modules = ModuleHelper.getModules();

            String lastUnpublished = null;
            if (modules != null) {
                for (Module m : modules) {
                    if (m instanceof AreaModule || !m.getPath().endsWith("*")) {
                        GWTJahiaNode currentNode = m.getNode();
                        if (currentNode != null) {
                            List<LayoutContainer> images = new ArrayList<LayoutContainer>();
                            if (activeLayers.containsKey("acl") && currentNode.isHasAcl()) {
                                AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                        "viewACLStatus");
                                LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                layoutContainer.add(icon.createImage());
                                images.add(layoutContainer);
                            }
                            if (activeLayers.containsKey("publication")) {
                                GWTJahiaPublicationInfo info = currentNode.getAggregatedPublicationInfo();
                                if (lastUnpublished == null || !currentNode.getPath().startsWith(lastUnpublished)) {
                                    if (info.isLocked()) {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "publication/locked");
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(icon.createImage());
                                        images.add(layoutContainer);
                                    }

                                    if (info.getStatus() == GWTJahiaPublicationInfo.NOT_PUBLISHED ||
                                            info.getStatus() == GWTJahiaPublicationInfo.UNPUBLISHED) {
                                        lastUnpublished = currentNode.getPath();
                                        if (info.getStatus() == GWTJahiaPublicationInfo.UNPUBLISHED) {
                                            AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                    "publication/unpublished");
                                            LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                            layoutContainer.add(icon.createImage());
                                            images.add(layoutContainer);
                                        } else {
                                            AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                    "publication/notpublished");
                                            LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                            layoutContainer.add(icon.createImage());
                                            images.add(layoutContainer);
                                        }
                                    } else if (info.getStatus() == GWTJahiaPublicationInfo.MODIFIED) {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "publication/modified");
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(icon.createImage());
                                        images.add(layoutContainer);
                                    } else if (info.getStatus() ==
                                            GWTJahiaPublicationInfo.MANDATORY_LANGUAGE_UNPUBLISHABLE) {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "publication/mandatorylanguageunpublishable");
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(icon.createImage());
                                        images.add(layoutContainer);
                                    } else if (info.getStatus() == GWTJahiaPublicationInfo.MANDATORY_LANGUAGE_VALID) {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "publication/mandatorylanguagevalid");
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(icon.createImage());
                                        images.add(layoutContainer);
                                    }
                                }
                            }
                            if (activeLayers.containsKey("visibility")) {
                                Map<GWTJahiaNode, ModelData> visibility = currentNode.getVisibilityInfo();
                                if (!visibility.isEmpty()) {
                                    if (currentNode.isVisible()) {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "visibilityStatusGreen");
                                        Image image = icon.createImage();
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(image);
                                        Set<Map.Entry<GWTJahiaNode, ModelData>> entries = visibility.entrySet();
                                        StringBuilder toolTip = new StringBuilder();
                                        for (Map.Entry<GWTJahiaNode, ModelData> entry : entries) {
                                            if (toolTip.length() > 0) {
                                                toolTip.append("<br/>");
                                            }
                                            XTemplate tpl = XTemplate.create((String) entry.getValue().get(
                                                    "xtemplate"));
                                            toolTip.append(tpl.applyTemplate(com.extjs.gxt.ui.client.util.Util.getJsObject(
                                                    entry.getKey())));
                                        }
                                        layoutContainer.setToolTip(toolTip.toString());
                                        images.add(layoutContainer);
                                    } else {
                                        AbstractImagePrototype icon = ToolbarIconProvider.getInstance().getIcon(
                                                "visibilityStatusRed");
                                        LayoutContainer layoutContainer = new LayoutContainer(new CenterLayout());
                                        layoutContainer.add(icon.createImage());
                                        Set<Map.Entry<GWTJahiaNode, ModelData>> entries = visibility.entrySet();
                                        StringBuilder toolTip = new StringBuilder();
                                        for (Map.Entry<GWTJahiaNode, ModelData> entry : entries) {
                                            if (toolTip.length() > 0) {
                                                toolTip.append("<br/>");
                                            }
                                            XTemplate tpl = XTemplate.create((String) entry.getValue().get(
                                                    "xtemplate"));
                                            toolTip.append(tpl.applyTemplate(com.extjs.gxt.ui.client.util.Util.getJsObject(
                                                    entry.getKey())));
                                        }
                                        layoutContainer.setToolTip(toolTip.toString());
                                        images.add(layoutContainer);
                                    }
                                }
                            }
                            if (!images.isEmpty()) {
                                infoLayers.addInfoLayer(m, images, null, true, "1");
                            }
                        }
                    }
                }
            }
        }
    }


    public boolean isDraggable() {
        return false;
    }

    public static MainModule getInstance() {
        return module;
    }

    public static native void exportStaticMethod() /*-{
        $wnd.goTo = function (path, template, params) {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::staticGoTo(Ljava/lang/String;Ljava/lang/String;)(path, template);
        }
        $wnd.goToUrl = function (path, template, params) {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::staticGoToUrl(Ljava/lang/String;)(path);
        }
        $wnd.addEventListener("popstate", function (event) {
            if (event.state) {
                @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::popState(Ljava/lang/String;Ljava/lang/String;)(event.state.location, event.state.config);
            }
        });
        $wnd.waitingMask = function (text) {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::waitingMask(Ljava/lang/String;)(text);
        };
        $wnd.hideMask = function () {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::hideMask()();
        };
        $wnd.editContent = function (path) {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::editContent(Ljava/lang/String;)(path);
        };
        $wnd.disableGlobalSelection = function (value) {
            @org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule::globalSelectionDisabled = value;
        };
    }-*/;

    public InfoLayers getInfoLayers() {
        return infoLayers;
    }

    private native boolean supportPushState() /*-{
        if ($wnd.history && $wnd.history.pushState) {
            return true;
        } else {
            return false;
        }
    }-*/;

    private native boolean pushState(String path, String location, String config) /*-{
        var state = {location: location, config: config};
        if ($wnd.history.state) {
            $wnd.history.pushState(state, null, path);
        } else {
            $wnd.history.replaceState(state, null, path);
        }
        return true;
    }-*/;

    public void goToExternalUrl(String url) {
        mask(Messages.get("label.loading", "Loading..."), "x-mask-loading");
        frame.setExternalUrl(url);
    }

    private void parseFrameContent(boolean forceImageRefresh, boolean forceCssRefresh, boolean forceJavascriptRefresh) {
        try {
            frameError = null;
            final IFrameElement iframe = IFrameElement.as(frame.getElement());
            Document contentDocument = iframe.getContentDocument();
            Element body = (Element) contentDocument.getElementsByTagName("body").getItem(0);
            Element head = (Element) contentDocument.getElementsByTagName("head").getItem(0);
            setHashMarker(frame.getCurrentFrameUrl());
            if (forceImageRefresh) {
                refreshImages(body);
            }
            if (head != null) {
                if (forceCssRefresh) {
                    refreshCSS(head);
                }
                if (forceJavascriptRefresh) {
                    refreshScripts(head);
                }
            }
            Hover.getInstance().removeAll();
            List<Element> el = null;
            List<Element> elBody = null;
            if ("true".equals(body.getAttribute("jahia-parse-html"))) {
                Element innerElement = getInnerElement();
                elBody = ModuleHelper.getAllJahiaTypedElementsRec(body);
                if (body.equals(innerElement)) {
                    el = elBody;
                } else {
                    el = ModuleHelper.getAllJahiaTypedElementsRec(getInnerElement());
                }

            } else if (body.getAttribute("jahia-error-code") != null && !"".equals(body.getAttribute("jahia-error-code"))) {
                frameError = body.getAttribute("jahia-error-code");
            } else {
                NodeList<com.google.gwt.dom.client.Element> el1 = body.getElementsByTagName("div");
                int i = 0;
                Element e = null;
                while (i < el1.getLength()) {
                    e = (Element) el1.getItem(i);
                    if ("mainmodule".equals(e.getAttribute(ModuleHelper.JAHIA_TYPE))) {
                        el = Arrays.asList(e);
                        elBody = Arrays.asList(e);
                        break;
                    }
                    i++;
                }


            }
            if (contextMenu != null) {
                contextMenu.hide();
            }

            if (el != null && elBody != null) {
                ModuleHelper.tranformLinks((Element) contentDocument.getDocumentElement());
                ModuleHelper.initAllModules(MainModule.this, body, el, elBody, config);
                editLinker.getSidePanel().enable();
            } else {
                // if body empty, this is not a jahia page
                path = "";
                // clear side panel
                editLinker.getSidePanel().disable();
            }
            editLinker.getMainModule().unmask();
            needParseAfterLayout = true;
            layout();
        } catch (Exception e) {
            Log.error("Error in EditFrame: " + e.getMessage(), e);
        }
    }

    private class EditFrame extends Frame {
        private String url = null;
        private boolean forceImageRefresh = false;
        private boolean forceCssRefresh = false;
        private boolean forceJavascriptRefresh = false;

        private EditFrame() {
        }

        public void onBrowserEvent(Event event) {
            try {
                if (event.getTypeInt() == Event.ONLOAD) {
                    if (isValidUrl(url)) {
                        final IFrameElement iframe = IFrameElement.as(frame.getElement());
                        Document contentDocument = iframe.getContentDocument();

                        parseFrameContent(forceImageRefresh, forceCssRefresh, forceJavascriptRefresh);

                        DOM.sinkEvents((Element) contentDocument.getDocumentElement(), Event.ONMOUSEMOVE + Event.ONMOUSEUP + Event.ONCONTEXTMENU + Event.ONCLICK + Event.ONMOUSEDOWN);
                        DOM.setEventListener((Element) contentDocument.getDocumentElement(), new EventListener() {
                            public void onBrowserEvent(Event event) {
                                if (event.getTypeInt() == Event.ONMOUSEMOVE || event.getTypeInt() == Event.ONMOUSEUP || event.getTypeInt() == Event.ONMOUSEDOWN) {
                                    inframe = true;
                                    Event.fireNativePreviewEvent(event);
                                    inframe = false;
                                } else {
                                    GWT.log("event:" + event.getTypeInt());
                                    scrollContainer.onBrowserEvent(event);
                                }
                            }
                        });
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                if (frameError != null && !frameErrorRedirect) {
                                    frameErrorRedirect = true;
                                    setUrl(getBaseUrl() + config.getDefaultLocation());
                                } else {
                                    frameErrorRedirect = false;
                                    onGWTFrameReady(iframe);
                                }
                            }
                        });
                    } else {
                        editLinker.getMainModule().unmask();
                    }
                }
            } catch (Exception e) {
                Log.error("Error in EditFrame: " + e.getMessage(), e);
            }
        }

        private String getCurrentFrameUrl() {
            return getFrameUrl(IFrameElement.as(frame.getElement()));
        }

        @Override
        public void setUrl(String url) {
            url = getPathFromUrl(url);
            if (isValidUrl(url)) {
                if (getElement() != null) {
                    this.url = url;
                    super.setUrl(url);
                } else {
                    this.url = url;
                }
            }
        }

        public void setExternalUrl(String url) {
            if (isAttached()) {
                this.url = url;
                super.setUrl(url);
            } else {
                this.url = url;
            }
        }

        public void setForceImageRefresh(boolean forceImageRefresh) {
            this.forceImageRefresh = forceImageRefresh;
        }

        public void setForceCssRefresh(boolean forceCssRefresh) {
            this.forceCssRefresh = forceCssRefresh;
        }

        public void setForceJavascriptRefresh(boolean forceJavascriptRefresh) {
            this.forceJavascriptRefresh = forceJavascriptRefresh;
        }

        @Override
        protected void onAttach() {
            super.onAttach();
            IFrameElement iframe = IFrameElement.as(frame.getElement());
            iframe.setAttribute("frameborder", "0");
//            iframe.setAttribute("sandbox","allows-scripts");
            if (url != null && !super.getUrl().endsWith(URL.encode(url))) {
                super.setUrl(url);
            }
        }

        public final native String getFrameUrl(IFrameElement iFrameElement) /*-{
            // This is known to work on all modern browsers.
            return iFrameElement.contentWindow.location.href;
        }-*/;

        public final native String onGWTFrameReady(IFrameElement iFrameElement) /*-{
            if (iFrameElement.contentWindow.onGWTFrameLoaded != null) {
                var onFrameLoaded = iFrameElement.contentWindow.onGWTFrameLoaded;
                for (var i = 0; i < onFrameLoaded.length; i++) {
                    onFrameLoaded[i]()
                }
                iFrameElement.contentWindow.onGWTFrameLoaded = []
            }
        }-*/;


    }

    private String getPathFromUrl(String url) {
        if (url.contains("://")) {
            url = url.substring(url.indexOf("://") + 3);
            url = url.substring(url.indexOf('/'));
        }
        return url;
    }

    public String getTemplate() {
        return template;
    }



    public final int getIE10FrameTop() {
        return getIE10FrameTop(IFrameElement.as(frame.getElement()));
    }

    public final int getIE10FrameLeft(){
        return getIE10FrameLeft(IFrameElement.as(frame.getElement()));
    }

    public final native int getIE10FrameTop(IFrameElement iFrameElement) /*-{
        if (iFrameElement.contentWindow.document.documentElement != null) {
            return iFrameElement.contentWindow.document.documentElement.scrollTop;
        }
    }-*/;

    public final native int getIE10FrameLeft(IFrameElement iFrameElement) /*-{
        if (iFrameElement.contentWindow.document.documentElement != null) {
            return iFrameElement.contentWindow.document.documentElement.scrollLeft;
        }
    }-*/;

}
