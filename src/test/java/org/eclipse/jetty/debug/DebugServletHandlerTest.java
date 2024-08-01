package org.eclipse.jetty.debug;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.toolchain.test.MavenPaths;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(WorkDirExtension.class)
public class DebugServletHandlerTest
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugServletHandlerTest.class);
    public WorkDir workDir;
    private Server server;
    private HttpClient client;

    @BeforeEach
    public void startClient() throws Exception
    {
        client = new HttpClient();
        client.start();
    }

    public void startServer(Handler handler) throws Exception
    {
        server = new Server(0);
        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    public void stopAll()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    @Test
    public void testDefaultServletDebug() throws Exception
    {
        Path basedir = workDir.getEmptyPathDir();
        Files.writeString(basedir.resolve("hello.txt"), "Hello from tests", UTF_8);

        Path dumpJsp = MavenPaths.findTestResourceFile("jsps/dump.jsp");
        Files.copy(dumpJsp, basedir.resolve("dump.jsp"));

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setServletHandler(new DebugServletHandler());
        contextHandler.setContextPath("/");
        Resource base = Resource.newResource(basedir);
        contextHandler.setBaseResource(base);

        // Since this is a ServletContextHandler we must manually configure JSP support.
        enableEmbeddedJspSupport(contextHandler);

        ServletHolder defHolder = new ServletHolder("default", DefaultServlet.class);
        contextHandler.addServlet(defHolder, "/");

        FilterHolder debugChainHolder = new FilterHolder(DebugChainFilter.class);
        debugChainHolder.setName("debug-chain");
        contextHandler.addFilter(debugChainHolder, "/*", EnumSet.allOf(DispatcherType.class));

        FilterHolder identityFilter = new FilterHolder((request, response, chain) -> chain.doFilter(request, response));
        identityFilter.setName("identity");
        contextHandler.addFilter(identityFilter, "/*", EnumSet.of(DispatcherType.REQUEST));

        FilterHolder wrapperHolder = new FilterHolder(WrapperFilter.class);
        wrapperHolder.setName("wrappers");
        contextHandler.addFilter(wrapperHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

        FilterHolder debugWrappersHolder = new FilterHolder(DebugWrappersFilter.class);
        debugWrappersHolder.setName("debug-wrappers");
        contextHandler.addFilter(debugWrappersHolder, "*.jsp", EnumSet.allOf(DispatcherType.class));

        startServer(contextHandler);

        ContentResponse response = client.newRequest(server.getURI().resolve("/dump.jsp"))
            .method("GET")
            .headers((headers) ->
            {
                headers.add("Accept", "*/*");
            })
            .send();
        assertEquals(200, response.getStatus());
    }

    /**
     * Setup JSP Support for ServletContextHandlers.
     * <p>
     * NOTE: This is not required or appropriate if using a WebAppContext.
     * </p>
     *
     * @param servletContextHandler the ServletContextHandler to configure
     * @throws IOException if unable to configure
     */
    private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException
    {
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        // Manually call JettyJasperInitializer on context startup
        servletContextHandler.addBean(new EmbeddedJspStarter(servletContextHandler));

        // Create / Register JSP Servlet (must be named "jsp" per spec)
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("scratchdir", scratchDir.toString());
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        servletContextHandler.addServlet(holderJsp, "*.jsp");

        servletContextHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
    }
}
