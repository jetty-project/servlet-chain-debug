package org.eclipse.jetty.debug;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugChainFilter implements Filter
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugChainFilter.class);
    private static final String DELIM = "\n  |";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            StringBuilder str = new StringBuilder();
            str.append(DELIM).append("requestURI=").append(httpRequest.getRequestURI());
            str.append(DELIM).append("dispatchType=").append(httpRequest.getDispatcherType());
            str.append(DELIM).append("chain=").append(Objects.toString(chain).replace("->", "\n      ->"));
            LOG.info(str.toString());
        }
        chain.doFilter(request, response);
    }
}
