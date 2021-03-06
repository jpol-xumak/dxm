/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 * <p>
 * http://www.jahia.com
 * <p>
 * Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 * <p>
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 * <p>
 * 1/ GPL
 * ==================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ===================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 * <p>
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.render.scripting.bundle;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A replacement for the JDK's {@link ScriptEngineManager} that properly deals with {@link ScriptEngineFactory}
 * implementations provided in OSGi modules. In particular, bundles that provide a {@code
 * javax.script.ScriptEngineFactory} file in their {@code META-INF/services} directory (Java Service Provider
 * infrastructure) will be handled by this ScriptEngineManager, wrapping the provided ScriptEngineFactory implementation
 * in a {@link BundleScriptEngineFactory} so that the appropriate class loader will be used to resolve views and
 * associated resources. Additionally, provided script extensions will now be listened to and installed bundles will be
 * scanned so that any installed module that provides views that can be handled by the newly installed
 * ScriptEngineFactory will be now able to answer to view requests.
 * <p>
 * Additionally, for each ScriptEngineFactory, this BundleScriptEngineManager will attempt to load an associated {@link
 * BundleScriptEngineFactoryConfigurator} which should be named {@code <fully qualified ScriptEngineFactory
 * name>Configurator} so that additional set up / clean up can be performed when the bundle is started or stopped.
 */
public class BundleScriptEngineManager extends ScriptEngineManager {

    private static final String SCRIPT_ENGINE_FACTORY_CLASS_NAME = ScriptEngineFactory.class.getName();
    private static final String META_INF_SERVICES = "META-INF/services";
    private static Logger logger = LoggerFactory.getLogger(BundleScriptEngineManager.class);

    private Bindings globalScopeBindings;

    private final Map<String, ScriptEngineFactory> extensionsToScriptFactories = new ConcurrentHashMap<>(17);
    private final Map<String, ScriptEngineFactory> namesToScriptFactories = new ConcurrentHashMap<>(17);
    private final Map<String, ScriptEngineFactory> mimeTypesToScriptFactories = new ConcurrentHashMap<>(17);
    private final Map<Long, List<BundleScriptEngineFactory>> bundleIdsToScriptFactories = new ConcurrentHashMap<>(17);
    private final Map<String, BundleScriptEngineFactoryConfigurator> configurators = new ConcurrentHashMap<>(17);
    private final Map<ClassLoader, Map<String, ScriptEngine>> engineCache = new ConcurrentHashMap<>(17);

    private enum KeyType {EXTENSION, MIME_TYPE, NAME}

    // Initialization on demand holder idiom: thread-safe singleton initialization
    private static class Holder {
        static final BundleScriptEngineManager INSTANCE = new BundleScriptEngineManager();

        private Holder() {
        }
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return a singleton instance of this class
     */
    public static BundleScriptEngineManager getInstance() {
        return Holder.INSTANCE;
    }

    private BundleScriptEngineManager() {
        this.globalScopeBindings = new SimpleBindings();
    }

