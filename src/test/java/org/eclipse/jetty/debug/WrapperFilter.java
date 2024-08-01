package org.eclipse.jetty.debug;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class WrapperFilter implements Filter
{
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            HttpServletResponse httpResponse = (HttpServletResponse)response;

            RequestWrapper requestWrapper = new RequestWrapper(httpRequest);
            ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
            AltResponseWrapper altResponseWrapper = new AltResponseWrapper(responseWrapper);
            chain.doFilter(requestWrapper, altResponseWrapper);
            return;
        }
        chain.doFilter(request, response);
    }

    public static class RequestWrapper extends HttpServletRequestWrapper
    {
        public RequestWrapper(HttpServletRequest request)
        {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException
        {
            return super.getInputStream();
        }
    }

    public static class ResponseWrapper extends HttpServletResponseWrapper
    {
        public ResponseWrapper(HttpServletResponse response)
        {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException
        {
            return super.getWriter();
        }
    }

    public static class AltResponseWrapper extends HttpServletResponseWrapper
    {
        public AltResponseWrapper(HttpServletResponse response)
        {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException
        {
            return super.getOutputStream();
        }
    }
}
