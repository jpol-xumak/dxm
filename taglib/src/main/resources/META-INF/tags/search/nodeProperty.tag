<%@ tag body-content="empty" dynamic-attributes="attributes" description="Renders input control for the node property depending on its type (boolean, text, date, category)." %>
<%@ attribute name="display" required="false" type="java.lang.Boolean"
              description="Should we display an input control for this query element or create a hidden one? In case of the hidden input field, the value should be provided."
        %>
<%@ attribute name="nodeType" required="true" type="java.lang.String"
              description="The node type of this property." %>
<%@ attribute name="name" required="true" type="java.lang.String" description="The name of the property." %>
<%@ attribute name="value" required="false" type="java.lang.String" description="The initial value for the property" %>
<%@ attribute name="from" required="false" type="java.lang.String"
              description="For date properties. Initial value for date from in case of the range date type." %>
<%@ attribute name="to" required="false" type="java.lang.String"
              description="For date properties. Initial value for date to in case of the range date type." %>
<%@ attribute name="match" required="false" type="java.lang.String"
              description="For text properties. The match type for search term." %>
<%@ attribute name="includeChildren" required="false" type="java.lang.Boolean"
              description="For category properties. The include children initial value." %>
<%@ attribute name="displayIncludeChildren" required="false" type="java.lang.Boolean"
              description="For category properties. Display the include children checkbox." %>
              
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib"%>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib"%>
<%@ taglib prefix="search" uri="http://www.jahia.org/tags/search"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<c:set var="includeChildren" value="${functions:default(includeChildren, false)}"/>
<c:set var="displayIncludeChildren" value="${functions:default(displayIncludeChildren, false)}"/>
<c:set var="display" value="${functions:default(display, true)}"/>
<c:set var="propName" value="src_properties(${nodeType}).${name}.value"/>
<search:nodePropertyDescriptor nodeType="${nodeType}" name="${name}">
    <c:if test="${display}">
        <c:set var="value" value="${functions:default(param[propName], value)}"/>
        <c:set target="${attributes}" property="name" value="${propName}"/>
        <c:choose>
            <c:when test="${descriptor.type == 'BOOLEAN'}">
                <c:set target="${attributes}" property="type" value="checkbox"/>
                <c:set target="${attributes}" property="value" value="true"/>
                <c:if test="${value == 'true'}">
                    <c:set target="${attributes}" property="checked" value="checked"/>
                </c:if>
                <input ${functions:attributes(attributes)}/>
            </c:when>
            <c:when test="${descriptor.type == 'TEXT'}">
                <c:if test="${descriptor.constrained}">
                    <select ${functions:attributes(attributes)}>
                        <option value=""><fmt:message key="searchForm.any"/></option>
                        <c:forEach items="${descriptor.allowedValues}" var="allowedValue">
                            <fmt:message key='${fn:replace(nodeType, ":", "_")}.${name}.${allowedValue}' var='dispvalue'/>
                            <option value="${fn:escapeXml(allowedValue)}" ${value == allowedValue ? 'selected="selected"' : ''}>
                               ${fn:startsWith(dispvalue, '???') ? fn:escapeXml(allowedValue) : dispvalue}
                            </option>
                        </c:forEach>
                    </select>
                </c:if>
                <c:if test="${!descriptor.constrained}">
                    <c:set var="propName" value="src_properties(${nodeType}).${name}.match"/>
                    <c:set var="match" value="${functions:default(param[propName], match)}"/>
                    <select name="src_properties(${nodeType}).${name}.match">
                        <option value="any_word" ${'any_word' == match ? 'selected="selected"' : ''}><fmt:message key="searchForm.nodeProperty.match.any_word"/></option>
                        <option value="all_words" ${'all_words' == match ? 'selected="selected"' : ''}><fmt:message key="searchForm.nodeProperty.match.all_words"/></option>
                        <option value="exact_phrase" ${'exact_phrase' == match ? 'selected="selected"' : ''}><fmt:message key="searchForm.nodeProperty.match.exact_phrase"/></option>
                        <option value="without_words" ${'without_words' == match ? 'selected="selected"' : ''}><fmt:message key="searchForm.nodeProperty.match.without_words"/></option>
                    </select>
                    <input ${functions:attributes(attributes)} value="${fn:escapeXml(value)}"/>
                </c:if>
            </c:when>
            <c:when test="${descriptor.type == 'CATEGORY'}">
                <c:set var="propName" value="src_properties(${nodeType}).${name}.categoryValue.value"/>
                <c:set target="${attributes}" property="name" value="${propName}"/>
                <c:set target="${attributes}" property="id" value="${functions:default(attributes.id, propName)}"/>
                <c:set var="value" value="${functions:default(param[propName], value)}"/>
                <c:set var="categoryRoot"
                       value="${not empty descriptor.selectorOptions && not empty descriptor.selectorOptions.root ? descriptor.selectorOptions.root : '/sites/systemsite/categories'}"/>
                <c:set var="valuesrc_category_display" value="${functions:default(param['src_category_display'], '')}"/>       
                <input type="hidden" id="src_category_fieldid" name="${attributes.name}" value="${fn:escapeXml(value)}"/>
                <input value="${valuesrc_category_display}" id="src_category_display" name="src_category_display" readonly />
                <uiComponents:treeItemSelector fieldId="src_category_fieldid" displayFieldId="src_category_display" nodeTypes="jnt:category" selectableNodeTypes="jnt:category" root="${categoryRoot}"
	                      includeChildren="${includeChildren}" displayIncludeChildren="${displayIncludeChildren}" fieldIdIncludeChildren="category_src_includeChildren" fieldNameIncludeChildren="src_properties(${nodeType}).${name}.categoryValue.includeChildren" />
            </c:when>
            <c:when test="${descriptor.type == 'DATE'}">
                <search:date name="src_properties(${nodeType}).${name}.dateValue" value="${value}" from="${from}"
                        to="${to}"/>
            </c:when>
        </c:choose>
    </c:if>
    <c:if test="${!display}">
        <c:choose>
            <c:when test="${descriptor.type == 'BOOLEAN' && value == 'true'}">
                <input type="hidden" name="${propName}" value="${fn:escapeXml(value)}"/>
            </c:when>
            <c:when test="${descriptor.type == 'TEXT'}">
                <input type="hidden" name="${propName}" value="${fn:escapeXml(value)}"/>
                <c:if test="${not descriptor.constrained && not empty match}">
                    <input type="hidden" name="src_properties(${nodeType}).${name}.match" value="${fn:escapeXml(match)}"/>
                </c:if>
            </c:when>
            <c:when test="${descriptor.type == 'CATEGORY'}">
                <input type="hidden" name="src_properties(${nodeType}).${name}.categoryValue.value"
                       value="${fn:escapeXml(value)}"/>
                <c:set var="includeChildren" value="${not empty includeChildren ? includeChildren : 'true'}"/>
                <input type="hidden" name="src_properties(${nodeType}).${name}.categoryValue.includeChildren"
                       value="${fn:escapeXml(includeChildren)}"/>
            </c:when>
            <c:when test="${descriptor.type == 'DATE'}">
                <input type="hidden" name="src_properties(${nodeType}).${name}.dateValue.type" value="${fn:escapeXml(value)}"/>
                <c:if test="${value == 'range'}">
                    <c:if test="${not empty from}">
                        <input type="hidden" name="src_properties(${nodeType}).${name}.dateValue.from"
                               value="${fn:escapeXml(from)}"/>
                    </c:if>
                    <c:if test="${not empty to}">
                        <input type="hidden" name="src_properties(${nodeType}).${name}.dateValue.to" value="${fn:escapeXml(to)}"/>
                    </c:if>
                </c:if>
            </c:when>
        </c:choose>
    </c:if>
</search:nodePropertyDescriptor>