    /**
     * {@inheritDoc}
     */
    public Object get(String key) {
        return globalScopeBindings.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Bindings getBindings() {
        return globalScopeBindings;
    }

    /**
     * {@inheritDoc}
     */
    public void put(String key, Object value) {
        globalScopeBindings.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setBindings(Bindings bindings) {
        this.globalScopeBindings = bindings;
    }

    private ScriptEngine getEngine(String key, Map<String, ScriptEngineFactory> factoriesForKeyType, KeyType keyType) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        Map<String, ScriptEngine> stringScriptEngineMap = engineCache.get(contextClassLoader);

        if (stringScriptEngineMap == null) {
            stringScriptEngineMap = new ConcurrentHashMap<>();
            engineCache.put(contextClassLoader, stringScriptEngineMap);
        }

        ScriptEngine scriptEngine = stringScriptEngineMap.get(key);
        if (scriptEngine == null) {

            final ScriptEngineFactory scriptEngineFactory = factoriesForKeyType.get(key);
            if (scriptEngineFactory != null) {
                // perform configuration of the factory if needed
                if (scriptEngineFactory instanceof BundleScriptEngineFactory) {
                    BundleScriptEngineFactory factory = (BundleScriptEngineFactory) scriptEngineFactory;
                    final BundleScriptEngineFactoryConfigurator configurator = getConfigurator(factory);
                    if (configurator != null) {
                        configurator.configurePreScriptEngineCreation(factory.getWrappedFactory());
                    }
                }

                scriptEngine = scriptEngineFactory.getScriptEngine();
                scriptEngine.setBindings(getBindings(), ScriptContext.GLOBAL_SCOPE);
            } else {
                switch (keyType) {
                    case EXTENSION:
                        scriptEngine = super.getEngineByExtension(key);
                        break;
                    case MIME_TYPE:
                        scriptEngine = super.getEngineByMimeType(key);
                        break;
                    case NAME:
                        scriptEngine = super.getEngineByName(key);
                        break;
                    default:
                        scriptEngine = null;
                }
            }

            if (scriptEngine != null) {
                stringScriptEngineMap.put(key, scriptEngine);
            }
        }
        return scriptEngine;
    }

    /**
     * {@inheritDoc}
     */
    public ScriptEngine getEngineByExtension(String extension) {
        return getEngine(extension, extensionsToScriptFactories, KeyType.EXTENSION);
    }

    /**
     * {@inheritDoc}
     */
    public ScriptEngine getEngineByMimeType(String mimeType) {
        return getEngine(mimeType, mimeTypesToScriptFactories, KeyType.MIME_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    public ScriptEngine getEngineByName(String shortName) {
        return getEngine(shortName, namesToScriptFactories, KeyType.NAME);
    }

    /**
     * {@inheritDoc}
     */
    public List<ScriptEngineFactory> getEngineFactories() {
        List<ScriptEngineFactory> bundleScriptEngineFactories = new ArrayList<>();
        bundleScriptEngineFactories.addAll(extensionsToScriptFactories.values());
        bundleScriptEngineFactories.addAll(namesToScriptFactories.values());
        bundleScriptEngineFactories.addAll(mimeTypesToScriptFactories.values());
        return bundleScriptEngineFactories;
    }

    /**
     * {@inheritDoc}
     */
    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        extensionsToScriptFactories.put(extension, factory);
    }

    /**
     * {@inheritDoc}
     */
    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        mimeTypesToScriptFactories.put(type, factory);
    }

    /**
     * {@inheritDoc}
     */
    public void registerEngineName(String name, ScriptEngineFactory factory) {
        namesToScriptFactories.put(name, factory);
    }

    private BundleScriptingContext getScriptingContext(Bundle bundle) throws IOException {
        List<String> factoryCandidates = findFactoryCandidates(bundle);
        if (factoryCandidates.isEmpty()) {
            return null;
        }

        ClassLoader factoryLoader = null;
        final List<ScriptEngineFactory> factories = new ArrayList<>(factoryCandidates.size());
        for (String factoryCandidate : factoryCandidates) {
            final Class<? extends ScriptEngineFactory> factoryClass;
            try {
                factoryClass = bundle.loadClass(factoryCandidate).asSubclass(ScriptEngineFactory.class);
                factories.add(factoryClass.cast(factoryClass.newInstance()));
            } catch (ClassNotFoundException e) {
                logger.warn("ScriptEngineFactory {} was registered to be loaded but no associated class was found in bundle {}. Ignoring.", factoryCandidate, bundle);
                continue;
            } catch (InstantiationException | IllegalAccessException e) {
                logger.warn("Couldn't instantiate ScriptEngineFactory {}. Cause: {}. Ignoring.", factoryCandidate, e.getLocalizedMessage());
                continue;
            } catch (ClassCastException e) {
                logger.warn("Registered ScriptEngineFactory {} doesn't implement ScriptEngineFactory in bundle {}. Ignoring.", factoryCandidate, bundle);
                continue;
            }

            if (factoryLoader == null) {
                // retrieve the class loader associated with the bundle
                factoryLoader = factoryClass.getClassLoader();
            }

            // attempt to load a configurator for the current script engine factory
            final String configuratorName = factoryCandidate + "Configurator";
            try {
                final Class<?> configuratorClass = bundle.loadClass(configuratorName);
                final BundleScriptEngineFactoryConfigurator configurator = configuratorClass.asSubclass(BundleScriptEngineFactoryConfigurator.class)
                        .newInstance();
                configurators.put(factoryCandidate, configurator);
            } catch (ClassNotFoundException e) {
                // no configurator found for this script engine factory
            } catch (ClassCastException e) {
                logger.warn("Found class {} that doesn't implement BundleScriptEngineFactoryConfigurator in bundle {}. Ignoring.", configuratorName, bundle);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Couldn't instantiate configurator class {} in bundle {}. Cause: {}", new String[]{configuratorName, bundle.toString(), e
                        .getLocalizedMessage()});
            }
        }

