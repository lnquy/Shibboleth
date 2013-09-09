<%@page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Application Context Info Page</title>

<style type="text/css">

table {
    border-spacing: 0;
    border-collapse: collapse;
}
td, th {
    padding: 3px;
    text-align: left;
    border: 1px solid black;
}

tr:nth-of-type(odd) { background-color: white; }

tr:nth-of-type(even) { background-color: lightgrey; }

</style>

</head>

<body>

    <h1>Spring ApplicationContext Info</h1>
    
    <c:forEach var="appContext" items="${appContextList}">

    <p>Context name: ${appContext.getApplicationName()}</p>
    <p>Context : ${appContext}</p>
    <p>Context parent: ${appContext.getParent()}</p>

    <p>BeanDefinition count : ${appContext.getBeanDefinitionCount()}</p>

    <p>Beans:</p>

    <table>

        <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Singleton</th>
            <th>Prototype</th>
        </tr>

        <c:forEach items="${appContext.getBeanDefinitionNames()}" var="beanName">
            <tr>
                <td>${beanName}</td>
                <td>${appContext.getType(beanName).getName()}</td>
                <td>${appContext.isSingleton(beanName)}</td>
                <td>${appContext.isPrototype(beanName)}</td>
            </tr>
        </c:forEach>

    </table>
    
    <br />
    <br />
    
    <hr size="5px" />
    
    </c:forEach>

</body>

</html>
