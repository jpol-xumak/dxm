/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.content.nodetypes;

import org.hibernate.annotations.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;

/**
 * This class register cnd files in the DB so that they are accessible
 * for all nodes on a cluster.
 */
@Entity
@Table(name = "jahia_nodetypes_provider")
public class NodeTypesDBProvider {
    private static Logger logger = LoggerFactory.getLogger(NodeTypesDBProvider.class);

    private Integer id;
    private String filename;
    private String cndFile;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "jahia_external_provider_id_seq")
    @SequenceGenerator(initialValue = 1, allocationSize = 40, name = "jahia_external_provider_id_seq", sequenceName = "jahia_external_provider_id_seq")
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    @Column(nullable = false)
    @Index(name = "jahia_nodetypes_provider_ix1")
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Lob
    @Column(nullable = false)
    public String getCndFile() {
        return cndFile;
    }

    public void setCndFile(String cndFile) {
        this.cndFile = cndFile;
    }
}