package at.metalab.moodlight;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class MoodlightMain {

	private final static Logger LOG = Logger.getLogger(MoodlightMain.class
			.getName());

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8083);
		server.addConnector(connector);
		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler servletContext = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		servletContext.setContextPath("/websocket");
		servletContext.setServer(server);

		ColorSocket.setPath(args[0]);
		LOG.info(String.format("using set_color in %s", args[0]));

		ContextHandler setColor = new ContextHandler("/set_color/") {
			@Override
			public void doHandle(String arg0, Request arg1,
					HttpServletRequest arg2, HttpServletResponse arg3)
					throws IOException, ServletException {
				arg1.setHandled(true);
				arg3.setStatus(200);
				arg3.setContentType("text/plain");
				arg3.setCharacterEncoding("UTF-8");
				arg3.getWriter().write("ok");

				ColorSocket.setColor(arg0.substring(1));

				arg3.flushBuffer();
			}
		};
		try {
			ServerContainer wscontainer = WebSocketServerContainerInitializer
					.configureContext(servletContext);
			wscontainer.addEndpoint(ColorSocket.class);

			// setup a resource handler to deliver the awesome slick web ui with
			// its
			// great ux :D
			ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setDirectoriesListed(false);
			resourceHandler.setWelcomeFiles(new String[] { "index.html" });
			resourceHandler.setBaseResource(Resource
					.newClassPathResource("static"));

			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[] { servletContext, setColor,
					resourceHandler });
			server.setHandler(handlers);

			server.start();
			LOG.info("server started");
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
