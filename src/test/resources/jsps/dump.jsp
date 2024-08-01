<%@ page import="java.util.Enumeration" %>
<html>
<head>
<title>Dump Ping</title>
</head>
<body>
<h1>Dump Ping</h1>

<table border="1">
<tr><th>Method:</th><td><%= request.getMethod() %></td></tr>
<tr><th>Protocol:</th><td><%= request.getProtocol() %></td></tr>
<tr><th>Request URI:</th><td><%= request.getRequestURI() %></td></tr>
<tr><th>ServletPath:</th><td><%= request.getServletPath() %></td></tr>
<tr><th>PathInfo:</th><td><%= request.getPathInfo() %></td></tr>

<%
   Enumeration e =request.getParameterNames();
   while(e.hasMoreElements())
   {
       String name = (String)e.nextElement();
%>
<tr><th>getParameter("<%= name %>")</th><td><%= request.getParameter(name) %></td></tr>
<%
   }
%>

</table>
</body></html>
