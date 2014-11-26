/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.apache.jackrabbit.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.webdav.DavConstants;

/**
 * Credentials provider that extracts the credentials information from the "Authorization" header (basic authentication type) and also
 * supports impersonation.
 * 
 * @author Sergiy Shyrkov
 */
public class JahiaBasicCredentialsProvider extends BasicCredentialsProvider {

    public static final String IMPERSONATOR = " impersonator ";

    public JahiaBasicCredentialsProvider(String defaultHeaderValue) {
        super(defaultHeaderValue);
    }

    @Override
    public Credentials getCredentials(HttpServletRequest request) throws LoginException, ServletException {
        String authHeader = request.getHeader(DavConstants.HEADER_AUTHORIZATION);
        if (authHeader != null) {
            try {
                String[] authStr = authHeader.split(" ");
                if (authStr.length >= 2 && authStr[0].equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Base64.decode(authStr[1].toCharArray(), out);
                    String decAuthStr = out.toString("ISO-8859-1");
                    int pos = decAuthStr.indexOf(':');
                    String userid = decAuthStr.substring(0, pos);
                    String passwd = decAuthStr.substring(pos + 1);
                    return createCredentials(userid, passwd.toCharArray());
                }
                throw new ServletException("Unable to decode authorization.");
            } catch (IOException e) {
                throw new ServletException("Unable to decode authorization: " + e.toString());
            }
        }

        return super.getCredentials(request);
    }

    /**
     * Creates the {@link SimpleCredentials} object for the provided username and password considering the impersonation case.
     * 
     * @param user
     *            the received username
     * @param password
     *            the user password
     * @return the {@link SimpleCredentials} object for the provided username and password considering the impersonation case
     */
    protected Credentials createCredentials(String user, char[] password) {
        SimpleCredentials credentials = null;
        if (user != null && user.contains(IMPERSONATOR)) {
            credentials = new SimpleCredentials(StringUtils.substringBefore(user, IMPERSONATOR),
                    ArrayUtils.EMPTY_CHAR_ARRAY);

            credentials.setAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE,
                    new SimpleCredentials(StringUtils.substringAfter(user, IMPERSONATOR), password));
        } else {
            credentials = new SimpleCredentials(user, password);
        }

        return credentials;
    }

}
