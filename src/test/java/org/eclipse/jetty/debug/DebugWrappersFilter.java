package org.eclipse.jetty.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugWrappersFilter implements Filter
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugWrappersFilter.class);
    private static final List<MethodDecl> REQUEST_OVERRIDES;
    private static final List<MethodDecl> RESPONSE_OVERRIDES;
    private static final String DELIM = "\n  |";

    static
    {
        REQUEST_OVERRIDES = new ArrayList<>();
        REQUEST_OVERRIDES.add(new MethodDecl("getInputStream", new Class[0]));
        REQUEST_OVERRIDES.add(new MethodDecl("getReader", new Class[0]));
        REQUEST_OVERRIDES.add(new MethodDecl("getDateHeader", new Class[]{ String.class }));
        REQUEST_OVERRIDES.add(new MethodDecl("getIntHeader", new Class[]{ String.class }));
        REQUEST_OVERRIDES.add(new MethodDecl("getHeader", new Class[]{ String.class }));
        REQUEST_OVERRIDES.add(new MethodDecl("getHeaders", new Class[]{ String.class }));

        RESPONSE_OVERRIDES = new ArrayList<>();
        RESPONSE_OVERRIDES.add(new MethodDecl("getOutputStream", new Class[0]));
        RESPONSE_OVERRIDES.add(new MethodDecl("getWriter", new Class[0]));
        RESPONSE_OVERRIDES.add(new MethodDecl("flushBuffer", new Class[0]));
        RESPONSE_OVERRIDES.add(new MethodDecl("reset", new Class[0]));
        RESPONSE_OVERRIDES.add(new MethodDecl("resetBuffer", new Class[0]));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            StringBuilder str = new StringBuilder();
            str.append(DELIM).append("requestURI=").append(httpRequest.getRequestURI());
            str.append(DELIM).append("dispatchType=").append(httpRequest.getDispatcherType());
            appendRequestState(str, "", httpRequest);
            appendResponseState(str, "", httpResponse);
            str.append(DELIM).append("chain=").append(Objects.toString(chain).replace("->", "\n      ->"));
            LOG.info(str.toString());
        }
        chain.doFilter(request, response);
    }

    private void appendRequestState(StringBuilder str, String indent, HttpServletRequest request)
    {
        if (request instanceof HttpServletRequestWrapper)
        {
            HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper)request;
            appendRequestWrapperState(str, indent, wrapper);
        }
        else
        {
            str.append(DELIM).append(indent).append("requestImpl=").append(request.getClass().getName());
        }
    }

    private void appendRequestWrapperState(StringBuilder str, String indent, HttpServletRequestWrapper wrapper)
    {
        str.append(DELIM).append(indent).append("requestWrapper=").append(wrapper.getClass().getName());

        Class<? extends HttpServletRequestWrapper> wrapperClass = wrapper.getClass();

        boolean overrides = false;
        for (MethodDecl methodDecl : REQUEST_OVERRIDES)
        {
            if (overridesMethod(wrapperClass, methodDecl.name, methodDecl.paramTypes))
            {
                if (!overrides)
                    str.append("(overrides:");
                str.append(methodDecl.name);
                overrides = true;
            }
        }
        if (overrides)
            str.append(")");

        ServletRequest request = wrapper.getRequest();
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            appendRequestState(str, indent + " ", httpRequest);
        }
        else
        {
            str.append(DELIM).append(indent).append(">request=").append(request.getClass().getName());
        }
    }

    private void appendResponseState(StringBuilder str, String indent, HttpServletResponse response)
    {
        if (response instanceof HttpServletResponseWrapper)
        {
            HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper)response;
            appendResponseWrapperState(str, indent, wrapper);
        }
        else
        {
            str.append(DELIM).append(indent).append("responseImpl=").append(response.getClass().getName());
        }
    }

    private void appendResponseWrapperState(StringBuilder str, String indent, HttpServletResponseWrapper wrapper)
    {
        str.append(DELIM).append(indent).append("responseWrapper=").append(wrapper.getClass().getName());

        Class<? extends HttpServletResponseWrapper> wrapperClass = wrapper.getClass();

        boolean overrides = false;
        for (MethodDecl methodDecl : RESPONSE_OVERRIDES)
        {
            if (overridesMethod(wrapperClass, methodDecl.name, methodDecl.paramTypes))
            {
                if (!overrides)
                    str.append("(overrides:");
                str.append(methodDecl.name);
                overrides = true;
            }
        }
        if (overrides)
            str.append(")");

        ServletResponse response = wrapper.getResponse();
        if (response instanceof HttpServletResponse)
        {
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            appendResponseState(str, indent + "  ", httpResponse);
        }
        else
        {
            str.append(DELIM).append(indent).append(">request=").append(response.getClass().getName());
        }
    }

    private boolean overridesMethod(Class<?> clazz, String methodName, Class<?>[] params)
    {
        try
        {
            clazz.getDeclaredMethod(methodName, params);
            return true;
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }
    }

    static class MethodDecl
    {
        String name;
        Class<?>[] paramTypes;

        public MethodDecl(String name, Class<?>[] paramTypes)
        {
            this.name = name;
            this.paramTypes = paramTypes;
        }
    }
}
