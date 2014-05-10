<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.idp.authn.context.*" %>
<%@ page import="org.opensaml.profile.context.ProfileRequestContext" %>
<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>

<%
AuthenticationErrorContext authenticationErrorContext = (AuthenticationErrorContext) request.getAttribute("authenticationErrorContext");
AuthenticationWarningContext authenticationWarningContext = (AuthenticationWarningContext) request.getAttribute("authenticationWarningContext");
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
    
    	<h2>Testbed JSP Login</h2>
        
<% if ( authenticationErrorContext != null && !authenticationErrorContext.getExceptions().isEmpty()) { %>
		<p>ERROR: <%= HTMLEncoder.encodeForHTML(authenticationErrorContext.getExceptions().get(0).getMessage()) %></p>
<% } %>

<% if ( authenticationErrorContext != null && !authenticationErrorContext.getClassifiedErrors().isEmpty()) { %>
        <p>Classified errors: <%= HTMLEncoder.encodeForHTML(authenticationErrorContext.getClassifiedErrors().toString()) %></p>
<% } %>

<% if ( authenticationWarningContext != null && !authenticationWarningContext.getClassifiedWarnings().isEmpty()) { %>
        <p>Classified warnings: <%= HTMLEncoder.encodeForHTML(authenticationWarningContext.getClassifiedWarnings().toString()) %></p>
<% } %>
        
        <form action="<%= request.getAttribute("flowExecutionUrl") %>" method="post">
            Username: <input type="text" name="username" value=""/> <br/>
            Password: <input type="password" name="password" value=""/> <br/>
            
            <input type="submit" name="_eventId_proceed" value="Login"/>
            <input type="checkbox" name="donotcache" value="1" /> Don't Remember Login
        </form>
        
    </body>
</html>