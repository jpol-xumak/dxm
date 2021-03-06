/*
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
package org.jahia.services.modulemanager.impl;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.util.StringUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.modulemanager.BundleInfo;
import org.jahia.services.modulemanager.InvalidModuleException;
import org.jahia.services.modulemanager.ModuleManagementException;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.services.modulemanager.ModuleNotFoundException;
import org.jahia.services.modulemanager.OperationResult;
import org.jahia.services.modulemanager.persistence.BundlePersister;
import org.jahia.services.modulemanager.persistence.PersistentBundle;
import org.jahia.services.modulemanager.persistence.PersistentBundleInfoBuilder;
import org.jahia.services.modulemanager.spi.BundleService;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * The main entry point service for the module management service, providing functionality for module deployment, undeployment, start and
 * stop operations, which are performed in a seamless way on a standalone installation as well as across the platform cluster.
 *
 * @author Sergiy Shyrkov
 */
public class ModuleManagerImpl implements ModuleManager {
    private static final Logger logger = LoggerFactory.getLogger(ModuleManagerImpl.class);

    private BundleService bundleService;
    private BundlePersister persister;
    
    /**
     * Operation callback which implementation performs the actual operation.
     *
     * @author Sergiy Shyrkov
     */
    private interface BundleOperation {

        String getName();

        void perform(BundleInfo info, String target) throws ModuleManagementException;
    }

    /**
     * @return basic information about this bundle as a new {@link BundleInfo} object
     */
    private static BundleInfo toBundleInfo(PersistentBundle persistentBundle) {
        return new BundleInfo(persistentBundle.getGroupId(), persistentBundle.getSymbolicName(), persistentBundle.getVersion());
    }

    private BundleInfo findTargetBundle(String bundleKey, String symbolicName, String version) {
        BundleInfo targetInfo = null;
        List<Bundle> matches = new ArrayList<>();
        Bundle[] allBundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle b : allBundles) {
            if (symbolicName.equals(b.getSymbolicName())
                    && (version == null || version.equals(b.getVersion().toString()))
                    && b.getState() != Bundle.UNINSTALLED) {
                matches.add(b);
            }
        }

        if (matches.size() > 1) {
            logger.warn("Found multiple bundles matching the key {}. Unable to uniquely identify target bundle",
                    bundleKey);
        } else if (!matches.isEmpty()) {
            Bundle target = matches.get(0);
            targetInfo = new BundleInfo(BundleUtils.getModuleGroupId(target), target.getSymbolicName(),
                    target.getVersion().toString());
        }

