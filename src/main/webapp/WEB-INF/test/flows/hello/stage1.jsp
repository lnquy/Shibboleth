<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
<title>Stage 1</title>
</head>

<body>

	<h1>Stage1</h1>
	
    <form method="post" action="${flowExecutionUrl}">
       <input type="submit" name="_eventId_next" value="Go To Next" />
       <input type="submit" name="_eventId_back" value="Go Back" />
    </form>

</body>

</html>