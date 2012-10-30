/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
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

package org.apache.jackrabbit.core.query.lucene;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.jackrabbit.core.query.lucene.join.SelectorRow;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetDISI;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.search.facets.JahiaQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Row;
import javax.jcr.query.qom.*;

import java.io.IOException;
import java.util.*;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;

/**
 * Override LuceneQueryFactory
 *
 * - optimize descendantNode constraint (use index)
 * - optimize childNode constraint (use index)
 * - handles rep:facet when executing (todo : might handle that in a constraint, as rep:filter ?)
 * - handles rep:filter in fulltext constraint  
 */
public class JahiaLuceneQueryFactoryImpl extends LuceneQueryFactory {
    private static Logger logger = LoggerFactory.getLogger(LazySelectorRow.class);   // <-- Added by jahia

    private JCRStoreProvider provider;
    private JCRSessionWrapper jcrSession;

    public JahiaLuceneQueryFactoryImpl(SessionImpl session, SearchIndex index, Map<String, Value> bindVariables) throws RepositoryException {
        super(session, index, bindVariables);
    }

    /**
     * Override LuceneQueryFactory.execute()
     */
    @Override
    public List<Row> execute(Map<String, PropertyValue> columns,
            Selector selector, Constraint constraint, Sort sort,
            boolean externalSort, long offsetIn, long limitIn)
            throws RepositoryException, IOException {
        final IndexReader reader = index.getIndexReader(true);
        final int offset = offsetIn < 0 ? 0 : (int) offsetIn;
        final int limit = limitIn < 0 ? Integer.MAX_VALUE : (int) limitIn;

        QueryHits hits = null;
        try {
            JackrabbitIndexSearcher searcher = new JackrabbitIndexSearcher(
                    session, reader, index.getContext().getItemStateManager());
            searcher.setSimilarity(index.getSimilarity());

            Predicate filter = Predicate.TRUE;
            BooleanQuery query = new BooleanQuery();

            QueryPair qp = new QueryPair(query);

            query.add(create(selector), MUST);
            if (constraint != null) {
                String name = selector.getSelectorName();
                NodeType type =
                    ntManager.getNodeType(selector.getNodeTypeName());
                filter = mapConstraintToQueryAndFilter(qp,
                        constraint, Collections.singletonMap(name, type),
                        searcher, reader);
            }


            // Added by jahia
            Set<String> foundIds = new HashSet<String>();
            int hasFacets = FacetHandler.hasFacetFunctions(columns, session);
            BitSet bitset = (hasFacets & FacetHandler.FACET_COLUMNS) == 0 ? null : new BitSet();            
            // End

            List<Row> rowList = externalSort ? new LinkedList<Row>() : null;
            Map<String, Row> rows = externalSort ? null : new LinkedHashMap<String, Row>();
            hits = searcher.evaluate(qp.mainQuery, sort, offset+limit);
            int currentNode = 0;
            int addedNodes = 0;

            ScoreNode node = hits.nextScoreNode();
            Map<String, Boolean> checkedAcls = new HashMap<String, Boolean>();

            while (node != null) {
                String[] infos = getIndexedNodeInfo(node, reader);
                if (foundIds.add(infos[0])) {  // <-- Added by jahia
                    try {
                        String[] acls = infos[1] != null ? infos[1].split(" ") : new String[0];
                        boolean canRead = true;

                        for (String acl : acls) {
                            Boolean aclChecked = checkedAcls.get(acl);
                            if (aclChecked == null) {
                                try {
                                    canRead = session.getAccessManager().canRead(null, new NodeId(acl));
                                    checkedAcls.put(acl, canRead);
                                } catch (RepositoryException e) {
                                }
                            } else {
                                canRead = aclChecked;
                            }
                            if (canRead) {
                                break;
                            }
                        }
                        if (canRead
                                && (!Constants.LIVE_WORKSPACE.equals(session
                                        .getWorkspace().getName())
                                        || infos[3] == null || "true"
                                            .equals(infos[3]))) {
                            if (filter == Predicate.TRUE) { // <-- Added by jahia
                                if ((hasFacets & FacetHandler.FACET_COLUMNS) == FacetHandler.FACET_COLUMNS) {
                                    try {
                                        bitset.set(node.getDoc(reader)); // <-- Added by jahia
                                    } catch (IOException e) {
                                        logger.debug("Can't retrive bitset from hits", e);
                                    }
                                }
                                
                                if ((hasFacets & FacetHandler.ONLY_FACET_COLUMNS) == 0) {
                                    Row row = null;

                                    if ("1".equals(infos[2])) {
                                        NodeImpl objectNode = session
                                                .getNodeById(node.getNodeId());
                                        if (objectNode
                                                .isNodeType("jnt:translation")) {
                                            objectNode = (NodeImpl) objectNode
                                                    .getParent();
                                        }
                                        row = new LazySelectorRow(
                                                columns,
                                                evaluator,
                                                selector.getSelectorName(),
                                                provider.getNodeWrapper(
                                                        objectNode, jcrSession),
                                                node.getScore());
                                    } else {
                                        row = new LazySelectorRow(columns,
                                                evaluator,
                                                selector.getSelectorName(),
                                                node.getNodeId(),
                                                node.getScore());
                                    }

                                    if (externalSort) {
                                        rowList.add(row);
                                    } else {
                                        // apply limit and offset rules locally
                                        if (currentNode >= offset
                                                && currentNode - offset < limit) {
                                            rows.put(node.getNodeId()
                                                    .toString(), row);
                                            addedNodes++;
                                        }
                                        currentNode++;
                                        // end the loop when going over the limit
                                        if (addedNodes == limit) {
                                            break;
                                        }
                                    }
                                }
                            } else {
                                NodeImpl objectNode = session.getNodeById(node
                                        .getNodeId());
                                if (objectNode.isNodeType("jnt:translation")) {
                                    objectNode = (NodeImpl) objectNode
                                            .getParent();
                                }
                                Row row = new SelectorRow(columns, evaluator,
                                        selector.getSelectorName(),
                                        provider.getNodeWrapper(objectNode,
                                                jcrSession), node.getScore());
                                if (filter.evaluate(row)) {
                                    if ((hasFacets & FacetHandler.ONLY_FACET_COLUMNS) == 0) {
                                        if (externalSort) {
                                            rowList.add(row);
                                        } else {
                                            // apply limit and offset rules locally
                                            if (currentNode >= offset
                                                    && currentNode - offset < limit) {
                                                rows.put(node.getNodeId()
                                                        .toString(), row);
                                                addedNodes++;
                                            }
                                            currentNode++;
                                            // end the loop when going over the limit
                                            if (addedNodes == limit) {
                                                break;
                                            }
                                        }
                                    }
                                    if ((hasFacets & FacetHandler.FACET_COLUMNS) == FacetHandler.FACET_COLUMNS) {
                                        try {
                                            bitset.set(node.getDoc(reader)); // <-- Added by jahia
                                        } catch (IOException e) {
                                            logger.debug(
                                                    "Can't retrive bitset from hits",
                                                    e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (PathNotFoundException e) {
                    } catch (ItemNotFoundException e) {
                        // skip the node
                    }
                } else {
                    if (((hasFacets & FacetHandler.ONLY_FACET_COLUMNS) == 0) && !externalSort && !infos[0].equals(node.getNodeId().toString())) {
                        // we've got the translation node -> adjusting the position of the original node in the result list  
                        rows.put(infos[0], rows.remove(infos[0]));
                    }
                }  // <-- Added by jahia
                node = hits.nextScoreNode();
            }

            if (rowList == null) {
                if (rows != null) {
                    rowList = new LinkedList<Row>(rows.values());
                } else {
                    rowList = new LinkedList<Row>();
                }
            }
            // Added by jahia
            if ((hasFacets & FacetHandler.FACET_COLUMNS) == FacetHandler.FACET_COLUMNS) {
                OpenBitSet docIdSet = new OpenBitSetDISI(new DocIdBitSet(bitset).iterator(), bitset.size());
                
                FacetHandler h = new FacetHandler(columns, selector, docIdSet, index, session);
                h.handleFacets(reader);
                rowList.add(0, h.getFacetsRow());
            }
            // End

            return rowList;
        } finally {
            if(hits != null){
                hits.close();
            }            
            Util.closeOrRelease(reader);
        }
    }

    /**
     * Get a String array of indexed fields for running quick checks 
     * [0] the uuid of the language independent node 
     * [1] the acl-id 
     * [2] "1" if visibility rule is set for node
     * [3] "true" node is published / "false" node is not published
     */
    private String[] getIndexedNodeInfo (ScoreNode sn, IndexReader reader) throws IOException {
        String[] info = new String[4];
        int docNb = sn.getDoc(reader);

        @SuppressWarnings("serial")
        Document doc = reader.document(docNb, new FieldSelector() {
            public FieldSelectorResult accept(String fieldName) {
                if (FieldNames.UUID == fieldName) {
                    return FieldSelectorResult.LOAD;
                } else if (JahiaNodeIndexer.TRANSLATED_NODE_PARENT == fieldName) {
                    return FieldSelectorResult.LOAD;
                } else if (FieldNames.PARENT == fieldName) {
                    return FieldSelectorResult.LOAD;
                } else if (JahiaNodeIndexer.ACL_UUID == fieldName) {
                    return FieldSelectorResult.LOAD;
                } else if (JahiaNodeIndexer.CHECK_VISIBILITY == fieldName) {
                    return FieldSelectorResult.LOAD;                
                } else if (JahiaNodeIndexer.PUBLISHED == fieldName) {
                    return FieldSelectorResult.LOAD;                                    
                } else {
                    return FieldSelectorResult.NO_LOAD;
                }
            }
        });

        if (doc.getField(JahiaNodeIndexer.TRANSLATED_NODE_PARENT) != null) {
            info[0] = doc.getField(FieldNames.PARENT).stringValue();
        } else {
            info[0] = sn.getNodeId().toString();
        }
        Field aclUuidField = doc.getField(JahiaNodeIndexer.ACL_UUID);
        if (aclUuidField != null) {
            info[1] = aclUuidField.stringValue();
        }
        Field checkVisibilityField = doc.getField(JahiaNodeIndexer.CHECK_VISIBILITY);
        if (checkVisibilityField != null) {
            info[2] = checkVisibilityField.stringValue();
        }
        Field publishedField = doc.getField(JahiaNodeIndexer.PUBLISHED);
        if (publishedField != null) {
            info[3] = publishedField.stringValue();
        }        
        return info;
    }

    protected Query getNodeIdQuery(String field, String path) throws RepositoryException {
        BooleanQuery or = null;
        try {
            if (field.equals(FieldNames.PARENT)) {
                Query q1 = new JackrabbitTermQuery(
                        new Term(FieldNames.PARENT, session.getNode(path).getIdentifier()));
                Query q2 = new JackrabbitTermQuery(
                        new Term(JahiaNodeIndexer.TRANSLATED_NODE_PARENT, session.getNode(path).getIdentifier()));
                or = new BooleanQuery();
                or.add(q1, BooleanClause.Occur.SHOULD);
                or.add(q2, BooleanClause.Occur.SHOULD);
            } else {
                return super.getNodeIdQuery(field, path);
            }
        } catch (AccessDeniedException e) {
            return new JackrabbitTermQuery(new Term(FieldNames.UUID, "invalid-node-id")); // never matches
        } catch (PathNotFoundException e) {
            return new JackrabbitTermQuery(new Term(FieldNames.UUID, "invalid-node-id")); // never matches
        }
        return or;
    }

//    protected Query getDescendantNodeQuery(
//            DescendantNode dn, JackrabbitIndexSearcher searcher)
//            throws RepositoryException, IOException {
//
////        new DescendantSelfAxisQuery()
//        Query query = null;
//        try {
//            query = new JackrabbitTermQuery(
//                    new Term(JahiaNodeIndexer.ANCESTOR, session.getNode(dn.getAncestorPath()).getIdentifier()));
//        } catch (PathNotFoundException e) {
//            query = new JackrabbitTermQuery(new Term(FieldNames.UUID, "invalid-node-id")); // never matches
//        }
//        return query;
////        return super.getDescendantNodeQuery(dn, searcher);
//    }

    protected Query getFullTextSearchQuery(FullTextSearch fts) throws RepositoryException {
        Query qobj = null;

        if (StringUtils.startsWith(fts.getPropertyName(), "rep:filter(")) {
            try {
                StaticOperand expr = fts.getFullTextSearchExpression();
                if (expr instanceof Literal) {
                    QueryParser qp = new JahiaQueryParser(FieldNames.FULLTEXT, new KeywordAnalyzer());
                    qp.setLowercaseExpandedTerms(false);
                    qobj = qp.parse(((Literal) expr).getLiteralValue().getString());
                } else {
                    throw new RepositoryException("Unknown static operand type: " + expr);
                }
            } catch (ParseException e) {
                throw new RepositoryException(e);
            }
        } else {
            qobj = super.getFullTextSearchQuery(fts);
        }
        return qobj;
    }

    protected Predicate mapConstraintToQueryAndFilter(
            QueryPair query, Constraint constraint,
            Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher, IndexReader reader)
            throws RepositoryException, IOException {
        try {
            if (constraint instanceof DescendantNode) {
                query.subQuery.add(new TermQuery(new Term(JahiaNodeIndexer.TRANSLATED_NODE_PARENT, session.getNode(((DescendantNode) constraint).getAncestorPath()).getParent().getIdentifier())),
                        BooleanClause.Occur.MUST_NOT);
            } else if (constraint instanceof ChildNode) {
                query.subQuery.add(new TermQuery(new Term(JahiaNodeIndexer.TRANSLATED_NODE_PARENT, session.getNode(((ChildNode)constraint).getParentPath()).getParent().getIdentifier())),
                        BooleanClause.Occur.MUST_NOT);
            }
        } catch (AccessDeniedException e) {
            // denied
            // todo : should find another way to test that we are not in a translation sub node
        } catch (PathNotFoundException e) {
            // not found
            query.subQuery.add(new JackrabbitTermQuery(new Term(
                    FieldNames.UUID, "invalid-node-id")), // never matches
                    MUST);
        }

        return super.mapConstraintToQueryAndFilter(query,constraint, selectorMap, searcher, reader);
    }

    public JCRStoreProvider getProvider() {
        return provider;
    }

    public void setProvider(JCRStoreProvider provider) {
        this.provider = provider;
    }

    public JCRSessionWrapper getJcrSession() {
        return jcrSession;
    }

    public void setJcrSession(JCRSessionWrapper jcrSession) {
        this.jcrSession = jcrSession;
    }

    @Override
    protected Query getComparisonQuery(DynamicOperand left, int transform,
            String operator, StaticOperand rigth,
            Map<String, NodeType> selectorMap) throws RepositoryException {
        if (left instanceof PropertyValue) {
            PropertyValue pv = (PropertyValue) left;
            if (pv.getPropertyName().equals("_PARENT")) {
                return new JackrabbitTermQuery(new Term(FieldNames.PARENT, getValueString(evaluator.getValue(rigth), PropertyType.REFERENCE)));
            }
        } 
        return super.getComparisonQuery(left, transform, operator, rigth, selectorMap);
    }

    class LazySelectorRow extends SelectorRow {   // <-- Added by jahia
        private Node node;
        private NodeId nodeId;

        LazySelectorRow(Map<String, PropertyValue> columns, OperandEvaluator evaluator, String selector, NodeId nodeId, double score) {
            super(columns, evaluator, selector, null, score);
            this.nodeId = nodeId;
        }        
        
        LazySelectorRow(Map<String, PropertyValue> columns, OperandEvaluator evaluator, String selector, Node node, double score) {
            super(columns, evaluator, selector, node, score);
            this.node = node;
        }

        @Override
        public Node getNode() {
            try {
                if (node == null) {
                    Node originalNode = session.getNodeById(nodeId);
                    if (originalNode.isNodeType("jnt:translation")) {
                        originalNode = originalNode.getParent();
                    }
                    if (originalNode != null) {
                        node = provider.getNodeWrapper(originalNode, jcrSession); 
                    }
                }
            } catch (ItemNotFoundException e) {
            } catch (PathNotFoundException e) {                
            } catch (RepositoryException e) {
                logger.error("Cannot get node "+nodeId,e);
            }
            return node;
        }

        @Override
        public Node getNode(String selectorName) throws RepositoryException {
            super.getNode(selectorName);
            return getNode();
        }
    }


}
