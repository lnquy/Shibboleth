<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="org.opensaml.profile.context.ErrorEventContext" %>
<%@ page import="org.owasp.esapi.Encoder" %>

<%
Encoder encoder = (Encoder) request.getAttribute("encoder");
ErrorEventContext errorEventContext = (ErrorEventContext) request.getAttribute("errorEventContext");
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
    
    	<h2>ERROR</h2>
        
<% if ( errorEventContext != null && errorEventContext.getEvent() != null) { %>
		<p>ERROR: <%= encoder.encodeForHTML(errorEventContext.getEvent().toString()) %></p>
<% } %>
        
    </body>
</html>