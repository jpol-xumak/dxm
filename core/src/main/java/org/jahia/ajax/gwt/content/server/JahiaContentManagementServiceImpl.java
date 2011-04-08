/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.ajax.gwt.content.server;

import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;
import net.htmlparser.jericho.StartTag;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.apache.jackrabbit.core.security.PrivilegeImpl;
import org.jahia.ajax.gwt.client.data.*;
import org.jahia.ajax.gwt.client.data.acl.GWTJahiaNodeACE;
import org.jahia.ajax.gwt.client.data.acl.GWTJahiaNodeACL;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeProperty;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodePropertyType;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodePropertyValue;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeType;
import org.jahia.ajax.gwt.client.data.job.GWTJahiaJobDetail;
import org.jahia.ajax.gwt.client.data.node.*;
import org.jahia.ajax.gwt.client.data.publication.GWTJahiaPublicationInfo;
import org.jahia.ajax.gwt.client.data.seo.GWTJahiaUrlMapping;
import org.jahia.ajax.gwt.client.data.toolbar.GWTConfiguration;
import org.jahia.ajax.gwt.client.data.toolbar.GWTEditConfiguration;
import org.jahia.ajax.gwt.client.data.toolbar.GWTJahiaToolbar;
import org.jahia.ajax.gwt.client.data.toolbar.GWTManagerConfiguration;
import org.jahia.ajax.gwt.client.data.wcag.WCAGValidationResult;
import org.jahia.ajax.gwt.client.data.wcag.WCAGViolation;
import org.jahia.ajax.gwt.client.data.workflow.*;
import org.jahia.ajax.gwt.client.data.workflow.history.GWTJahiaWorkflowHistoryItem;
import org.jahia.ajax.gwt.client.service.GWTJahiaServiceException;
import org.jahia.ajax.gwt.client.service.content.ExistingFileException;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.commons.server.JahiaRemoteService;
import org.jahia.ajax.gwt.helper.*;
import org.jahia.bin.Export;
import org.jahia.bin.Jahia;
import org.jahia.bin.googledocs.GoogleDocsEditor;
import org.jahia.exceptions.JahiaException;
import org.jahia.params.ProcessingContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.googledocs.GoogleDocsService;
import org.jahia.services.googledocs.GoogleDocsService.GoogleDocsExportFormats;
import org.jahia.services.googledocs.GoogleDocsServiceFactory;
import org.jahia.services.htmlvalidator.Result;
import org.jahia.services.htmlvalidator.ValidatorResults;
import org.jahia.services.htmlvalidator.WAIValidator;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.EncryptionUtils;
import org.jahia.utils.LanguageCodeConverters;
import org.jahia.utils.i18n.JahiaResourceBundle;
import org.slf4j.Logger;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolationException;
import java.io.InputStream;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GWT server code implementation for the DMS repository services.
 *
 * @author rfelden
 * @version 5 mai 2008 - 17:23:39
 */
public class JahiaContentManagementServiceImpl extends JahiaRemoteService implements JahiaContentManagementService {

	private static final transient Logger logger = org.slf4j.LoggerFactory.getLogger(JahiaContentManagementServiceImpl.class);

    private NavigationHelper navigation;
    private ContentManagerHelper contentManager;
    private SearchHelper search;
    private PublicationHelper publication;
    private WorkflowHelper workflow;
    private VersioningHelper versioning;
    private PortletHelper portlet;
    private ContentDefinitionHelper contentDefinition;
    private ContentHubHelper contentHub;
    private PropertiesHelper properties;
    private LanguageHelper languages;
    private TemplateHelper template;
    private ImageHelper image;
    private ZipHelper zip;
    private ACLHelper acl;
    private DiffHelper diff;
    private SeoHelper seo;
    private UIConfigHelper uiConfig;
    private int sessionPollingFrequency;
    private GoogleDocsExportFormats googleDocsExportFormats;
    private GoogleDocsServiceFactory googleDocsServiceFactory;
    private CacheHelper cacheHelper;
    private SchedulerHelper schedulerHelper;
    private UIConfigHelper uiConfigHelper;

    public void setGoogleDocsServiceFactory(GoogleDocsServiceFactory googleDocsServiceFactory) {
        this.googleDocsServiceFactory = googleDocsServiceFactory;
    }

    public void setAcl(ACLHelper acl) {
        this.acl = acl;
    }

    public void setContentDefinition(ContentDefinitionHelper contentDefinition) {
        this.contentDefinition = contentDefinition;
    }

    public void setContentHub(ContentHubHelper contentHub) {
        this.contentHub = contentHub;
    }

    public void setContentManager(ContentManagerHelper contentManager) {
        this.contentManager = contentManager;
    }

    public void setPortlet(PortletHelper portlet) {
        this.portlet = portlet;
    }

    public void setNavigation(NavigationHelper navigation) {
        this.navigation = navigation;
    }

    public void setProperties(PropertiesHelper properties) {
        this.properties = properties;
    }

    public void setPublication(PublicationHelper publication) {
        this.publication = publication;
    }

    public void setWorkflow(WorkflowHelper workflow) {
        this.workflow = workflow;
    }

    public void setSearch(SearchHelper search) {
        this.search = search;
    }

    public void setLanguages(LanguageHelper languages) {
        this.languages = languages;
    }

    public void setDiff(DiffHelper diff) {
        this.diff = diff;
    }

    public void setTemplate(TemplateHelper template) {
        this.template = template;
    }

    public void setImage(ImageHelper image) {
        this.image = image;
    }

    public void setVersioning(VersioningHelper versioning) {
        this.versioning = versioning;
    }

    public void setZip(ZipHelper zip) {
        this.zip = zip;
    }

    public void setUiConfig(UIConfigHelper uiConfig) {
        this.uiConfig = uiConfig;
    }

    public void setSessionPollingFrequency(int sessionPollingFrequency) {
        this.sessionPollingFrequency = sessionPollingFrequency;
    }

    public void setCacheHelper(CacheHelper cacheHelper) {
        this.cacheHelper = cacheHelper;
    }

    public void setSchedulerHelper(SchedulerHelper schedulerHelper) {
        this.schedulerHelper = schedulerHelper;
    }

    public void setUiConfigHelper(UIConfigHelper uiConfigHelper) {
        this.uiConfigHelper = uiConfigHelper;
    }


    // ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface JahiaContentManagementServiceAsync ---------------------

    /**
     * Get ManageConfiguratin
     *
     * @param name
     * @return
     * @throws GWTJahiaServiceException
     */
    public GWTManagerConfiguration getManagerConfiguration(String name) throws GWTJahiaServiceException {
        GWTManagerConfiguration config = null;
        try {
            config = uiConfig.getGWTManagerConfiguration(getSite(), getSite(), getRemoteJahiaUser(), getLocale(), getUILocale(),
                    getRequest(), name);
            config.setSiteNode(navigation.getGWTJahiaNode(getSite(), GWTJahiaNode.DEFAULT_SITE_FIELDS));
            setAvailablePermissions(config);
        }catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
        return config;
    }

