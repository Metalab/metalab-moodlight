package at.metalab.moodlight;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.fusesource.mqtt.client.MQTT;

public class MoodlightMain {

	private final static Logger LOG = Logger.getLogger(MoodlightMain.class.getName());

	public static void main(String[] args) throws Exception {
		// if you want to override the host/port and or the
		// topic to which the colors
		// are sent you can set the following JVM System properties:
		// -Dmoodlight.mqttHost=... (defaults to 127.0.0.1)
		// -Dmoodlight.mqttPort=... (defaults to 1883)
		// -Dmoodlight.mqttTopic=... (default to ESP_RGB_1)

		MQTT mqtt = new MQTT();

		String mqttHost = "tcp://" + System.getProperty("moodlight.mqttHost", "127.0.0.1") + ":"
				+ System.getProperty("moodlight.mqttPort", "1883");

		String mqttTopic = System.getProperty("moodlight.mqttTopic", "ESP_RGB_1");

		LOG.info(String.format("Moodlight setting: using topic '%s' at '%s'", mqttHost, mqttTopic));

		try {
			mqtt.setHost(mqttHost);
		} catch (URISyntaxException uriSyntaxException) {
			LOG.severe("could not create mqtt object for: " + mqttHost);
		}

		final MqttUnreliablePublisher mqttPub = new MqttUnreliablePublisher(mqtt, mqttTopic);
		ColorSocket.mqttPub = mqttPub;

		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8083);
		server.addConnector(connector);
		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContext.setContextPath("/websocket");
		servletContext.setServer(server);

		ContextHandler setColor = new ContextHandler("/set_color/") {
			@Override
			public void doHandle(String arg0, Request arg1, HttpServletRequest arg2, HttpServletResponse arg3)
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
			ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(servletContext);
			wscontainer.addEndpoint(ColorSocket.class);

			// setup a resource handler to deliver the awesome slick web ui with
			// its
			// great ux :D
			ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setDirectoriesListed(false);
			resourceHandler.setWelcomeFiles(new String[] { "index.html" });
			resourceHandler.setBaseResource(Resource.newClassPathResource("static"));

			HandlerList handlers = new HandlerList();
			handlers.setHandlers(new Handler[] { servletContext, setColor, resourceHandler });
			server.setHandler(handlers);

			server.start();
			LOG.info("server started");
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
