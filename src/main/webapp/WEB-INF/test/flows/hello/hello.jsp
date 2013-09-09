<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
<title>Hello World</title>
</head>

<body>

	<h1>Hello World from Brent</h1>
	
	<form method="post" action="${flowExecutionUrl}">
	   <input type="hidden" name="_eventId" value="next" />
	   <input type="submit" value="Go To Next" />
	</form>

</body>

</html>