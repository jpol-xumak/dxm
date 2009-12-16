<%--

    This file is part of Jahia: An integrated WCM, DMS and Portal Solution
    Copyright (C) 2002-2009 Jahia Solutions Group SA. All rights reserved.

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

    As a special exception to the terms and conditions of version 2.0 of
    the GPL (or any later version), you may redistribute this Program in connection
    with Free/Libre and Open Source Software ("FLOSS") applications as described
    in Jahia's FLOSS exception. You should have received a copy of the text
    describing the FLOSS exception, and it is also available here:
    http://www.jahia.com/license

    Commercial and Supported Versions of the program
    Alternatively, commercial and supported versions of the program may be used
    in accordance with the terms contained in a separate written agreement
    between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
    for your use, please contact the sales department at sales@jahia.com.

--%>
<%@ tag body-content="empty" description="Renders search term input control." %>
<%@ tag dynamic-attributes="attributes"%>
<%@ attribute name="display" required="false" type="java.lang.Boolean"
              description="Should we display an input control for this query element or create a hidden one? In case of the hidden input field, the value should be provided. [true]"
        %>
<%@ attribute name="value" required="false" type="java.lang.String" description="The initial value." %>
<%@ attribute name="match" required="false" type="java.lang.String" description="The match type for search terms. [as_is]" %>
<%@ attribute name="searchIn" required="false" type="java.lang.String"
              description="Comma separated list of fields to search in. [content]" %>
<%@ attribute name="searchInAllowSelection" required="false" type="java.lang.Boolean"
              description="Do we need to display search fields options to allow user selection? [false]" %>
<%@ attribute name="searchInSelectionOptions" required="false" type="java.lang.String"
              description="Comma separated list of fields to search in that are available for user selection.
              This option has effect only in case the searchInAllowSelection attribute is set to true. [content,metadata]" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search"%>

<c:set var="display" value="${functions:default(display, true)}"/>
<c:set var="formId" value='<%= findAncestorWithClass(this, (Class) request.getAttribute("org.jahia.tags.search.form.class")).toString() %>'/>
<c:set var="termIndex" value="${searchTermIndexes[formId]}"/>
<c:set target="${attributes}" property="type" value="${display ? 'text' : 'hidden'}"/>
<c:set var="key" value="src_terms[${termIndex}].term"/>
<c:set target="${attributes}" property="name" value="${key}"/>
<c:set var="value" value="${functions:default(param[key], value)}"/>
<c:set var="key" value="src_terms[${termIndex}].fields"/>
<c:set var="searchIn" value="${functions:default(param[key], searchIn)}"/>
<c:set var="searchInAllowSelection" value="${functions:default(searchInAllowSelection, false)}"/>
<c:set var="searchInSelectionOptions" value="${functions:default(searchInSelectionOptions, 'content,metadata')}"/>
<input ${functions:attributes(attributes)} value="${fn:escapeXml(value)}"/>
<c:if test="${not empty match && match != 'as_is'}">
    <c:set var="key" value="src_terms[${termIndex}].match"/>
    <input type="hidden" name="${key}" value="${fn:escapeXml(functions:default(param[key], match))}"/>
</c:if>
<c:if test="${searchInAllowSelection || not empty searchIn}">
    <s:termFields value="${searchIn}" selectionOptions="${searchInSelectionOptions}" display="${searchInAllowSelection}"/>
</c:if>
<c:set target="${searchTermIndexes}" property="${formId}" value="${searchTermIndexes[formId] + 1}"/>