        return targetInfo;
    }

    private BundleInfo getBundleInfo(String bundleKey) {

        BundleInfo info = null;

        // first check if we have the full key and all the info needed already
        if (bundleKey.indexOf('/') != -1) {
            info = BundleInfo.fromKey(bundleKey);
            if (info.getGroupId() != null && info.getSymbolicName() != null && info.getVersion() != null) {
                // we have everything
                return info;
            }
        }

        return findTargetBundle(bundleKey, info != null ? info.getSymbolicName() : bundleKey,
                info != null ? info.getVersion() : null);
    }

    /**
     * Performs an installation of the bundle calling the bundle services.
     *
     * @param info the bundle info
     * @param target group of target cluster nodes for bundle installation
     * @param start <code>true</code>, if the bundle should be started right after installation
     * @return the result of the operation
     * @throws ModuleManagementException in case of bundle service errors
     */
    private OperationResult install(PersistentBundle info, final String target, boolean start)
            throws ModuleManagementException {
        bundleService.install(info.getLocation(), target, start);

        return OperationResult.success(toBundleInfo(info));
    }

    @Override
    public OperationResult install(Resource bundleResource, String target) throws ModuleManagementException {
        return install(bundleResource, target, false);
    }

    @Override
    public OperationResult install(Resource bundleResource, String target, boolean start) throws ModuleManagementException {

        long startTime = System.currentTimeMillis();
        logger.info("Performing installation for bundle {} on target {}", new Object[] {bundleResource, target});

        OperationResult result = null;
        PersistentBundle bundleInfo = null;
        Exception error = null;
        try {
            bundleInfo = PersistentBundleInfoBuilder.build(bundleResource);
            if (bundleInfo == null) {
                throw new InvalidModuleException();
            }
            persister.store(bundleInfo);
            result = install(bundleInfo, target, start);
        } catch (ModuleManagementException e) {
            error = e;
            throw e;
        } catch (Exception e) {
            error = e;
            throw new ModuleManagementException(e);
        } finally {
            Object info = bundleInfo != null ? toBundleInfo(bundleInfo) : bundleResource;
            long timeTaken = System.currentTimeMillis() - startTime;
            if (error == null) {
                logger.info("Installation completed for bundle {} on target {} in {} ms. Operation result: {}",
                        new Object[] { info, target, timeTaken, result });
            } else {
                logger.info("Installation failed for bundle {} on target {} (took {} ms). Operation error: {}",
                        new Object[] { info, target, timeTaken, error });
            }
        }

        return result;
    }

    /**
     * Performs the specified operation of the bundle.
     *
     * @param bundleKey the key of the bundle to perform operation on
     * @param target the target cluster group
     * @param operation the operation callback to be used to effectively execute the operation
     * @return the result of the operation
     */
    private OperationResult performOperation(String bundleKey, String target, BundleOperation operation) {

        if (StringUtils.isEmpty(bundleKey)) {
            throw new IllegalArgumentException("Bundle '" + bundleKey + "' key in invalid");
        }

        long startTime = System.currentTimeMillis();
        logger.info("Performing {} operation for bundle {} on target {}",
                new Object[] { operation.getName(), bundleKey, target });

        OperationResult result = null;
        BundleInfo info = null;
        Exception error = null;
        try {
            info = getBundleInfo(bundleKey);
            if (info == null) {
                throw new ModuleNotFoundException(bundleKey);
            }
            operation.perform(info, target);
            result = OperationResult.success(info);
        } catch (ModuleManagementException e) {
            error = e;
            throw e;
        } catch (Exception e) {
            error = e;
            throw new ModuleManagementException(e);
        } finally {
            if (error == null) {
                logger.info("{} operation completed for bundle {} on target {} in {} ms. Opearation result: {}",
                        new Object[] { operation.getName(), bundleKey, target, System.currentTimeMillis() - startTime,
                                result });
            } else {
                logger.info("{} operation failed for bundle {} on target {} (took {} ms). Opearation error: {}",
                        new Object[] { operation.getName(), bundleKey, target, System.currentTimeMillis() - startTime,
                                error });
            }
        }

        return result;
    }

    /**
     * Injects an instance of the bundle service.
     *
     * @param bundleService an instance of the bundle service
     */
    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    /**
     * Injects an instance of the bundle persister.
     *
     * @param persister an instance of the bundle persister
     */
    public void setPersister(BundlePersister persister) {
        this.persister = persister;
    }

    @Override
    public OperationResult start(String bundleKey, String target) {

        return performOperation(bundleKey, target, new BundleOperation() {

            @Override
            public String getName() {
                return "Start";
            }

            @Override
            public void perform(BundleInfo info, String target) throws ModuleManagementException {
                bundleService.start(info, target);
            }
        });
    }

    @Override
    public OperationResult stop(String bundleKey, String target) {

        return performOperation(bundleKey, target, new BundleOperation() {

            @Override
            public String getName() {
                return "Stop";
            }

            @Override
            public void perform(BundleInfo info, String target) throws ModuleManagementException {
                bundleService.stop(info, target);
            }
        });
    }

    @Override
    public OperationResult uninstall(String bundleKey, String target) {

        return performOperation(bundleKey, target, new BundleOperation() {

            @Override
            public String getName() {
                return "Uninstall";
            }

            @Override
            public void perform(BundleInfo info, String target) throws ModuleManagementException {
                bundleService.uninstall(info, target);
            }
        });
    }
}
