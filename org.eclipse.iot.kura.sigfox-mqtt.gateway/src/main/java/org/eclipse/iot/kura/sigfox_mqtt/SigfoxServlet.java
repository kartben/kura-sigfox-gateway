package org.eclipse.iot.kura.sigfox_mqtt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SigfoxServlet extends HttpServlet implements DataServiceListener {

	private static final long serialVersionUID = 4081710969213918924L;

	private static final String PREFIX = "sigfox/";

	public class SigfoxMessage {

		protected JSONObject _json;
		protected int _inflightMQTTMessageId;
		protected boolean _published = false;

		/**
		 * Constructs a SigfoxMessage, wrapping a JSON document sent from Sigfox
		 * backend
		 * 
		 * @param json
		 *            a document similar to:
		 * 
		 *            <pre>
		 * {
		 *  "device" : "1A000",
		 *  "data" : "48656c6c6f",
		 *  "time" : 1444816990,
		 *  "rssi" : -125.00,
		 *  "position" : [44, 1]
		 *}
		 *            </pre>
		 */
		public SigfoxMessage(JSONObject json) {
			_json = json;
		}

		public String getDeviceID() throws JSONException {
			return _json.getString("device");
		}

		public String getPayload() throws JSONException {
			return _json.getString("data");
		}

		public long getTimestamp() throws JSONException {
			return _json.getLong("time");
		}

		public double getRSSI() throws JSONException {
			return _json.getDouble("rssi");
		}

		public double getLat() throws JSONException {
			return _json.getJSONArray("position").getDouble(0);
		}

		public double getLon() throws JSONException {
			return _json.getJSONArray("position").getDouble(1);
		}

		public boolean isPublished() throws KuraStoreException, JSONException {
			return _published;
		}
	}

	private static final int MAX_RECENT_MESSAGES = 10;

	private static final Logger _logger = LoggerFactory.getLogger(SigfoxServlet.class);
	private CircularFifoQueue<SigfoxMessage> _recentMessages = new CircularFifoQueue<>(MAX_RECENT_MESSAGES);

	private DataService _dataService;

	public SigfoxServlet(DataService dataService) {
		_dataService = dataService;
	}

	public final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = null;

		try {
			out = response.getWriter();
			JSONArray json = new JSONArray();

			for (SigfoxMessage m : _recentMessages) {
				json.put(new JSONObject(m));
			}

			out.print(json.toString());
			out.flush();
		} catch (IOException e) {
			_logger.error("Error while preparing JSON answer", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = request.getReader();
		String str;
		while ((str = br.readLine()) != null) {
			sb.append(str);
		}

		try {
			JSONObject json = new JSONObject(sb.toString());
			SigfoxMessage message = new SigfoxMessage(json);
			_recentMessages.add(message);
			dispatchMessage(message);

		} catch (JSONException e) {
			// e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);
	}

	private void dispatchMessage(SigfoxMessage message) throws JSONException {
		String deviceId = message.getDeviceID();

		String baseTopic = PREFIX + deviceId + "/";

		try {
			// publish message as QoS 1
			message._inflightMQTTMessageId = _dataService.publish(baseTopic + "message",
					message.getPayload().getBytes(), 1, false, 5);

			// update position (retained, QoS 0)
			_dataService.publish(baseTopic + "location/lat", new Double(message.getLat()).toString().getBytes(), 0,
					true, 5);
			_dataService.publish(baseTopic + "location/lon", new Double(message.getLon()).toString().getBytes(), 0,
					true, 5);
			// update RSSI (retained, QoS 0)
			_dataService.publish(baseTopic + "rssi", new Double(message.getRSSI()).toString().getBytes(), 0, true, 5);

		} catch (KuraStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectionEstablished() {
	}

	@Override
	public void onDisconnecting() {
	}

	@Override
	public void onDisconnected() {
	}

	@Override
	public void onConnectionLost(Throwable cause) {
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		for (SigfoxMessage msg : _recentMessages) {
			if (msg._inflightMQTTMessageId == messageId) {
				_logger.info("ACK MSG with id", messageId);
				msg._published = true;
			}
		}
	}

}
