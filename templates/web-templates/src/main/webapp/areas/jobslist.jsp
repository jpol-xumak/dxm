<%--


    This file is part of Jahia: An integrated WCM, DMS and Portal Solution
    Copyright (C) 2002-2009 Jahia Limited. All rights reserved.

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
    in Jahia's FLOSS exception. You should have recieved a copy of the text
    describing the FLOSS exception, and it is also available here:
    http://www.jahia.com/license

    Commercial and Supported Versions of the program
    Alternatively, commercial and supported versions of the program may be used
    in accordance with the terms contained in a separate written agreement
    between you and Jahia Limited. If you are unsure which license is appropriate
    for your use, please contact the sales department at sales@jahia.com.

--%>

<%@ page language="java" contentType="text/html;charset=UTF-8" %>

<%@ include file="../common/declarations.jspf" %>

<!--start content -->
<!-- Change id to modify positioning :  #position1=areaB/mainArea/areaA  #position2=areaB/mainArea #position3=mainArea/areaA   position4=mainArea only #position5=50%areaB/50% mainArea -->
<div id="position2">
    <div class="spacer">
                <template:include page="common/breadcrumb.jsp"/>
        <h2><c:out value="${requestScope.currentPage.highLightDiffTitle}" escapeXml="false"/></h2>
        <template:include page="modules/introduction/introductionDisplay.jsp"/>
<!--start jobsSearchForm -->
		<div class="jobsSearchForm">
		<template:jahiaPageForm name="jobPageForm" method="get">
			<fieldset><legend><fmt:message key='jobSearchForm'/></legend>
			<p class="jobsSearchKeyword"><label for="jobsSearchKeyword"><fmt:message key='job.keywordSearch'/></label>
			<input type="text" name="jobsSearchKeyword" id="jobsSearchKeyword" class="field jobsSearchKeyword" value="${param.jobsSearchKeyword}" tabindex="4" /></p>
			<div class="divButton"><input type="submit" name="submit" id="submit" class="button" value="<fmt:message key="search"/>" tabindex="5" /></div>
			</fieldset>
		</template:jahiaPageForm>
		</div>
<!--stop jobsSearchForm -->
        <template:include page="modules/job/jobDisplay.jsp"/>
    </div>
</div><!--stopContent-->