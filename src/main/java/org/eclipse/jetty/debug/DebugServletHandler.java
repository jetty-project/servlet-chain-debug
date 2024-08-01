package org.eclipse.jetty.debug;

import javax.servlet.FilterChain;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugServletHandler extends ServletHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugServletHandler.class);

    @Override
    protected FilterChain newFilterChain(FilterHolder filterHolder, FilterChain chain)
    {
        FilterChain filterchain = super.newFilterChain(filterHolder, chain);
        LOG.debug("filterChain = {}", filterchain);
        return filterchain;
    }
}
