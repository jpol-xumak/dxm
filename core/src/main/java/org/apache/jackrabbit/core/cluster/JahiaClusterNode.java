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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.*;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Extends default clustered node implementation. Add support for NodeLevelLockableJournal
 */
public class JahiaClusterNode extends ClusterNode {

    /**
     * Status constant.
     */
    private static final int NONE = 0;

    /**
     * Status constant.
     */
    private static final int STARTED = 1;

    /**
     * Status constant.
     */
    private static final int STOPPED = 2;

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(JahiaClusterNode.class);

    /**
     * Status flag, one of {@link #NONE}, {@link #STARTED} or {@link #STOPPED}.
     */
    private volatile int status = NONE;


    /**
     * Starts this cluster node.
     *
     * @throws org.apache.jackrabbit.core.cluster.ClusterException if an error occurs
     */
    @Override
    public synchronized void start() throws ClusterException {
        if (status == NONE) {
            super.start();
            status = STARTED;
        }
    }

    /**
     * Stops this cluster node.
     */
    @Override
    public synchronized void stop() {
        status = STOPPED;
        super.stop();
        Journal j = getJournal();
        if (j != null && (j instanceof AbstractJournal)) {
            String revisionFile = ((AbstractJournal) j).getRevision();
            if (revisionFile != null) {
                InstanceRevision currentFileRevision = null;
                try {
                    currentFileRevision = new FileRevision(new File(revisionFile));
                    long rev = getRevision();
                    currentFileRevision.set(rev);
                    log.info("Written local revision {} into revision file", rev);
                } catch (JournalException e) {
                    if (log.isDebugEnabled()) {
                        log.error("Unable to write local revision into a file: " + e.getMessage(), e);
                    } else {
                        log.error("Unable to write local revision into a file: {}", e.getMessage());
                    }
                } finally {
                    if (currentFileRevision != null) {
                        currentFileRevision.close();
                    }
                }
            }
        }
    }

    /**
     * Create an {@link UpdateEventChannel} for some workspace.
     *
     * @param workspace workspace name
     * @return lock event channel
     */
    @Override
    public UpdateEventChannel createUpdateChannel(String workspace) {
        return new WorkspaceUpdateChannel(workspace);
    }


    /**
     * Workspace update channel.
     */
    private class WorkspaceUpdateChannel extends ClusterNode.WorkspaceUpdateChannel implements UpdateEventChannel {

        /**
         * Create a new instance of this class.
         *
         * @param workspace workspace name
         */
        public WorkspaceUpdateChannel(String workspace) {
            super(workspace);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateCreated(Update update) throws ClusterException {
            if (status != STARTED) {
                log.info("not started: update create ignored.");
                return;
            }

            super.updateCreated(update);

            try {
                storeNodeIds(update);
                lockNodes(update);
            } catch (JournalException e) {
                throw new ClusterException("Unable to create log entry: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ClusterException("Unexpected error while creating log entry: " + e.getMessage(), e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateCommitted(Update update, String path) {
            if (status != STARTED) {
                log.info("not started: update commit ignored.");
                return;
            }
            try {
                super.updateCommitted(update, path);
            } finally {
                try {
                    unlockNodes(update);
                } catch (JournalException e) {
                    log.error("Unable to commit log entry: " + e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Unexpected error while committing log entry: " + e.getMessage(), e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateCancelled(Update update) {
            if (status != STARTED) {
                log.info("not started: update cancel ignored.");
                return;
            }
            try {
                super.updateCancelled(update);
            } finally {
                try {
                    unlockNodes(update);
                } catch (JournalException e) {
                    log.error("Unable to cancel log entry: " + e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Unexpected error while cancelling log entry: " + e.getMessage(), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void unlockNodes(Update update) throws JournalException {
        Journal journal = getJournal();
        if (journal instanceof NodeLevelLockableJournal) {
            Set<NodeId> ids = (Set<NodeId>) update.getAttribute("allIds");
            ((NodeLevelLockableJournal) journal).unlockNodes(ids);
        }
    }

    @SuppressWarnings("unchecked")
    private void lockNodes(Update update) throws JournalException {
        Journal journal = getJournal();
        if (journal instanceof NodeLevelLockableJournal) {
            Set<NodeId> ids = (Set<NodeId>) update.getAttribute("allIds");
            ((NodeLevelLockableJournal) journal).lockNodes(ids);
        }
    }

    private void storeNodeIds(Update update) {
        if (getJournal() instanceof NodeLevelLockableJournal) {
            Set<NodeId> nodeIdList = new HashSet<NodeId>();
            for (ItemState state : update.getChanges().addedStates()) {
                // For added states we always lock the parent, whatever the type. The node itself does not exist yet,
                // oes not need to be locked - only the parent will be modified
                nodeIdList.add(state.getParentId());
            }
            for (ItemState state : update.getChanges().modifiedStates()) {
                // Lock the modified node - take the parent node if state is a property
                if (state.isNode()) {
                    nodeIdList.add((NodeId) state.getId());
                } else {
                    nodeIdList.add(state.getParentId());
                }
            }
            for (ItemState state : update.getChanges().deletedStates()) {
                // Lock the deleted node - take the parent node if state is a property, otherwise lock node and its
                // parent
                if (state.isNode()) {
                    nodeIdList.add(state.getParentId());
                    nodeIdList.add((NodeId) state.getId());
                } else {
                    nodeIdList.add(state.getParentId());
                }
            }
            update.setAttribute("allIds", nodeIdList);
        }
    }

    @Override
    public void process(ChangeLogRecord record) {
        super.process(new ChangeLogRecord(new ExternalChangeLog(record.getChanges()), record.getEvents(), null, record.getWorkspace(), record.getTimestamp(), record.getUserData()));
    }
    
    @Override
    public void process(NamespaceRecord record) {
        NodeTypeRegistry.getInstance().getNamespaces().put( record.getNewPrefix() , record.getUri());
        super.process(record);
    }

    @Override
    public void process(NodeTypeRecord record) {
        try {
            // In case of any change in the registered nodetypes, reread the provider nodetype registry
            JCRStoreService.getInstance().reloadNodeTypeRegistry();
        } catch (RepositoryException e) {
            String msg = "Unable to register nodetypes : " + e.getMessage();
            log.error(msg);
        }
        super.process(record);
    }

    @Override
    public void setRevision(long revision) {
        // Revision will be set by the NodeLevelLockableJournal earlier by calling reallySetRevision.
        // Ignore all ClusterNode internal call to setRevision
        if (!(getJournal() instanceof NodeLevelLockableJournal)) {
            super.setRevision(revision);
        }
    }

    public void reallySetRevision(long revision) {
        // Should be called by NodeLevelLockableJournal when syncing
        log.debug("Set revision : " + revision);
        super.setRevision(revision);
    }


    public static class ExternalChangeLog extends ChangeLog {
        public ExternalChangeLog(ChangeLog changes) {
            for (ItemState state : changes.addedStates()) {
                added(state);
            }
            for (ItemState state : changes.deletedStates()) {
                deleted(state);
            }
            for (ItemState state : changes.modifiedStates()) {
                modified(state);
            }
            for (NodeReferences ref : changes.modifiedRefs()) {
                modified(ref);
            }
        }
    }
}
