/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.test.framework;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.content.rules.RulesListener;
import org.jahia.services.importexport.DocumentViewImportHandler;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class DeployResourcesTestExecutionListener extends
        AbstractTestExecutionListener {
    private static Logger LOGGER = LoggerFactory
            .getLogger(DeployResourcesTestExecutionListener.class);

    private static final String TEST_SYSTEM_ID = "currentmodule";
    private static final String MAIN_META_INF = "./src/main/resources/META-INF";
    private static final String TEST_META_INF = "./src/test/resources/META-INF";

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        addCndFiles(MAIN_META_INF, TEST_SYSTEM_ID);
        addCndFiles(TEST_META_INF, TEST_SYSTEM_ID);
        if (NodeTypeRegistry.getInstance().getFiles(TEST_SYSTEM_ID) != null) {
            JCRStoreService.getInstance().deployDefinitions(TEST_SYSTEM_ID);
        }

        addRuleFiles(MAIN_META_INF);
        addRuleFiles(TEST_META_INF);

        importXmlFiles(MAIN_META_INF);
        importXmlFiles(TEST_META_INF);
        
        startSchedulers();
    }
    
    private void startSchedulers() {
        try {
            // start schedulers
            ServicesRegistry.getInstance().getSchedulerService().startSchedulers();
        } catch (JahiaInitializationException e) {
            LOGGER.error(e.getMessage(), e);
            throw new JahiaRuntimeException(e);
        }
    }
    
    private void addCndFiles(String resourcePath, String systemId)
            throws Exception {
        for (File cndFile : listFiles(resourcePath, "*.cnd")) {
            NodeTypeRegistry.getInstance().addDefinitionsFile(cndFile,
                    systemId, null);
        }
    }

    private void addRuleFiles(String resourcePath) throws Exception {
        for (File dslFile : listFiles(resourcePath, "*.dsl")) {
            for (RulesListener listener : RulesListener.getInstances()) {
                listener.addRulesDescriptor(dslFile);
            }
        }

        for (File drlFile : listFiles(resourcePath, "*.drl")) {
            for (RulesListener listener : RulesListener.getInstances()) {
                List<String> filesAccepted = listener.getFilesAccepted();
                if (filesAccepted.contains(StringUtils.substringAfterLast(
                        drlFile.getPath(), "/"))) {
                    listener.addRules(drlFile);
                }
            }
        }
    }

    private void importXmlFiles(String resourcePath) throws Exception {
        for (final File importFile : listFiles(resourcePath, "*import*.xml")) {
            final String targetPath = "/"
                    + StringUtils.substringAfter(
                            StringUtils.substringBeforeLast(
                                    importFile.getPath(), "."), "import-")
                            .replace('-', '/');

            JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser() != null ? JCRSessionFactory
                    .getInstance().getCurrentUser() : JahiaUserManagerService
                    .getInstance().lookupRootUser().getJahiaUser();
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(user,
                    null, null, new JCRCallback<Boolean>() {
                        public Boolean doInJCR(JCRSessionWrapper session)
                                throws RepositoryException {
                            InputStream is = null;
                            try {
                                is = new FileInputStream(importFile);
                                session.importXML(
                                        targetPath,
                                        is,
                                        ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                        DocumentViewImportHandler.ROOT_BEHAVIOUR_IGNORE);
                            } catch (IOException e) {
                                LOGGER.error("Cannot create file input stream",
                                        e);
                            } finally {
                                IOUtils.closeQuietly(is);
                            }

                            return null;
                        }
                    });
        }
    }

    private Collection<File> listFiles(String resourcePath, String extension) {
        File dir = new File(resourcePath);
        FileFilter fileFilter = new WildcardFileFilter(extension);
        File[] files = dir.listFiles(fileFilter);
        return files == null ? Collections.<File>emptyList() : Arrays.<File>asList(files); 
    }
}