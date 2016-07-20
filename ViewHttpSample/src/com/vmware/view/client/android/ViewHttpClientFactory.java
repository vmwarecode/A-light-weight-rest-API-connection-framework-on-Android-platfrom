package com.vmware.view.client.android;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.net.Proxy;
import android.util.Log;

class ViewHttpClientFactory {
	public static HttpClient makeHttpClient() {
		HttpClient client = null;
		// Set up default connection params.
		HttpParams connParams = new BasicHttpParams();
		ConnManagerParams.setMaxConnectionsPerRoute(connParams,
				new ConnPerRoute() {
					public int getMaxForRoute(HttpRoute route) {
						return MAX_CONNECTION_FOR_REOUTER;
					}
				});

		ConnManagerParams.setMaxTotalConnections(connParams, 20);

		try {
			KeyStore trustStore;

			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			// Set up scheme registry.
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", sf, 443));

			ClientConnectionManager cm = new ClientConnectionManager(
					connParams, schemeRegistry);

			// Set up client params.
			HttpParams httpParams = new BasicHttpParams();

			String host = Proxy.getDefaultHost();
			int port = Proxy.getDefaultPort();
			if (null != host && port != -1) {
				HttpHost httpHost = null;
				httpHost = new HttpHost(host, port);
				httpParams
						.setParameter(ConnRouteParams.DEFAULT_PROXY, httpHost);
			}
			HttpConnectionParams.setConnectionTimeout(httpParams,
					DEFAULT_TIMEOUT_MILLIS);
			HttpConnectionParams.setSoTimeout(httpParams,
					DEFAULT_TIMEOUT_MILLIS);
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);

			// define the user agent to distinguish differnt client type
			HttpProtocolParams.setUserAgent(httpParams, HTTP_USER_AGENT);

			client = new DefaultHttpClient(cm, httpParams);
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}

		return client;
	}

	private static class ClientConnectionManager extends
			ThreadSafeClientConnManager {
		public ClientConnectionManager(HttpParams params, SchemeRegistry schreg) {
			super(params, schreg);
		}

		@Override
		public ClientConnectionRequest requestConnection(HttpRoute route,
				Object state) {
			IdleConnectionMonitorThread.ensureRunning(this,
					KEEP_ALIVE_DURATION_SECS, KEEP_ALIVE_INTERVAL_SECS);
			return super.requestConnection(route, state);
		}
	}

	/**
	 * The thread will close the Expired&Unused connections every 300 seconds
	 */
	private static class IdleConnectionMonitorThread extends Thread {

		private final ClientConnectionManager manager;
		private final int idleTimeoutSeconds;

		private final int checkIntervalSeconds;
		private static IdleConnectionMonitorThread thread = null;

		public IdleConnectionMonitorThread(ClientConnectionManager manager,
				int idleTimeoutSeconds, int checkIntervalSeconds) {
			super();
			this.manager = manager;
			this.idleTimeoutSeconds = idleTimeoutSeconds;
			this.checkIntervalSeconds = checkIntervalSeconds;
		}

		public static synchronized void ensureRunning(
				ClientConnectionManager manager, int idleTimeoutSeconds,
				int checkIntervalSeconds) {
			if (thread == null) {
				thread = new IdleConnectionMonitorThread(manager,
						idleTimeoutSeconds, checkIntervalSeconds);
				thread.start();
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					synchronized (this) {
						wait(checkIntervalSeconds * 1000);
					}
					manager.closeExpiredConnections();
					manager.closeIdleConnections(idleTimeoutSeconds,
							TimeUnit.SECONDS);
					synchronized (IdleConnectionMonitorThread.class) {
						if (manager.getConnectionsInPool() == 0) {
							thread = null;
							return;
						}
					}
				}
			} catch (InterruptedException e) {
				thread = null;
			}
		}
	}

	private static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host,
					port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	/** How long connections are kept alive. */
	private static final int KEEP_ALIVE_DURATION_SECS = 25;

	/** How often the monitoring thread checks for connections to close. */
	private static final int KEEP_ALIVE_INTERVAL_SECS = 30;

	/** The default timeout for client connections in milliseconds. */
	private static final int DEFAULT_TIMEOUT_MILLIS = 30 * 1000;
	
	/** The maximum connection number. */
	private static final int MAX_CONNECTION_FOR_REOUTER = 6;

	/** the client agent **/
	private static final String HTTP_USER_AGENT = "View_Android_Client";
}