        // check if the bundle defined any view extension priorities
        // todo: add more validation and better specify how this functionality should work
        final Dictionary<String, String> headers = bundle.getHeaders();
        final String extensionsPriorities = headers.get(BundleScriptingConfigurationConstants.JAHIA_SCRIPTING_EXTENSIONS_PRIORITIES);
        final Map<String, Integer> extensionsPrioritiesMap;
        if (extensionsPriorities != null) {
            final String[] extensionPriorityPairs = StringUtils.split(extensionsPriorities);
            extensionsPrioritiesMap = new HashMap<>(extensionPriorityPairs.length);
            for (String extensionPriorityPair : extensionPriorityPairs) {
                final String[] extensionPrioritySplit = StringUtils.split(extensionPriorityPair, '=');
                boolean valid = false;
                if (extensionPrioritySplit != null && extensionPrioritySplit.length == 2) {
                    try {
                        extensionsPrioritiesMap.put(extensionPrioritySplit[0], Integer.parseInt(extensionPrioritySplit[1]));
                        valid = true;
                    } catch (NumberFormatException e) {
                        valid = false;
                    }
                }

                if (!valid) {
                    logger.warn("Invalid extension - priority pair: {}. Format is extension=priority, priority should be an integer. Extension will be " +
                            "ignored.", extensionPriorityPair);
                }
            }
        } else {
            extensionsPrioritiesMap = null;
        }

        return new BundleScriptingContext(factories, factoryLoader, extensionsPrioritiesMap);
    }

    private BundleScriptEngineFactoryConfigurator getConfigurator(BundleScriptEngineFactory factory) {
        return configurators.get(factory.getWrappedFactoryClassName());
    }

