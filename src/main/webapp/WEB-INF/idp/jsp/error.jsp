<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="net.shibboleth.utilities.java.support.codec.HTMLEncoder" %>


<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <body>
    
    	<h2>ERROR</h2>
        
<% if ( request.getAttribute("flowRequestContext").getCurrentEvent() != null) { %>
		<p>ERROR: <%= HTMLEncoder.encodeForHTML(request.getAttribute("flowRequestContext").getCurrentEvent().getId()) %></p>
<% } %>
        
    </body>
</html>