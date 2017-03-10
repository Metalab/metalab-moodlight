package at.metalab.moodlight;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint(value = "/colors/")
public class ColorSocket {

	private final static Logger LOG = Logger.getLogger(ColorSocket.class.getName());

	private final static Set<Session> SESSIONS = Collections.synchronizedSet(new HashSet<Session>());

	public static MqttUnreliablePublisher mqttPub;

	@OnOpen
	public void onWebSocketConnect(Session session) {
		LOG.info("Socket Connected: " + session);
		SESSIONS.add(session);
	}

	@OnMessage
	public void onWebSocketText(Session session, String message) {
		setColor(message);
	}

	private static long lastUpdate = System.currentTimeMillis();

	public synchronized static void setColor(String colorCommand) {
		if (System.currentTimeMillis() - lastUpdate < 500) {
			LOG.finest("skipping color change: " + colorCommand);
			return;
		}
		lastUpdate = System.currentTimeMillis();

		LOG.finest("received new color: " + colorCommand);

		String destination = colorCommand.substring(0, colorCommand.indexOf(':'));
		String color = colorCommand.substring(colorCommand.indexOf(':') + 1);

		if ("all".equals(destination) || "strip".equals(destination)) {
			LOG.info("setting color '" + color + "' on led strip");
			mqttPub.publish(color.toUpperCase());
		}

		if ("all".equals(destination) || "zumtobel".equals(destination)) {
			LOG.info("setting color '" + color + "' on zumtobel");

			String colorStr = "*" + color; // avoids changing the indices below :)

			byte r = Integer.valueOf(colorStr.substring(1, 3), 16).byteValue();
			byte g = Integer.valueOf(colorStr.substring(3, 5), 16).byteValue();
			byte b = Integer.valueOf(colorStr.substring(5, 7), 16).byteValue();

			ArtnetSender.send(r, g, b);
		}
	}

	@OnClose
	public void onWebSocketClose(Session session, CloseReason reason) {
		SESSIONS.remove(session);
		LOG.fine("Socket Closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}
}