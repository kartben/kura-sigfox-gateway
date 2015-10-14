package org.eclipse.iot.kura.sigfox_mqtt;

import javax.servlet.ServletException;

import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SigfoxGatewayComponent implements DataServiceListener{
	
	private static final Logger s_logger = LoggerFactory.getLogger(SigfoxGatewayComponent.class);

	private HttpService _httpService;
	private DataService _dataService;

	private SigfoxServlet _sigfoxServlet;

	public void setHttpService(HttpService httpService) {
		this._httpService = httpService;
	}

	public void unsetHttpService(HttpService httpService) {
		this._httpService = null;
	}

	public void setDataService(DataService dataService) {
		this._dataService = dataService;
	}

	public void unsetDataService(DataService dataService) {
		this._dataService = null;
	}
	
	protected void activate(BundleContext context) {
		HttpContext httpContext = new DefaultHttpContext(_httpService.createDefaultHttpContext());
		
		try {
			_httpService.registerResources("/sigfox", "www/index.html", httpContext);
			_httpService.registerResources("/sigfox/css", "www/css", httpContext);
			_httpService.registerResources("/sigfox/js", "www/js", httpContext);
			
			_sigfoxServlet = new SigfoxServlet(_dataService);
			
			_httpService.registerServlet("/sigfox/api", _sigfoxServlet, null, httpContext);
		} catch (NamespaceException|ServletException e) {
			s_logger.error("Error registering sigfox app", e);
			throw new ComponentException(e);
		}
		s_logger.info("Sigfox activated");
	}
	
	protected void deactivate(BundleContext context) {
		_httpService.unregister("/sigfox/css");
		_httpService.unregister("/sigfox/js");
		_httpService.unregister("/sigfox/api");
		_httpService.unregister("/sigfox");
		
		s_logger.info("Sigfox deactivated");
	}

	public void onConnectionEstablished() {
		_sigfoxServlet.onConnectionEstablished();
	}

	public void onDisconnecting() {
		_sigfoxServlet.onDisconnecting();
	}

	public void onDisconnected() {
		_sigfoxServlet.onDisconnected();
	}

	public void onConnectionLost(Throwable cause) {
		_sigfoxServlet.onConnectionLost(cause);
	}

	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		_sigfoxServlet.onMessageArrived(topic, payload, qos, retained);
	}

	public void onMessagePublished(int messageId, String topic) {
		_sigfoxServlet.onMessagePublished(messageId, topic);
	}

	public void onMessageConfirmed(int messageId, String topic) {
		_sigfoxServlet.onMessageConfirmed(messageId, topic);
	}
}
