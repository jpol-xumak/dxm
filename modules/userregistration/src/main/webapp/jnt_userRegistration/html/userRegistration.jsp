<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js"/>
<c:if test="${not renderContext.loggedIn}">
    <script type="text/javascript">
        $(document).ready(function() {
            $("#newUser").validate({
                rules: {
                    desired_firstname: "required",
                    desired_lastname: "required",
                    desired_login: {
                        required: true,
                        minlength: 2
                    },
                    desired_password: {
                        required: true,
                        minlength: 5
                    },
                    confirm_password: {
                        required: true,
                        minlength: 5,
                        equalTo: "#desired_password"
                    },
                    desired_email: {
                        required: true,
                        email: true
                    }
                },
                messages: {
                    desired_firstname: "Please enter your firstname",
                    desired_lastname: "Please enter your lastname",
                    desired_login: {
                        required: "Please enter a username",
                        minlength: "Your username must consist of at least 2 characters"
                    },
                    desired_password: {
                        required: "Please provide a password",
                        minlength: "Your password must be at least 5 characters long"
                    },
                    confirm_password: {
                        required: "Please provide a password",
                        minlength: "Your password must be at least 5 characters long",
                        equalTo: "Please enter the same password as above"
                    },
                    desired_email: "Please enter a valid email address"
                }
            });
        });
    </script>
    <form method="post" action="${url.base}${currentNode.path}.newUser.do" name="newUser" id="newUser">
        <input type="hidden" name="userredirectpage" value="${currentNode.properties['userRedirectPage'].node.path}"/>
        <c:if test="${not empty currentNode.properties['from']}">
            <input type="hidden" name="from" value="${currentNode.properties['from'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['to']}">
            <input type="hidden" name="to" value="${currentNode.properties['to'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['cc']}">
            <input type="hidden" name="cc" value="${currentNode.properties['cc'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['bcc']}">
            <input type="hidden" name="bcc" value="${currentNode.properties['bcc'].string}"/>
        </c:if>
        <input type="hidden" name="toAdministrator" value="${currentNode.properties['ToAdministrator'].string}"/>

        <h3 class="boxdocspacetitleh3"><fmt:message key="userregistration.label.form.name"/></h3>
        <fieldset>
            <legend><fmt:message key="userregistration.label.form.name"/></legend>

            <p><label for="desired_login"><fmt:message key="userregistration.label.form.login"/></label>
                <input type="text" name="desired_login" id="desired_login" value="" tabindex="20"/></p>


            <p><label for="desired_password"><fmt:message
                    key="userregistration.label.form.password"/></label><input type="password" name="desired_password"
                                                                               id="desired_password"/></p>

            <p><label for="confirm_password"><fmt:message
                    key="userregistration.label.form.password"/></label><input type="password" name="confirm_password"
                                                                               id="confirm_password"/></p>

            <p><label for="desired_email"><fmt:message
                    key="userregistration.label.form.email"/></label><input type="text" name="desired_email"
                                                                            id="desired_email"/></p>

            <p><label for="desired_firstname"><fmt:message
                    key="userregistration.label.form.firstname"/></label><input type="text" name="desired_firstname"
                                                                                id="desired_firstname"/></p>

            <p><label for="desired_lastname"><fmt:message
                    key="userregistration.label.form.lastname"/></label><input type="text" name="desired_lastname"
                                                                               id="desired_lastname"/></p>

            <div>
                <input type="submit" class="button"
                       value="<fmt:message key="userregistration.label.form.create"/>"/>
            </div>
        </fieldset>
    </form>
</c:if>