    /**
     * Get edit configuration
     *
     * @return
     * @throws GWTJahiaServiceException
     */
    public GWTEditConfiguration getEditConfiguration(String path, String name) throws GWTJahiaServiceException {
        GWTEditConfiguration config = null;
        try {
            JCRSessionWrapper session = retrieveCurrentSession();
            config = uiConfig.getGWTEditConfiguration(session.getNode(path), getSite(), getRemoteJahiaUser(), getLocale(), getUILocale(),
                    getRequest(), name);
            config.setSiteNode(navigation.getGWTJahiaNode(getSite(), GWTJahiaNode.DEFAULT_SITE_FIELDS));

            List<GWTJahiaNode> sites = getRoot(Arrays.asList(config.getSitesLocation()), Arrays.asList("jnt:virtualsite"), null, null, GWTJahiaNode.DEFAULT_SITE_FIELDS, null, null, false);
            String permission = name.equals("editmode") ? "editModeAccess" : "studioModeAccess";
            Map<String, GWTJahiaNode> sitesMap = new HashMap<String, GWTJahiaNode>();
            for (GWTJahiaNode site : sites) {
                if (session.getNodeByUUID(site.getUUID()).hasPermission(permission)) {
                sitesMap.put(site.getSiteUUID(), site);
            }
            }
            config.setSitesMap(sitesMap);

            setAvailablePermissions(config);
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
        return config;
    }


    private void setAvailablePermissions(GWTConfiguration config) throws RepositoryException, GWTJahiaServiceException {
        Privilege[] p = new JahiaPrivilegeRegistry(retrieveCurrentSession().getWorkspace().getNamespaceRegistry()).getRegisteredPrivileges();
        List<String> l = new ArrayList<String>();
        for (Privilege privilege : p) {
            l.add(((PrivilegeImpl)privilege).getPrefixedName());
        }
        config.setPermissions(l);
    }

    public BasePagingLoadResult<GWTJahiaNode> lsLoad(GWTJahiaNode parentNode, List<String> nodeTypes, List<String> mimeTypes,
                                                     List<String> filters, List<String> fields, boolean checkSubChild, int limit, int offset)
            throws GWTJahiaServiceException {
        List<GWTJahiaNode> filteredList = new ArrayList<GWTJahiaNode>();
        for (GWTJahiaNode n : navigation
                .ls(parentNode, nodeTypes, mimeTypes, filters, fields, checkSubChild, retrieveCurrentSession())) {
            if (n.isMatchFilters()) {
                filteredList.add(n);
            }
        }
        final int length = filteredList.size();
        if (offset > -1 && limit > 0) {
            if (offset >= length) {
                filteredList = new ArrayList<GWTJahiaNode>();
            } else {
                filteredList = new ArrayList<GWTJahiaNode>(filteredList.subList(offset, Math.min(length-1,offset+limit)));
            }
        }
        return new BasePagingLoadResult<GWTJahiaNode>(filteredList, offset, length);
    }

    public List<GWTJahiaNode> getRoot(List<String> paths, List<String> nodeTypes, List<String> mimeTypes,
                                      List<String> filters, List<String> fields, List<String> selectedNodes,
                                      List<String> openPaths) throws GWTJahiaServiceException {
        if (openPaths == null || openPaths.size() == 0) {
            openPaths = getOpenPathsForRepository(paths.toString());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("retrieving open paths for " + paths + " :\n" + openPaths);
        }

        return navigation
                .retrieveRoot(paths, nodeTypes, mimeTypes, filters, fields, selectedNodes, openPaths, getSite(),
                        retrieveCurrentSession(), getLocale());
    }

    public List<GWTJahiaNode> getRoot(List<String> paths, List<String> nodeTypes, List<String> mimeTypes,
                                      List<String> filters, List<String> fields, List<String> selectedNodes,
                                      List<String> openPaths, boolean checkSubChild) throws GWTJahiaServiceException {
        if (openPaths == null || openPaths.size() == 0) {
            openPaths = getOpenPathsForRepository(paths.toString());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("retrieving open paths for " + paths + " :\n" + openPaths);
        }

        return navigation
                .retrieveRoot(paths, nodeTypes, mimeTypes, filters, fields, selectedNodes, openPaths, getSite(),
                        retrieveCurrentSession(), getLocale(), checkSubChild);
    }

    public List<GWTJahiaNode> getNodes(List<String> paths, List<String> fields) {
        List<GWTJahiaNode> list = new ArrayList<GWTJahiaNode>();
        for (String path : paths) {
            try {
                GWTJahiaNode gwtJahiaNode = navigation.getNode(path, fields, retrieveCurrentSession());
                list.add(gwtJahiaNode);
            } catch (GWTJahiaServiceException e) {
                logger.debug(e.getMessage(), e);
            }
        }
        return list;

    }

    /**
     * Get a node by its path if existing.
     *
     * @param path path o fthe node you want
     * @return the founded node if existing
     * @throws org.jahia.ajax.gwt.client.service.GWTJahiaServiceException
     *          if node does not exist
     */
    public GWTJahiaNode getNode(String path) throws GWTJahiaServiceException {
        return navigation.getNode(path, retrieveCurrentSession());
    }

    /**
     * Get nodes tag by name
     *
     * @param tagName
     * @param create
     * @return
     * @throws GWTJahiaServiceException
     */
    public GWTJahiaNode getTagNode(String tagName, boolean create) throws GWTJahiaServiceException {
        final String s = tagName.trim();
        GWTJahiaNode tagNode = navigation.getTagNode(s, getSite());
        if (tagNode == null && create) {
            return createNode(navigation.getTagsNode(getSite()).getPath(), s, "jnt:tag", null, null,
                    new ArrayList<GWTJahiaNodeProperty>(), null);
        } else {
            return tagNode;
        }
    }


    public void saveOpenPathsForRepository(String repositoryType, List<String> paths) throws GWTJahiaServiceException {
        getSession().setAttribute(NavigationHelper.SAVED_OPEN_PATHS + repositoryType, paths);
    }

    public BasePagingLoadResult<GWTJahiaNode> search(GWTJahiaSearchQuery searchQuery, int limit, int offset)
            throws GWTJahiaServiceException {
        // To do: find a better war to handle total size
        List<GWTJahiaNode> result = search.search(searchQuery, 0, 0, retrieveCurrentSession());
        int size = result.size();
        result = new ArrayList<GWTJahiaNode>(result.subList(offset, Math.min(size, offset + limit)));
        return new BasePagingLoadResult<GWTJahiaNode>(result, offset, size);
    }


    public List<GWTJahiaNode> search(String searchString, int limit, List<String> nodeTypes, List<String> mimeTypes,
                                     List<String> filters) throws GWTJahiaServiceException {
        return search.search(searchString, limit, nodeTypes, mimeTypes, filters, retrieveCurrentSession());
    }

    public List<GWTJahiaNode> searchSQL(String searchString, int limit, List<String> nodeTypes, List<String> mimeTypes,
                                        List<String> filters, List<String> fields,boolean sortOnDisplayName) throws GWTJahiaServiceException {
        List<GWTJahiaNode> gwtJahiaNodes = search.searchSQL(searchString, limit, nodeTypes, mimeTypes, filters, fields,
                retrieveCurrentSession());
        if (sortOnDisplayName) {
            final Collator collator = Collator.getInstance(retrieveCurrentSession().getLocale());
            Collections.sort(gwtJahiaNodes, new Comparator<GWTJahiaNode>() {
                public int compare(GWTJahiaNode o1, GWTJahiaNode o2) {
                    return collator.compare(o1.getDisplayName(), o2.getDisplayName());
                }
            });
        }
        return gwtJahiaNodes;
    }

    public List<GWTJahiaPortletDefinition> searchPortlets(String match) throws GWTJahiaServiceException {
        try {
            return portlet.searchPortlets(match, getRemoteJahiaUser(), getLocale());
        } catch (Exception e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public List<GWTJahiaNode> getSavedSearch() throws GWTJahiaServiceException {
        return search.getSavedSearch(retrieveCurrentSession());
    }

    public void saveSearch(GWTJahiaSearchQuery searchQuery, String path, String name, boolean onTopOf)
            throws GWTJahiaServiceException {
        if (onTopOf) {
            final GWTJahiaNode parentNode = navigation.getParentNode(path, retrieveCurrentSession());
            final GWTJahiaNode jahiaNode =
                    search.saveSearch(searchQuery, parentNode.getPath(), name, retrieveCurrentSession());
            try {
                contentManager.moveOnTopOf(jahiaNode.getPath(), path, retrieveCurrentSession());
            } catch (RepositoryException e) {
                throw new GWTJahiaServiceException(e.getMessage());
            }
        } else {
            search.saveSearch(searchQuery, path, name, retrieveCurrentSession());
        }
    }

    public void mount(String path, String target, String root) throws GWTJahiaServiceException {
        contentHub.mount(target, root, getUser());
    }

    public List<GWTJahiaNode> getMountpoints() throws GWTJahiaServiceException {
        return navigation.getMountpoints(retrieveCurrentSession());
    }

    public void storePasswordForProvider(String providerKey, String username, String password) {
        contentHub.storePasswordForProvider(getUser(), providerKey, username, password);
    }

    public Map<String, String> getStoredPasswordsProviders() {
        return contentHub.getStoredPasswordsProviders(getUser());
    }

    public void setLock(List<String> paths, boolean locked) throws GWTJahiaServiceException {
        contentManager.setLock(paths, locked, retrieveCurrentSession());
    }

    public void deletePaths(List<String> paths) throws GWTJahiaServiceException {
        contentManager.deletePaths(paths, getUser(), retrieveCurrentSession());
    }

    public String getAbsolutePath(String path) throws GWTJahiaServiceException {
        return navigation.getAbsolutePath(path, retrieveCurrentSession(), getRequest());
    }

    public void checkWriteable(List<String> paths) throws GWTJahiaServiceException {
        contentManager.checkWriteable(paths, getUser(), retrieveCurrentSession());
    }

    public void paste(List<String> pathsToCopy, String destinationPath, String newName, boolean cut)
            throws GWTJahiaServiceException {
        contentManager
                .copy(pathsToCopy, destinationPath, newName, false, cut, false, false, retrieveCurrentSession(null));
    }

    public void pasteReferences(List<String> pathsToCopy, String destinationPath, String newName)
            throws GWTJahiaServiceException {
        contentManager
                .copy(pathsToCopy, destinationPath, newName, false, false, true, false, retrieveCurrentSession(null));
    }

    public GWTJahiaGetPropertiesResult getProperties(String path, String langCode) throws GWTJahiaServiceException {
        if (langCode == null) {
            return getProperties(path, getLocale());
        }
        return getProperties(path, LanguageCodeConverters.getLocaleFromCode(langCode));
    }


    /**
     * Get GWTJahiaGetPropertiesResult
     *
     * @param path
     * @param locale
     * @return
     * @throws GWTJahiaServiceException
     */
    private GWTJahiaGetPropertiesResult getProperties(String path, Locale locale) throws GWTJahiaServiceException {
        final GWTJahiaNode node = navigation.getNode(path, retrieveCurrentSession());
        final HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
            JCRNodeWrapper nodeWrapper = sessionWrapper.getNode(node.getPath());
            map.put("contextNode", nodeWrapper);
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
        }

        // get node type
        final List<GWTJahiaNodeType> nodeTypes = contentDefinition.getNodeTypes(node.getNodeTypes(), getUILocale());

        // get properties
        final Map<String, GWTJahiaNodeProperty> props = properties.getProperties(path, retrieveCurrentSession(locale));

        final GWTJahiaGetPropertiesResult result = new GWTJahiaGetPropertiesResult(nodeTypes, props);
        result.setNode(node);
        result.setAvailabledLanguages(languages.getLanguages(getSite(), getRemoteJahiaUser(), getLocale()));
        result.setCurrentLocale(languages.getCurrentLang(getLocale()));
        return result;
    }

    /**
     * Save properties of for the given nodes
     *
     *
     * @param nodes
     * @param newProps
     * @param removedTypes
     * @throws GWTJahiaServiceException
     */
    public void saveProperties(List<GWTJahiaNode> nodes, List<GWTJahiaNodeProperty> newProps, Set<String> removedTypes)
            throws GWTJahiaServiceException {
        properties.saveProperties(nodes, newProps, removedTypes, getRemoteJahiaUser(), retrieveCurrentSession());
    }

    /**
     * Save properties by langCode
     *
     *
     * @param nodes
     * @param newProps
     * @param removedTypes
     *@param locale  @throws GWTJahiaServiceException
     */
    private void saveProperties(List<GWTJahiaNode> nodes, List<GWTJahiaNodeProperty> newProps, Set<String> removedTypes, String locale)
            throws GWTJahiaServiceException {
        properties.saveProperties(nodes, newProps, removedTypes, getRemoteJahiaUser(),
                retrieveCurrentSession(LanguageCodeConverters.languageCodeToLocale(locale)));
    }

    /**
     * Save properties and acl for the given nodes
     *
     *
     * @param nodes
     * @param acl
     * @param removedTypes
     * @throws GWTJahiaServiceException
     */
    public void savePropertiesAndACL(List<GWTJahiaNode> nodes, GWTJahiaNodeACL acl,
                                     Map<String, List<GWTJahiaNodeProperty>> langCodeProperties,
                                     List<GWTJahiaNodeProperty> sharedProperties, Set<String> removedTypes) throws GWTJahiaServiceException {
        Iterator<String> langCode = langCodeProperties.keySet().iterator();
        // save properties per lang
        while (langCode.hasNext()) {
            String currentLangCode = langCode.next();
            List<GWTJahiaNodeProperty> props = langCodeProperties.get(currentLangCode);
            saveProperties(nodes, props, removedTypes, currentLangCode);
        }

        // save shared properties
        saveProperties(nodes, sharedProperties,removedTypes);

        // save acl
        if (acl != null) {
            for (GWTJahiaNode node : nodes) {
                contentManager.setACL(node.getPath(), acl, retrieveCurrentSession());
            }
        }

    }

    /**
     * Save node with properties, acl and new ordered children
     *
     *
     *
     * @param node
     * @param orderedChildrenNode
     * @param acl
     * @param langCodeProperties
     * @param sharedProperties
     * @param removedTypes
     * @throws GWTJahiaServiceException
     */
    public void saveNode(GWTJahiaNode node, List<GWTJahiaNode> orderedChildrenNode, GWTJahiaNodeACL acl,
                         Map<String, List<GWTJahiaNodeProperty>> langCodeProperties,
                         List<GWTJahiaNodeProperty> sharedProperties, Set<String> removedTypes) throws GWTJahiaServiceException {
        closeEditEngine(node.getPath());

        final JCRSessionWrapper jcrSessionWrapper = retrieveCurrentSession();

        try {
            JCRNodeWrapper nodeWrapper = jcrSessionWrapper.getNodeByUUID(node.getUUID());
            if (!nodeWrapper.getName().equals(node.getName())) {
                String name = contentManager.findAvailableName(nodeWrapper.getParent(), node.getName());
                nodeWrapper.rename(name);
                jcrSessionWrapper.save();
                node.setName(name);
                node.setPath(nodeWrapper.getPath());
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e);
        }

//        setLock(Arrays.asList(node.getPath()), false);
        Iterator<String> langCode = langCodeProperties.keySet().iterator();

        // save shared properties
        saveProperties(Arrays.asList(node), sharedProperties, removedTypes);

        // save properties per lang
        while (langCode.hasNext()) {
            String currentLangCode = langCode.next();
            List<GWTJahiaNodeProperty> props = langCodeProperties.get(currentLangCode);
            saveProperties(Arrays.asList(node), props, removedTypes, currentLangCode);
        }

        // save children orders
        contentManager.updateChildren(node, orderedChildrenNode, jcrSessionWrapper);


        // save acl
        if (acl != null) {
            contentManager.setACL(node.getPath(), acl, jcrSessionWrapper);
        }

        if (node.get("vanityMappings") != null) {
            saveUrlMappings(node, langCodeProperties.keySet(), (List<GWTJahiaUrlMapping>) node.get("vanityMappings"));
        }
        if (node.get("activeWorkflows") != null) {
            workflow.updateWorkflowRules(node,
                    (Set<GWTJahiaWorkflowDefinition>) node.get("activeWorkflows"), jcrSessionWrapper);
        }
    }

    /**
     * Create node with multilangue
     *
     * @param parentPath
     * @param name
     * @param nodeType
     * @param mixin
     * @param acl
     * @param props
     * @param langCodeProperties
     * @return
     * @throws GWTJahiaServiceException
     */
    public GWTJahiaNode createNode(String parentPath, String name, String nodeType, List<String> mixin,
                                   GWTJahiaNodeACL acl, List<GWTJahiaNodeProperty> props,
                                   Map<String, List<GWTJahiaNodeProperty>> langCodeProperties)
            throws GWTJahiaServiceException {
        if (name == null) {
            if (langCodeProperties != null) {
                String defaultLanguage = getLocale().toString();
                if (getSite() != null) {
                    defaultLanguage = getSite().getDefaultLanguage();
                }
                List<GWTJahiaNodeProperty> l = langCodeProperties.get(defaultLanguage);
                if (l == null && langCodeProperties.size() > 0) {
                    l = langCodeProperties.values().iterator().next();
                }
                if (l != null) {
                    name = contentManager.generateNameFromTitle(l);
                }
            }
        }

        GWTJahiaNode res;

        final JCRSessionWrapper jcrSessionWrapper = retrieveCurrentSession();
        res = contentManager.createNode(parentPath, name, nodeType, mixin, props, jcrSessionWrapper);

        GWTJahiaNode node = res;

        if (acl != null) {
            contentManager.setACL(res.getPath(), acl, jcrSessionWrapper);
        }

        try {
            jcrSessionWrapper.save();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException("Node creation failed. Cause: " + e.getMessage());
        }

        // save shared properties
        if (langCodeProperties != null && !langCodeProperties.isEmpty()) {
            List<GWTJahiaNode> nodes = new ArrayList<GWTJahiaNode>();
            nodes.add(node);
            Iterator<String> langCode = langCodeProperties.keySet().iterator();
            // save properties per lang
            while (langCode.hasNext()) {
                String currentLangCode = langCode.next();
                List<GWTJahiaNodeProperty> properties = langCodeProperties.get(currentLangCode);
                saveProperties(nodes, properties, null, currentLangCode);
            }
        }
        try {
            jcrSessionWrapper.save();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException("Node creation failed. Cause: " + e.getMessage());
        }

        return node;
    }

    /**
     * Move and create node
     *
     * @param path
     * @param name
     * @param nodeType
     * @param mixin
     * @param acl
     * @param properties
     * @throws GWTJahiaServiceException
     */
    public GWTJahiaNode createNodeAndMoveBefore(String path, String name, String nodeType, List<String> mixin,
                                                GWTJahiaNodeACL acl, List<GWTJahiaNodeProperty> properties,
                                                Map<String, List<GWTJahiaNodeProperty>> langCodeProperties)
            throws GWTJahiaServiceException {

        final GWTJahiaNode parentNode = navigation.getParentNode(path, retrieveCurrentSession());

        GWTJahiaNode jahiaNode =
                createNode(parentNode.getPath(), name, nodeType, mixin, acl, properties, langCodeProperties);

        try {
            contentManager.moveOnTopOf(jahiaNode.getPath(), path, retrieveCurrentSession());
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }

        try {
            retrieveCurrentSession().save();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException("Node creation failed. Cause: " + e.getMessage());
        }

        return jahiaNode;
    }

    /**
     * Create Folder
     *
     * @param parentPath
     * @param name
     * @throws GWTJahiaServiceException
     */
    public void createFolder(String parentPath, String name) throws GWTJahiaServiceException {
        contentManager.createFolder(parentPath, name, retrieveCurrentSession());
    }

    public GWTJahiaNode createPortletInstance(String path, GWTJahiaNewPortletInstance wiz)
            throws GWTJahiaServiceException {
        return portlet.createPortletInstance(path, wiz, retrieveCurrentSession());
    }

    public GWTJahiaNode createRSSPortletInstance(String path, String name, String url) throws GWTJahiaServiceException {
        return portlet.createRSSPortletInstance(path, name, url, getSite(), retrieveCurrentSession());
    }

    public GWTJahiaNode createGoogleGadgetPortletInstance(String path, String name, String script)
            throws GWTJahiaServiceException {
        return portlet.createGoogleGadgetPortletInstance(path, name, script, getSite(), retrieveCurrentSession());
    }

    public void checkExistence(String path) throws GWTJahiaServiceException {
        if (contentManager.checkExistence(path, retrieveCurrentSession())) {
            throw new ExistingFileException(path);
        }
    }

    public void rename(String path, String newName) throws GWTJahiaServiceException {
        contentManager.rename(path, newName, retrieveCurrentSession());
    }

    public void move(String sourcePath, String targetPath) throws GWTJahiaServiceException {
        try {
            contentManager.move(sourcePath, targetPath, retrieveCurrentSession());
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void moveAtEnd(String sourcePath, String targetPath) throws GWTJahiaServiceException {
        try {
            contentManager.moveAtEnd(sourcePath, targetPath, retrieveCurrentSession());
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void moveOnTopOf(String sourcePath, String targetPath) throws GWTJahiaServiceException {
        try {
            contentManager.moveOnTopOf(sourcePath, targetPath, retrieveCurrentSession());
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public GWTJahiaNodeACE createDefaultUsersGroupACE(List<String> permissions, boolean grand)
            throws GWTJahiaServiceException {
        return acl.createUsersGroupACE(permissions, grand, getSite());
    }

    public List<GWTJahiaNodeUsage> getUsages(List<String> paths) throws GWTJahiaServiceException {
        return navigation.getUsages(paths, retrieveCurrentSession());
    }

    public List<GWTJahiaNode> getNodesByCategory(GWTJahiaNode category) throws GWTJahiaServiceException {
        return navigation.getNodesByCategory(category.getPath(), retrieveCurrentSession());
    }

    public BasePagingLoadResult<GWTJahiaNode> getNodesByCategory(GWTJahiaNode category, int limit, int offset)
            throws GWTJahiaServiceException {
        // ToDo: handle pagination directly in the jcr
        final List<GWTJahiaNode> result = getNodesByCategory(category);
        int size = result.size();
        new ArrayList<GWTJahiaNode>(result.subList(offset, Math.min(size, offset + limit)));
        return new BasePagingLoadResult<GWTJahiaNode>(result, offset, size);
    }


    public void zip(List<String> paths, String archiveName) throws GWTJahiaServiceException {
        zip.zip(paths, archiveName, retrieveCurrentSession());
    }

    public void unzip(List<String> paths) throws GWTJahiaServiceException {
        zip.unzip(paths, false, retrieveCurrentSession());
    }

    public String getExportUrl(String path) throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper jcrSessionWrapper = JCRSessionFactory.getInstance().getCurrentUserSession();
            return Jahia.getContextPath() + Export.getExportServletPath() + "/" +
                    jcrSessionWrapper.getWorkspace().getName() + path;
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void cropImage(String path, String target, int top, int left, int width, int height, boolean forceReplace)
            throws GWTJahiaServiceException {
        JCRSessionWrapper session = retrieveCurrentSession();
        image.crop(path, target, top, left, width, height, forceReplace, session);
    }

    public void resizeImage(String path, String target, int width, int height, boolean forceReplace)
            throws GWTJahiaServiceException {
        JCRSessionWrapper session = retrieveCurrentSession();
        image.resizeImage(path, target, width, height, forceReplace, session);
    }

    public void rotateImage(String path, String target, boolean clockwise, boolean forceReplace)
            throws GWTJahiaServiceException {
        JCRSessionWrapper session = retrieveCurrentSession();
        image.rotateImage(path, target, clockwise, forceReplace, session);
    }

    public void activateVersioning(List<String> path) throws GWTJahiaServiceException {
        versioning.activateVersioning(path, retrieveCurrentSession());
    }

    public List<GWTJahiaNodeVersion> getVersions(String path) throws GWTJahiaServiceException {
        try {
            JCRNodeWrapper node = JCRSessionFactory.getInstance().getCurrentUserSession().getNode(path);
            List<GWTJahiaNodeVersion> versions = navigation.getVersions(node,!node.isNodeType("nt:file"));
            sortVersions(versions);
            return versions;
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    private void sortVersions(List<GWTJahiaNodeVersion> versions) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Collections.sort(versions, new Comparator<GWTJahiaNodeVersion>() {
            public int compare(GWTJahiaNodeVersion o1, GWTJahiaNodeVersion o2) {
                String[] strings1 = o1.getLabel().split("_at_");
                String[] strings2 = o2.getLabel().split("_at_");
                if (strings1.length == 2 && strings2.length == 2) {
                    try {

                        Date dateV2 = simpleDateFormat.parse(strings2[1]);
                        o2.setDate(dateV2);
                        Date dateV1 = simpleDateFormat.parse(strings1[1]);
                        o1.setDate(dateV1);
                        return dateV2.compareTo(dateV1);
                    } catch (ParseException e) {
                        return o1.getLabel().compareTo(o2.getLabel());
                    }
                } else {
                    return o1.getLabel().compareTo(o2.getLabel());
                }
            }
        });
    }

    public BasePagingLoadResult<GWTJahiaNodeVersion> getVersions(GWTJahiaNode node, int limit, int offset)
            throws GWTJahiaServiceException {
        try {
            JCRNodeWrapper nodeWrapper = JCRSessionFactory.getInstance().getCurrentUserSession(getWorkspace(), getLocale()).getNode(
                    node.getPath());
            List<GWTJahiaNodeVersion> result = navigation.getVersions(
                    nodeWrapper, true);
            sortVersions(result);
            // add current workspace version
            final GWTJahiaNodeVersion liveVersion = new GWTJahiaNodeVersion("live", node);
            result.add(0, liveVersion);
            liveVersion.setUrl(navigation.getNodeURL(null, nodeWrapper, null, null, "live", getLocale()));

            final GWTJahiaNodeVersion defaultVersion = new GWTJahiaNodeVersion("default", node);
            result.add(0, defaultVersion);
            defaultVersion.setUrl(navigation.getNodeURL(null, nodeWrapper, null, null, "default", getLocale()));

            // get sublist: Todo Find a better way
            int size = result.size();
            result = new ArrayList<GWTJahiaNodeVersion>(result.subList(offset, Math.min(size, offset + limit)));
            return new BasePagingLoadResult<GWTJahiaNodeVersion>(result, offset, size);
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void restoreNode(GWTJahiaNodeVersion gwtJahiaNodeVersion, boolean allSubTree)
            throws GWTJahiaServiceException {
        String nodeUuid = gwtJahiaNodeVersion.getNode().getUUID();
        // restore by label
        versioning.restoreVersionLabel(nodeUuid, gwtJahiaNodeVersion.getDate(), gwtJahiaNodeVersion.getLabel(),
                allSubTree, retrieveCurrentSession());
    }

    public void restoreNodeByIdentifierAndDate(String identifier, Date versionDate, String versionLabel, boolean allSubTree)
            throws GWTJahiaServiceException {
        // restore by label
        versioning.restoreVersionLabel(identifier, versionDate, versionLabel, allSubTree, retrieveCurrentSession());
    }

    public void uploadedFile(List<String[]> uploadeds) throws GWTJahiaServiceException {
        for (String[] uploaded : uploadeds) {
            contentManager.uploadedFile(uploaded[0], uploaded[1], Integer.parseInt(uploaded[2]), uploaded[3],
                    retrieveCurrentSession());
        }
    }

    public GWTRenderResult getRenderedContent(String path, String workspace, String locale, String template,
                                              String configuration, Map<String, String> contextParams, boolean editMode,
                                              String configName) throws GWTJahiaServiceException {
        Locale localValue = getLocale();
        if (locale != null) {
            localValue = LanguageCodeConverters.languageCodeToLocale(locale);
            if (!getLocale().equals(localValue)) {
                getSession().setAttribute(ProcessingContext.SESSION_LOCALE, localValue);
            }
        }
        return this.template
                .getRenderedContent(path, template, configuration, contextParams, editMode, configName, getRequest(),
                        getResponse(), retrieveCurrentSession(workspace, localValue, true));
    }

    public String getNodeURL(String servlet, String path, Date versionDate, String versionLabel, String workspace,
                             String locale) throws GWTJahiaServiceException {
        final JCRSessionWrapper session = retrieveCurrentSession(workspace != null ? workspace : getWorkspace(),
                locale != null ? LanguageCodeConverters.languageCodeToLocale(locale) : getLocale(), false);
        try {
            JCRNodeWrapper node = session.getNode(path);

            return getResponse().encodeURL(this.navigation.getNodeURL(servlet, node, versionDate, versionLabel, session.getWorkspace().getName(),
                    session.getLocale()));
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e);
        }
    }

    public void importContent(String parentPath, String fileKey, Boolean asynchronously)
            throws GWTJahiaServiceException {
        contentManager.importContent(parentPath, fileKey, asynchronously);
    }

    public Map<String,GWTJahiaWorkflowDefinition> getWorkflowDefinitions(List<String> workflowDefinitionIds) throws GWTJahiaServiceException {
        Map<String,GWTJahiaWorkflowDefinition> l = new HashMap<String,GWTJahiaWorkflowDefinition>();
        for (String wf : workflowDefinitionIds) {
            l.put(wf, workflow.getGWTJahiaWorkflowDefinition(wf, getUILocale()));
        }
        return l;
    }

    public void startWorkflow(String path, GWTJahiaWorkflowDefinition workflowDefinition,
                              List<GWTJahiaNodeProperty> properties, List<String> comments) throws GWTJahiaServiceException {
        workflow.startWorkflow(path, workflowDefinition, retrieveCurrentSession(), properties, comments);
    }

    public void startWorkflow(List<String> uuids, GWTJahiaWorkflowDefinition def,
                              List<GWTJahiaNodeProperty> properties, List<String> comments, Map<String, Object> args) throws GWTJahiaServiceException {
        workflow.startWorkflow(uuids, def, retrieveCurrentSession(), properties, comments, args);
    }


    public void assignAndCompleteTask(GWTJahiaWorkflowTask task, GWTJahiaWorkflowOutcome outcome,
                                      List<GWTJahiaNodeProperty> properties) throws GWTJahiaServiceException {
        workflow.assignAndCompleteTask(task, outcome, retrieveCurrentSession(), properties);
    }

    public List<GWTJahiaWorkflowComment> addCommentToWorkflow(GWTJahiaWorkflow wf, String comment) {
        this.workflow.addCommentToWorkflow(wf, getUser(), comment, getLocale());
        return getWorkflowComments(wf);
    }

    public List<GWTJahiaWorkflowComment> getWorkflowComments(GWTJahiaWorkflow workflow) {
        return this.workflow.getWorkflowComments(workflow, getLocale());
    }

    public List<GWTJahiaWorkflowHistoryItem> getWorkflowHistoryForUser() throws GWTJahiaServiceException {
        List<GWTJahiaWorkflowHistoryItem> res = workflow.getWorkflowHistoryForUser(getUser(), getLocale());

        JCRSessionWrapper session = retrieveCurrentSession();

//        for (GWTJahiaWorkflowHistoryItem jahiaWorkflow : res) {
//            try {
//                JCRNodeWrapper node =
//                        session.getNodeByUUID(((GWTJahiaWorkflowHistoryProcess) jahiaWorkflow).getNodeId());
//                GWTJahiaNode gwtJahiaNode = navigation.getGWTJahiaNode(node, new ArrayList<String>());
//                jahiaWorkflow.set("node", gwtJahiaNode);
//            } catch (RepositoryException e) {
//                logger.error(e.getMessage(), e);
//            }
//        }

        return res;
    }

    /**
     * Publish the specified uuids.
     *
     * @param uuids the list of node uuids to publish, will not auto publish the parents
     * @throws GWTJahiaServiceException
     */
    public void publish(List<String> uuids,
                        List<GWTJahiaNodeProperty> properties, List<String> comments) throws GWTJahiaServiceException {
        publication.publish(uuids, retrieveCurrentSession(), properties, comments);
    }

    /**
     * Unpublish the specified path and its subnodes.
     *
     * @param paths the path to unpublish, will not unpublish the references
     * @throws GWTJahiaServiceException
     */
    public void unpublish(List<String> paths) throws GWTJahiaServiceException {
        long l = System.currentTimeMillis();
        JCRSessionWrapper session = retrieveCurrentSession();
        for (String s : paths) {
            try {
                JCRNodeWrapper node = session.getNode(s);
                publication.unpublish(node.getIdentifier(), Collections.singleton(session.getLocale().toString()),
                        session.getUser());
            } catch (RepositoryException e) {
                throw new GWTJahiaServiceException(e.getMessage());
            }
        }
        logger.debug("-->" + (System.currentTimeMillis() - l));
    }

    /**
     * Get the publication status information for multiple pathes.
     *
     * @param uuids path to get publication info from
     * @return a GWTJahiaPublicationInfo object filled with the right status for the publication state of this path
     * @throws GWTJahiaServiceException
     */
    public List<GWTJahiaPublicationInfo> getPublicationInfo(List<String> uuids, boolean allSubTree)
            throws GWTJahiaServiceException {
        final JCRSessionWrapper session = retrieveCurrentSession();
        List<GWTJahiaPublicationInfo> all = publication
                .getFullPublicationInfos(uuids, Collections.singleton(session.getLocale().toString()), session, allSubTree);

        return all;
    }


    /**
     * Get worflow info by path
     *
     * @param path
     * @return
     * @throws GWTJahiaServiceException
     */
    public GWTJahiaWorkflowInfo getWorkflowInfo(String path) throws GWTJahiaServiceException {
        return workflow.getWorkflowInfo(path, retrieveCurrentSession(), getLocale());
    }

    /**
     * Get site languages
     *
     * @return
     * @throws GWTJahiaServiceException
     */
    public List<GWTJahiaLanguage> getSiteLanguages() throws GWTJahiaServiceException {
        return languages.getLanguages(getSite(), getRemoteJahiaUser(), getLocale());
    }

    /**
     * Gwt Highlighted
     *
     * @param original
     * @param amendment
     * @return
     */
    public String getHighlighted(String original, String amendment) {
        return diff.getHighlighted(original, amendment);
    }


    private List<String> getOpenPathsForRepository(String repositoryType) throws GWTJahiaServiceException {
        return (List<String>) getSession().getAttribute(navigation.SAVED_OPEN_PATHS + repositoryType);
    }

    private JahiaUser getUser() {
        return getRemoteJahiaUser();
    }

    /* (non-Javadoc)
     * @see org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService#getUrlMappings(org.jahia.ajax.gwt.client.data.node.GWTJahiaNode, java.lang.String)
     */

    public List<GWTJahiaUrlMapping> getUrlMappings(GWTJahiaNode node, String locale) throws GWTJahiaServiceException {
        try {
            return seo.getUrlMappings(node, locale, retrieveCurrentSession());
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService#saveUrlMappings(org.jahia.ajax.gwt.client.data.node.GWTJahiaNode, java.util.Set, java.util.List)
     */

    public void saveUrlMappings(GWTJahiaNode node, Set<String> updatedLocales, List<GWTJahiaUrlMapping> mappings)
            throws GWTJahiaServiceException {
        try {
            seo.saveUrlMappings(node, updatedLocales, mappings, retrieveCurrentSession());
        } catch (ConstraintViolationException e) {
            // TODO handle it and display correct messages for the user
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void deployTemplates(String templatesPath, String sitePath) throws GWTJahiaServiceException {
    	logger.info("Deploying templates {} to the target {}", templatesPath, sitePath);
        contentManager.deployTemplates(templatesPath, sitePath, retrieveCurrentSession());
    	logger.info("...template deployment done.");
    }

    public GWTJahiaNode createTemplateSet(String key, String baseSet, String siteType) throws GWTJahiaServiceException {
        try {
            return contentManager.createTemplateSet(key, baseSet, siteType, retrieveCurrentSession());
        } catch (Exception e) {
            logger.error("", e);
            throw new GWTJahiaServiceException(e.toString());
        }
    }

    public GWTJahiaNode generateWar(String moduleName) throws GWTJahiaServiceException {
        try {
            GWTJahiaNode node = contentManager.generateWar(moduleName, retrieveCurrentSession());

            return node;
        } catch (Exception e) {
            logger.error("", e);
            throw new GWTJahiaServiceException(e.toString());
        }
    }


    /**
     * @param seoHelper the seoHelper to set
     */
    public void setSeo(SeoHelper seoHelper) {
        this.seo = seoHelper;
    }

    public List<GWTJahiaWorkflowHistoryItem> getWorkflowHistoryProcesses(String nodeId, String locale)
            throws GWTJahiaServiceException {
        return workflow.getWorkflowHistoryProcesses(nodeId,
                retrieveCurrentSession(LanguageCodeConverters.languageCodeToLocale(locale)), getUILocale());
    }

    public List<GWTJahiaWorkflowHistoryItem> getWorkflowHistoryTasks(String provider, String processId, String locale)
            throws GWTJahiaServiceException {
        return workflow.getWorkflowHistoryTasks(provider, processId, getUILocale());
    }

    public Integer isValidSession() throws GWTJahiaServiceException {
        // >0 : shedule poll repeating for this value
        // 0 : session expire
        // <0 : polling desactivated
        final HttpSession session = getRequest().getSession(false);
        if (session != null) {
            Long date = (Long) session.getAttribute("lastPoll");
            long lastAccessed = session.getLastAccessedTime();
            long now = System.currentTimeMillis();
            if (date != null && (date / 1000 == lastAccessed / 1000)) {
                // last call was (probably) a poll call
                long first = (Long) session.getAttribute("firstPoll");
                logger.debug("Inactive since : " + (now - first));
                if (now - first < session.getMaxInactiveInterval() * 1000) {
                    session.setMaxInactiveInterval(session.getMaxInactiveInterval() - (int) ((now - first) / 1000));
                } else {
                    session.invalidate();
                }
            } else {
                session.setAttribute("firstPoll", now);
            }

            session.setAttribute("lastPoll", now);
            return sessionPollingFrequency;
        } else {
            return 0;
        }
    }

    public GWTJahiaCreateEngineInitBean initializeCreateEngine(String typename, String parentpath)
            throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
            JCRNodeWrapper parent = sessionWrapper.getNode(parentpath);

            GWTJahiaCreateEngineInitBean result = new GWTJahiaCreateEngineInitBean();
            result.setLanguages(languages.getLanguages(getSite(), getRemoteJahiaUser(), getLocale()));
            String defaultLanguage = parent.getResolveSite().getDefaultLanguage();
            if (defaultLanguage == null) {
                defaultLanguage = sessionWrapper.getRootNode().getResolveSite().getDefaultLanguage();
            }

            result.setDefaultLanguageCode(defaultLanguage);
            result.setCurrentLocale(languages.getCurrentLang(getLocale()));
            final List<ExtendedNodeType> availableMixins = contentDefinition.getAvailableMixin(typename);

            List<GWTJahiaNodeType> gwtMixin = contentDefinition.getGWTNodeTypes(availableMixins, getUILocale());

            result.setMixin(gwtMixin);

            List<ExtendedNodeType> allTypes = new ArrayList<ExtendedNodeType>();
            ExtendedNodeType nodeType = NodeTypeRegistry.getInstance().getNodeType(typename);
            allTypes.add(nodeType);
            allTypes.addAll(availableMixins);

            result.setInitializersValues(contentDefinition.getAllInitializersValues(allTypes,
                    nodeType, null, parent, getUILocale()));

            result.setAcl(contentManager.getACL(parentpath, true, sessionWrapper, getUILocale()));

            String defaultName = JCRContentUtils.generateNodeName(nodeType.getLabel(LanguageCodeConverters.languageCodeToLocale(defaultLanguage)), 32);
            result.setDefaultName(contentManager.findAvailableName(parent, defaultName));
            return result;
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException("Cannot get node");
        }
    }

    public GWTJahiaCreatePortletInitBean initializeCreatePortletEngine(String typename, String parentpath)
            throws GWTJahiaServiceException {

        GWTJahiaCreateEngineInitBean result = initializeCreateEngine(typename, parentpath);
        GWTJahiaCreatePortletInitBean portletInitBean = new GWTJahiaCreatePortletInitBean();
        portletInitBean.setInitializersValues(result.getInitializersValues());
        portletInitBean.setLanguages(result.getLanguages());
        portletInitBean.setMixin(result.getMixin());
        portletInitBean.setNodeType(contentDefinition.getNodeType(typename, getUILocale()));

        return portletInitBean;
    }

    public GWTJahiaEditEngineInitBean initializeEditEngine(String nodepath, boolean tryToLockNode)
            throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
            JCRNodeWrapper nodeWrapper = sessionWrapper.getNode(nodepath);
            final GWTJahiaNode node = navigation.getGWTJahiaNode(nodeWrapper);
            if (tryToLockNode && !nodeWrapper.isLocked() && nodeWrapper.hasPermission(Privilege.JCR_LOCK_MANAGEMENT)) {
                nodeWrapper.checkout();
                nodeWrapper.lockAndStoreToken("engine");
            }

            dumpLocks(nodeWrapper);

            // get node type
            final List<GWTJahiaNodeType> nodeTypes =
                    contentDefinition.getNodeTypes(nodeWrapper.getNodeTypes(), getUILocale());

            // get properties
            final Map<String, GWTJahiaNodeProperty> props =
                    properties.getProperties(nodepath, retrieveCurrentSession());

            final GWTJahiaEditEngineInitBean result = new GWTJahiaEditEngineInitBean(nodeTypes, props);
            result.setNode(node);
            result.setAvailabledLanguages(languages.getLanguages(getSite(), getRemoteJahiaUser(), getLocale()));
            result.setCurrentLocale(languages.getCurrentLang(getLocale()));

            String defaultLanguage = nodeWrapper.getResolveSite().getDefaultLanguage();
            if (defaultLanguage == null) {
                defaultLanguage = sessionWrapper.getRootNode().getResolveSite().getDefaultLanguage();
            }

            result.setDefaultLanguageCode(defaultLanguage);


            final List<ExtendedNodeType> availableMixins =
                    contentDefinition.getAvailableMixin(nodeWrapper.getPrimaryNodeTypeName());

            List<GWTJahiaNodeType> gwtMixin = contentDefinition.getGWTNodeTypes(availableMixins, getUILocale());

            result.setMixin(gwtMixin);

            List<ExtendedNodeType> allTypes = new ArrayList<ExtendedNodeType>();
            allTypes.add(nodeWrapper.getPrimaryNodeType());
            allTypes.addAll(Arrays.asList(nodeWrapper.getMixinNodeTypes()));
            allTypes.addAll(availableMixins);

            JCRNodeWrapper parent = null;
            try {
                nodeWrapper.getParent();
            } catch (ItemNotFoundException ignored) {

            }
            result.setInitializersValues(
                    contentDefinition.getAllInitializersValues(allTypes, nodeWrapper.getPrimaryNodeType(), nodeWrapper,
                            parent, getUILocale()));
            final GWTJahiaNodeACL gwtJahiaNodeACL = contentManager.getACL(nodepath, false, sessionWrapper, getUILocale());
            result.setAcl(gwtJahiaNodeACL);
            Map<String,Set<String>> referencesWarnings = new HashMap<String, Set<String>>();
            for (GWTJahiaNodeProperty property : props.values()) {
                try {
                    if (property.getName().equals("*") || nodeWrapper.getProperty(property.getName()).getDefinition().isProtected()) {
                        continue;
                    }
                } catch (PathNotFoundException e) {
                    // ignore
                }
                List<GWTJahiaNode> refs = new ArrayList<GWTJahiaNode>();
                for (GWTJahiaNodePropertyValue value : (property.getValues())) {
                    if ((value.getType() == GWTJahiaNodePropertyType.REFERENCE ||
                            value.getType() == GWTJahiaNodePropertyType.WEAKREFERENCE) && value.getNode() != null) {
                        refs.add(value.getNode());
                    }
                }
                if (!refs.isEmpty()) {
                    final Set<String> allDeniedUsers = compareAcl(gwtJahiaNodeACL, refs);
                    if (!allDeniedUsers.isEmpty()) {
                        referencesWarnings.put(property.getName(), allDeniedUsers);
                    }
                }
            }
            result.setReferencesWarnings(referencesWarnings);
            return result;
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException("Cannot get node");
        }
    }

    public void closeEditEngine(String nodepath)
            throws GWTJahiaServiceException {
        final JCRSessionWrapper jcrSessionWrapper = retrieveCurrentSession();
        try {
            JCRNodeWrapper n = jcrSessionWrapper.getNode(nodepath);
            if (n.isLocked()) {
                n.unlock("engine");
            }

            dumpLocks(n);
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException("Cannot unlock node");
        }
    }

    public Set<String> compareAcl(GWTJahiaNodeACL nodeAcl, List<GWTJahiaNode> references)
            throws GWTJahiaServiceException {
        JCRSessionWrapper sessionWrapper = retrieveCurrentSession();

        final Set<String> result = new HashSet<String>();

        for (GWTJahiaNode reference : references) {
            GWTJahiaNodeACL referenceAcl =
                    contentManager.getACL(reference.getPath(), false, sessionWrapper, getUILocale());

            final Set<String> allReadUsers = new HashSet<String>();  // All users having read rights
            for (GWTJahiaNodeACE ace : nodeAcl.getAce()) {
                if ((ace.getPermissions().containsKey("jcr:read_live") &&
                        !Boolean.FALSE.equals(ace.getPermissions().get("jcr:read_live"))) ||
                        (ace.getInheritedPermissions().containsKey("jcr:read_live") &&
                                !Boolean.FALSE.equals(ace.getInheritedPermissions().get("jcr:read_live")))) {
                    allReadUsers.add(ace.getPrincipalType() + ":" + ace.getPrincipal());
                }
            }

            final Set<String> allDeniedUsers = new HashSet<String>();  // All users having read rights
            for (GWTJahiaNodeACE ace : referenceAcl.getAce()) {
                if ((ace.getPermissions().containsKey("jcr:read_live") &&
                        !Boolean.TRUE.equals(ace.getPermissions().get("jcr:read_live"))) ||
                        (ace.getInheritedPermissions().containsKey("jcr:read_live") &&
                                !Boolean.TRUE.equals(ace.getInheritedPermissions().get("jcr:read_live")))) {
                    allDeniedUsers.add(ace.getPrincipalType() + ":" + ace.getPrincipal());
                }
            }
            allDeniedUsers.retainAll(allReadUsers);
            result.addAll(allDeniedUsers);
        }
        return result;
    }

    public GWTJahiaEditEngineInitBean initializeEditEngine(List<String> paths, boolean tryToLockNode)
            throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper sessionWrapper = retrieveCurrentSession();

            List<GWTJahiaNodeType> nodeTypes = null;
            List<GWTJahiaNodeType> gwtMixin = null;
            List<ExtendedNodeType> allTypes = new ArrayList<ExtendedNodeType>();

            JCRNodeWrapper nodeWrapper = null;
            for (String path : paths) {
                nodeWrapper = sessionWrapper.getNode(path);
                if (tryToLockNode) {
                    nodeWrapper.lockAndStoreToken("engine");
                }

                dumpLocks(nodeWrapper);

                // get node type
                final List<GWTJahiaNodeType> theseTypes =
                        contentDefinition.getNodeTypes(nodeWrapper.getNodeTypes(), getUILocale());
                if (nodeTypes == null) {
                    nodeTypes = theseTypes;
                } else {
                    GWTJahiaNodeType p = nodeTypes.get(0);
                    nodeTypes.retainAll(theseTypes);

                    if (!nodeWrapper.isNodeType(p.getName())) {
                        nodeTypes.remove(0);
                        List<String> superTypes = p.getSuperTypes();
                        for (String s : superTypes) {
                            if (nodeWrapper.isNodeType(s)) {
                                nodeTypes.add(0, contentDefinition.getNodeType(s, getUILocale()));
                                break;
                            }
                        }
                    }
                }

                final List<ExtendedNodeType> availableMixins =
                        contentDefinition.getAvailableMixin(nodeWrapper.getPrimaryNodeTypeName());

                List<GWTJahiaNodeType> theseMixin = contentDefinition.getGWTNodeTypes(availableMixins, getUILocale());
                if (gwtMixin == null) {
                    gwtMixin = theseMixin;
                } else {
                    gwtMixin.retainAll(theseMixin);
                }

                allTypes.add(nodeWrapper.getPrimaryNodeType());
                allTypes.addAll(Arrays.asList(nodeWrapper.getMixinNodeTypes()));
                allTypes.addAll(availableMixins);
            }

            final GWTJahiaEditEngineInitBean result =
                    new GWTJahiaEditEngineInitBean(nodeTypes, new HashMap<String, GWTJahiaNodeProperty>());
            result.setAvailabledLanguages(languages.getLanguages(getSite(), getRemoteJahiaUser(), getLocale()));
            result.setCurrentLocale(languages.getCurrentLang(getLocale()));
            result.setMixin(gwtMixin);
            result.setInitializersValues(contentDefinition.getAllInitializersValues(allTypes,
                    NodeTypeRegistry.getInstance().getNodeType("nt:base"), nodeWrapper, nodeWrapper.getParent(),
                    getUILocale()));
            return result;
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException("Cannot get node");
        }

    }

    private void dumpLocks(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        if (logger.isDebugEnabled()) {
            logger.debug("Locks for " + nodeWrapper.getName() + " - " +nodeWrapper.isLocked());
            if (nodeWrapper.hasProperty("j:lockTypes")) {
                for (Value value : nodeWrapper.getProperty("j:lockTypes").getValues()) {
                    logger.debug(value.getString());
                }
            }
            NodeIterator ni = nodeWrapper.getNodes("j:translation*");
            while (ni.hasNext()) {
                JCRNodeWrapper jcrNodeWrapper = (JCRNodeWrapper) ni.next();
                logger.debug("Locks for " + jcrNodeWrapper.getName() + " - " +jcrNodeWrapper.isLocked());
                if (jcrNodeWrapper.hasProperty("j:lockTypes")) {
                    for (Value value : jcrNodeWrapper.getProperty("j:lockTypes").getValues()) {
                        logger.debug(value.getString());
                    }
                }

            }
        }
    }

    public Map<GWTJahiaWorkflowType,List<GWTJahiaWorkflowDefinition>> getWorkflowRules(String path)
            throws GWTJahiaServiceException {
        JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
        return workflow.getWorkflowRules(path, sessionWrapper, sessionWrapper.getLocale());
    }

    public void setGoogleDocsExportFormats(GoogleDocsExportFormats googleDocsExportFormats) {
        this.googleDocsExportFormats = googleDocsExportFormats;
    }

    public List<String> getGoogleDocsExportFormats(String nodeIdentifier) throws GWTJahiaServiceException {
        try {
            JCRNodeWrapper node = retrieveCurrentSession().getNodeByIdentifier(nodeIdentifier);
            return googleDocsExportFormats.getExportFormats(node.getFileContent().getContentType());
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void synchronizeWithGoogleDocs(String nodeIdentifier) throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper session = retrieveCurrentSession();
            JCRNodeWrapper node = session.getNodeByIdentifier(nodeIdentifier);
            String uri = GoogleDocsEditor.getDocumentUriBeingEdited(getSession());
            if (uri == null) {
                throw new GWTJahiaServiceException(
                        JahiaResourceBundle.getJahiaInternalResource("message.googleDocs.synchronize.documentNotFound",
                                getLocale(), "No document currently being edited in Google Docs can be found."));
            }
            try {
                GoogleDocsService googleDocsService =
                        googleDocsServiceFactory.getDocsService(getRequest(), getResponse());
                InputStream content = googleDocsService.downloadFile(uri);
                try {
                    JCRNodeWrapper parent = node.getParent();
                    session.checkout(parent);
                    parent.uploadFile(node.getName(), content, node.getFileContent().getContentType());
                    session.save();
                } finally {
                    IOUtils.closeQuietly(content);
                    String docId = null;
                    if (uri.contains("docId=")) {
                        docId = StringUtils.substringBetween(uri, "docId=", "&");
                    } else if (uri.contains("key=")) {
                        docId = StringUtils.substringBetween(uri, "key=", "&");
                    } else if (uri.contains("id=")) {
                        docId = StringUtils.substringBetween(uri, "id=", "&");
                    }
                    if (docId != null) {
                        try {
                            googleDocsService.delete(docId);
                        } catch (Exception e) {
                            logger.warn("Unable to delete document '" + docId + "' from Google Docs", e);
                        }
                    } else {
                        logger.warn("Unable to retrieve document ID from the URI: " + uri);
                    }
                }
            } catch (Exception e) {
                throw new GWTJahiaServiceException(e.getMessage());
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    /**
     * Returns the source node that is used in the specified reference node.
     *
     * @param referenceIdentifier the current reference node UUID
     * @return the source node that is used in the specified reference node
     * @throws GWTJahiaServiceException in case of an error
     */
    public GWTJahiaNode getSource(String referenceIdentifier) throws GWTJahiaServiceException {
        GWTJahiaNode gwtSourceNode = null;

        try {
            JCRSessionWrapper session = retrieveCurrentSession();
            JCRNodeWrapper node = session.getNodeByIdentifier(referenceIdentifier);
            if (node.hasProperty("j:node")) {
                JCRNodeWrapper source = (JCRNodeWrapper) node.getProperty("j:node").getNode();
                if (source != null) {
                    gwtSourceNode = navigation.getGWTJahiaNode(source);
                }
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }

        return gwtSourceNode;
    }

    public void flush(String path) {
        cacheHelper.flush(path, true);
    }

    public void flushAll() {
        cacheHelper.flushAll();
    }

    public List<GWTJahiaJobDetail> getActiveJobs() throws GWTJahiaServiceException {
        return schedulerHelper.getActiveJobs(getLocale());
    }

    public BasePagingLoadResult<GWTJahiaJobDetail> getJobs(int offset, int limit, String sortField, String sortDir,
                                                           List<String> groupNames) throws GWTJahiaServiceException {
        // todo Proper pagination support would imply that we only load the job details that were requested. Also sorting is not at all supported for the moment. 
        JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
        List<GWTJahiaJobDetail> jobList =
                schedulerHelper.getAllJobs(getLocale(), sessionWrapper.getUser(), new HashSet<String>(groupNames));
        int size = jobList.size();
        jobList = new ArrayList<GWTJahiaJobDetail>(jobList.subList(offset, Math.min(size, offset + limit)));
        BasePagingLoadResult<GWTJahiaJobDetail> pagingLoadResult = new BasePagingLoadResult<GWTJahiaJobDetail>(jobList, offset, size);
        return pagingLoadResult;
    }

    public Boolean deleteJob(String jobName, String groupName) throws GWTJahiaServiceException {
        return schedulerHelper.deleteJob(jobName, groupName);
    }

    public List<String> getAllJobGroupNames() throws GWTJahiaServiceException {
        return schedulerHelper.getAllJobGroupNames();
    }

    public BasePagingLoadResult<GWTJahiaContentHistoryEntry> getContentHistory(String nodeIdentifier, int offset, int limit) throws GWTJahiaServiceException {
        JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
        List<GWTJahiaContentHistoryEntry> historyListJahia = null;
        try {
            historyListJahia = contentManager.getContentHistory(sessionWrapper, nodeIdentifier, offset, limit);
            int size = historyListJahia.size();
            historyListJahia = new ArrayList<GWTJahiaContentHistoryEntry>(historyListJahia.subList(offset, Math.min(size, offset + limit)));
            BasePagingLoadResult<GWTJahiaContentHistoryEntry> pagingLoadResult = new BasePagingLoadResult<GWTJahiaContentHistoryEntry>(historyListJahia, offset, size);
            return pagingLoadResult;
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    public void cleanReferences(String path) throws GWTJahiaServiceException {
        contentManager.deleteReferences(path, getUser(), retrieveCurrentSession());
    }

    public GWTJahiaFieldInitializer getFieldInitializerValues(String typeName, String propertyName, String parentPath, Map<String, List<GWTJahiaNodePropertyValue>> dependentValues)
            throws GWTJahiaServiceException {
        try {
            JCRSessionWrapper sessionWrapper = retrieveCurrentSession();
            JCRNodeWrapper parent = sessionWrapper.getNode(parentPath);

            ExtendedNodeType nodeType = NodeTypeRegistry.getInstance().getNodeType(typeName);

            GWTJahiaFieldInitializer initializer = contentDefinition.getInitializerValues(nodeType.getPropertyDefinition(propertyName),
                    nodeType, null, parent, dependentValues, getUILocale());

            return initializer;
        } catch (RepositoryException e) {
            logger.error("Cannot get node", e);
            throw new GWTJahiaServiceException("Cannot get node");
        }
    }

    public List<GWTJahiaNode> getSitePagesWithTargetAreaName(String targetAreaName) {
        List<GWTJahiaNode> nodes = new ArrayList<GWTJahiaNode>();
        try {
            Query q = retrieveCurrentSession().getWorkspace().getQueryManager().createQuery("select * from [jnt:contentFolder] as l where localname()='"+targetAreaName+"' and isdescendantnode(l,['"+getSite().getPath()+"'])",Query.JCR_SQL2);
            QueryResult queryResult = q.execute();
            NodeIterator nodeIterator = queryResult.getNodes();
            while (nodeIterator.hasNext()) {
                JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) nodeIterator.nextNode();
                nodes.add(navigation.getGWTJahiaNode(nodeWrapper.getParent()));
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        } catch (GWTJahiaServiceException e) {
            logger.error(e.getMessage(), e);
        }
        return nodes;
    }

    public Map<String, WCAGValidationResult> validateWCAG(Map<String, String> richTexts) {
        Map<String, WCAGValidationResult> result = new HashMap<String, WCAGValidationResult>(richTexts.size());
        Locale locale;
        try {
            locale = getUILocale();
        } catch (GWTJahiaServiceException e) {
            logger.warn("Unable to get UI locale", e);
            locale = getRequest().getLocale();
        }

        for (Map.Entry<String, String> richText : richTexts.entrySet()) {
            ValidatorResults validatorResults = new WAIValidator(locale).validate(richText.getValue());
            result.put(richText.getKey(), toWCAGResult(validatorResults));
        }

        return result;
    }

    public int getNumberOfTasksForUser() throws GWTJahiaServiceException {
        return workflow.getNumberOfTasksForUser(getUser(), getLocale());
    }

    private WCAGValidationResult toWCAGResult(ValidatorResults validatorResults) {
        if (validatorResults.isEmpty()) {
            return WCAGValidationResult.OK;
        }

        WCAGValidationResult wcagResult = new WCAGValidationResult();
        for (Result result : validatorResults.getErrors()) {
            wcagResult.getErrors().add(new WCAGViolation(result.getType().toString(), result.getMessage(), result.getContext(), StringEscapeUtils.escapeXml(result.getCode()), result.getExample(), Integer.valueOf(result.getLine()), Integer.valueOf(result.getColumn())));
        }

        for (Result result : validatorResults.getWarnings()) {
            wcagResult.getWarnings().add(new WCAGViolation(result.getType().toString(), result.getMessage(), result.getContext(), StringEscapeUtils.escapeXml(result.getCode()), result.getExample(), Integer.valueOf(result.getLine()), Integer.valueOf(result.getColumn())));
        }

        for (Result result : validatorResults.getInfos()) {
            wcagResult.getInfos().add(new WCAGViolation(result.getType().toString(), result.getMessage(), result.getContext(), StringEscapeUtils.escapeXml(result.getCode()), result.getExample(), Integer.valueOf(result.getLine()), Integer.valueOf(result.getColumn())));
        }

        return wcagResult;
    }

    public GWTJahiaToolbar getGWTToolbars(String toolbarGroup) throws GWTJahiaServiceException {
        return uiConfigHelper.getGWTToolbarSet(getSite(), getSite(), getRemoteJahiaUser(), getLocale(),getUILocale(), getRequest(), toolbarGroup);
    }

    public GWTJahiaPortletOutputBean drawPortletInstanceOutput(String windowID, String entryPointIDStr, String pathInfo, String queryString) {
        GWTJahiaPortletOutputBean result = new GWTJahiaPortletOutputBean();
        try {
            int fieldId = Integer.parseInt(windowID);
            String portletOutput = ServicesRegistry.getInstance().getApplicationsDispatchService().getAppOutput(fieldId, entryPointIDStr, getRemoteJahiaUser(), getRequest(), getResponse(), getServletContext());
            try {
                JCRNodeWrapper node = JCRSessionFactory.getInstance().getCurrentUserSession().getNodeByUUID(entryPointIDStr);
                String nodeTypeName = node.getPrimaryNodeTypeName();
                /** todo cleanup the hardcoded value here */
                if ("jnt:htmlPortlet".equals(nodeTypeName)) {
                    result.setInIFrame(false);
                }
                if ("jnt:contentPortlet".equals(nodeTypeName) || "jnt:rssPortlet".equals(nodeTypeName)) {
                    result.setInContentPortlet(true);
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            result.setHtmlOutput(portletOutput);

            // what we need to do now is to do special processing for <script> tags, and on the client side we will
            // create them dynamically.
            Source source = new Source(portletOutput);
            source = new Source((new SourceFormatter(source)).toString());
            List<StartTag> scriptTags = source.getAllStartTags(HTMLElementName.SCRIPT);
            for (StartTag curScriptTag : scriptTags) {
                if ((curScriptTag.getAttributeValue("src") != null) &&
                        (!curScriptTag.getAttributeValue("src").equals(""))) {
                    result.getScriptsWithSrc().add(curScriptTag.getAttributeValue("src"));
                } else {
                    result.getScriptsWithCode().add(curScriptTag.getElement().getContent().toString());
                }
            }

        } catch (JahiaException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }


    public List<GWTJahiaSite> getAvailableSites() {
        final Iterator<JahiaSite> sites;
        final List<GWTJahiaSite> returnedSites = new ArrayList<GWTJahiaSite>();
        try {
            sites = ServicesRegistry.getInstance().getJahiaSitesService().getSites();
            while (sites.hasNext()) {
                JahiaSite jahiaSite = sites.next();
                GWTJahiaSite gwtJahiaSite = new GWTJahiaSite();
                gwtJahiaSite.setSiteId(jahiaSite.getID());
                gwtJahiaSite.setSiteName(jahiaSite.getTitle());
                gwtJahiaSite.setSiteKey(jahiaSite.getSiteKey());
                gwtJahiaSite.setTemplateFolder(jahiaSite.getTemplateFolder());
                gwtJahiaSite.setTemplatePackageName(jahiaSite.getTemplatePackageName());
                gwtJahiaSite.setInstalledModules(jahiaSite.getInstalledModules());
                returnedSites.add(gwtJahiaSite);
            }
        } catch (JahiaException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return returnedSites;
    }

	public boolean createRemotePublication(String nodeName, Map<String, String> props, boolean validateConnectionSettings)
	        throws GWTJahiaServiceException {
		
    	String theUrl = props.get("remoteUrl");
    	while (theUrl.endsWith("/")) {
    		theUrl = theUrl.substring(0, theUrl.length() - 1);
    	}
    	if (!theUrl.startsWith("http://") && !theUrl.startsWith("https://")) {
    		theUrl = "http://" + theUrl;
    	}
    	props.put("remoteUrl", theUrl);
    	
    	String thePath = props.get("remotePath");
    	while (thePath.endsWith("/")) {
    		thePath = thePath.substring(0, thePath.length() - 1);
    	}
    	props.put("remotePath", thePath);
		
		if (validateConnectionSettings) {
			publication.validateConnection(props, retrieveCurrentSession());
		}

		List<GWTJahiaNodeProperty> gwtProps = new ArrayList<GWTJahiaNodeProperty>();
		gwtProps.add(new GWTJahiaNodeProperty("remoteUrl", props.get("remoteUrl")));
		gwtProps.add(new GWTJahiaNodeProperty("remotePath", props.get("remotePath")));
		gwtProps.add(new GWTJahiaNodeProperty("remoteUser", props.get("remoteUser")));
		gwtProps.add(new GWTJahiaNodeProperty("remotePassword", encryptPassword(props
		        .get("remotePassword"))));
		gwtProps.add(new GWTJahiaNodeProperty("node", props.get("node")));
		gwtProps.add(new GWTJahiaNodeProperty("schedule", props.get("schedule")));

		createNode("/remotePublications", JCRContentUtils.generateNodeName(nodeName, 255),
		        "jnt:remotePublication", null, null, gwtProps,
		        new HashMap<String, List<GWTJahiaNodeProperty>>());

		return true;
	}

	private String encryptPassword(String pwd) {
		return StringUtils.isNotEmpty(pwd) ? EncryptionUtils.passwordBaseEncrypt(pwd) : StringUtils.EMPTY;
	}

	public Integer deleteAllCompletedJobs() throws GWTJahiaServiceException {
	    return schedulerHelper.deleteAllCompletedJobs();
    }

    public String getNodeURLByIdentifier(String servlet, String identifier, Date versionDate, String versionLabel, String workspace,
                             String locale) throws GWTJahiaServiceException {
        final JCRSessionWrapper session = retrieveCurrentSession(workspace != null ? workspace : getWorkspace(),
                locale != null ? LanguageCodeConverters.languageCodeToLocale(locale) : getLocale(), false);
        try {
            JCRNodeWrapper nodeByIdentifier = session.getNodeByIdentifier(identifier);
            if(nodeByIdentifier.isFile()) {
                String url = nodeByIdentifier.getUrl();
                if (versionDate != null) {
                    url += "?v=" + (versionDate.getTime());
                    if (versionLabel != null) {
                        url += "&l=" + versionLabel;
                    }
                }

                return url;
            } else {
                return getResponse().encodeURL(this.navigation.getNodeURL(servlet, nodeByIdentifier, versionDate, versionLabel, session.getWorkspace().getName(),
                        session.getLocale()));
            }
        } catch (RepositoryException e) {
            throw new GWTJahiaServiceException(e);
        }
    }

}