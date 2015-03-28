package at.metalab.moodlight;

import java.io.File;
import java.io.IOException;
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

	private static String path;

	private final static Logger LOG = Logger.getLogger(ColorSocket.class
			.getName());

	private final static Set<Session> SESSIONS = Collections
			.synchronizedSet(new HashSet<Session>());

	@OnOpen
	public void onWebSocketConnect(Session session) {
		LOG.info("Socket Connected: " + session);
		SESSIONS.add(session);
	}

	@OnMessage
	public void onWebSocketText(Session session, String message) {
		String r = String.valueOf(Integer.valueOf(message.substring(0, 2), 16));
		String g = String.valueOf(Integer.valueOf(message.substring(2, 4), 16));
		String b = String.valueOf(Integer.valueOf(message.substring(4, 6), 16));

		LOG.info(String.format("receive color: %s -> r=%s, g=%s, b=%s",
				message, r, g, b));

		setColor(r, g, b);
	}

	public static synchronized void setColor(String r, String g, String b) {
		LOG.info(String.format("setColor: r=%s, g=%s, b=%s", r, g, b));

		try {
			Process p = Runtime.getRuntime().exec(
					new String[] { "./set_color", r, g, b }, new String[] {},
					new File(path));
			try {
				LOG.info("set_color exited with rc = " + p.waitFor());
			} catch (InterruptedException interruptedException) {

			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
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

	public static void setPath(String path) {
		ColorSocket.path = path;
	}
}