    private List<String> findFactoryCandidates(Bundle bundle) throws IOException {
        if ("system.bundle".equals(bundle.getSymbolicName())) {
            return Collections.emptyList();
        }

        if (bundle.getState() == Bundle.ACTIVE) {
            Enumeration<URL> urls = bundle.findEntries(META_INF_SERVICES, SCRIPT_ENGINE_FACTORY_CLASS_NAME, false);
            if (urls == null) {
                return Collections.emptyList();
            }

            List<String> factoryCandidates = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL u = urls.nextElement();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        factoryCandidates.add(line.trim());
                    }
                }
            }
            return factoryCandidates;
        }

        return Collections.emptyList();
    }

    /**
     * Removes the {@link ScriptEngineFactory} instances associated with the specified {@link Bundle} when it is
     * stopped, making sure that the associated views are also made unavailable by calling {@link
     * BundleScriptResolver#remove(ScriptEngineFactory, Bundle)}.
     *
     * @param bundle the stopping bundle for which we want to deactivate ScriptEngineFactories if needed
     */
    public void removeScriptEngineFactoriesIfNeeded(Bundle bundle) {
        final List<BundleScriptEngineFactory> factories = bundleIdsToScriptFactories.remove(bundle.getBundleId());
        if (factories != null) {
            for (BundleScriptEngineFactory bundleScriptEngineFactory : factories) {
                // removing scripts associated with the ScriptEngineFactory needs extension information so it needs
                // to occur before we remove the extension to factory mapping
                BundleScriptResolver.getInstance().remove(bundleScriptEngineFactory, bundle);

                // check if we have a configurator to call
                final BundleScriptEngineFactoryConfigurator configurator = getConfigurator(bundleScriptEngineFactory);
                if (configurator != null) {
                    configurator.destroy(bundleScriptEngineFactory.getWrappedFactory());
                }
                configurators.remove(bundleScriptEngineFactory.getWrappedFactoryClassName());

                // clean-up our maps last since we might need that information to perform the rest of the clean-up
                final List<String> extensions = bundleScriptEngineFactory.getExtensions();
                for (String extension : extensions) {
                    extensionsToScriptFactories.remove(extension);
                }

                final List<String> mimeTypes = bundleScriptEngineFactory.getMimeTypes();
                for (String mimeType : mimeTypes) {
                    mimeTypesToScriptFactories.remove(mimeType);
                }

                final List<String> names = bundleScriptEngineFactory.getNames();
                for (String name : names) {
                    namesToScriptFactories.remove(name);
                }
            }
            //clean up engine cache
            engineCache.clear();
        }
    }

    /**
     * Register any {@link ScriptEngineFactory} instances defined in the context of the specified {@link Bundle}.
     * Any ScriptEngineFactory implementation that is declared in the
     * {@code META-INF/services/javax.script.ScriptEngineFactory} file of the Bundle is instantiated and configured.
     * Additionally, the OSGi headers are examined to look for any {@code Jahia-Scripting-Extensions-Priorities}
     * header value to configure ordering of scripting languages. If the Bundle contains any
     * {@link BundleScriptEngineFactoryConfigurator} implementations matching a declared ScriptEngineFactory
     * implementation, then such classes are instantiated and associated with the appropriate ScriptEngineFactory
     * instance to configure it when appropriate. Finally,
     * {@link BundleScriptResolver#register(ScriptEngineFactory, Bundle)} is called for each such configured
     * ScriptEngineFactory in order to be notify it that new scripting languages are available and activate
     * associated views if needed.
     *
     * @param bundle the starting bundle for which we want to activate ScriptEngineFactories if needed
     */
    public void addScriptEngineFactoriesIfNeeded(Bundle bundle) {
        try {
            final BundleScriptingContext scriptingContext = getScriptingContext(bundle);
            if (scriptingContext != null) {
                addFactories(bundle, scriptingContext);
            }
        } catch (IOException e) {
            logger.error("Error trying to get bundle " + bundle + " script engine factory information", e);
        }
    }

    private void addFactories(Bundle bundle, BundleScriptingContext scriptingContext) {
        List<ScriptEngineFactory> engineFactories = scriptingContext.getEngineFactories();
        final List<BundleScriptEngineFactory> factories = new ArrayList<>(engineFactories.size());

        for (ScriptEngineFactory factory : engineFactories) {
            final BundleScriptEngineFactory bundleScriptEngineFactory = new BundleScriptEngineFactory(factory, scriptingContext);

            final List<String> extensions = factory.getExtensions();
            for (String extension : extensions) {
                extensionsToScriptFactories.put(extension, bundleScriptEngineFactory);
            }

            final List<String> mimeTypes = factory.getMimeTypes();
            for (String mimeType : mimeTypes) {
                mimeTypesToScriptFactories.put(mimeType, bundleScriptEngineFactory);
            }

            final List<String> names = factory.getNames();
            for (String name : names) {
                namesToScriptFactories.put(name, bundleScriptEngineFactory);
            }

            factories.add(bundleScriptEngineFactory);

            // check if we need to further configure the factory
            final BundleScriptEngineFactoryConfigurator configurator = getConfigurator(bundleScriptEngineFactory);
            if (configurator != null) {
                configurator.configure(factory, bundle, scriptingContext.getClassLoader());
            }
            BundleScriptResolver.getInstance().register(bundleScriptEngineFactory, bundle);
        }

        // clean engine cache after installing a new script engine
        engineCache.clear();

        bundleIdsToScriptFactories.put(bundle.getBundleId(), factories);
    }

    public ScriptEngineFactory getFactoryForExtension(String extension) {
        if (StringUtils.isEmpty(extension)) {
            throw new IllegalArgumentException("Null or empty extension");
        }

        return extensionsToScriptFactories.get(extension);
    }
}
