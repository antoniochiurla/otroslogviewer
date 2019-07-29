/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package pl.otros.web;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.jdesktop.swingx.JXTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.otros.logview.api.model.LogData;
import pl.otros.logview.api.model.LogDataCollector;
import pl.otros.logview.api.model.LogDataStore;
import pl.otros.logview.api.model.MarkerColors;

/**
 * Embedded HTTP/1.1 file server based on a non-blocking I/O model and capable of direct channel
 * (zero copy) data transfer.
 */
public class NHttpLogDataServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHttpLogDataServer.class);
	
	private boolean running;
	private HttpServer httpServer;

	private final JXTablesProvider jxTableProvider;

    public NHttpLogDataServer(final String address, final int port, final JXTablesProvider jxTableProvider) throws Exception {

        this.jxTableProvider = jxTableProvider;
		final IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        httpServer = ServerBootstrap.bootstrap()
                .setLocalAddress(InetAddress.getByName(address))
                .setListenerPort(port)
                .setServerInfo("Test/1.1")
                .setIOReactorConfig(config)
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .registerHandler("/ds*", new HttpDataStoreHandler(jxTableProvider))
                .registerHandler("*", new HttpResourceHandler())
                .create();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            	if(running){
            		httpServer.shutdown(5, TimeUnit.SECONDS);
            	}
            }
        });

    }
    
	public void start() throws IOException, InterruptedException {
		httpServer.start();
		running = true;
        LOGGER.info("Serving resources on " + httpServer.getEndpoint().getAddress());
	}
	
	public void stop() {
		httpServer.shutdown(5, TimeUnit.SECONDS);
		running = false;
	}
    
    public boolean isStarted() {
    	return running;
    }
    
    static class HttpDefaultHandler implements HttpAsyncRequestHandler<HttpRequest> {
        public HttpAsyncRequestConsumer<HttpRequest> processRequest(
                final HttpRequest request,
                final HttpContext context) {
            // Buffer request content in memory for simplicity
            return new BasicAsyncRequestConsumer();
        }

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
				throws HttpException, IOException {
            final HttpResponse response = httpExchange.getResponse();
            handleInternal(request, response, context);
            httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
		}

        private void handleInternal(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final HttpCoreContext coreContext = HttpCoreContext.adapt(context);
            final HttpConnection conn = coreContext.getConnection(HttpConnection.class);
            response.setStatusCode(HttpStatus.SC_OK);
            final NStringEntity body = new NStringEntity("Prova <B>Boldato</B>",ContentType.create("text/html", "UTF-8"));
            response.setEntity(body);
            LOGGER.debug(conn + ": serving default");
        }

    }
    
	static class HttpDataStoreHandler implements HttpAsyncRequestHandler<HttpRequest> {
		Pattern patternSelectDataStore = Pattern.compile("^/ds$");
//		Pattern patternListDataStoreContent = Pattern.compile("^/ds/(([^/]+))$");
//		Pattern patternGetLogDataContent = Pattern.compile("^/ds/([^/]+)/(([^/]+))$");
		Pattern patternListDataStoreContent = Pattern.compile("^/ds/(([0-9]+))$");
		Pattern patternGetLogDataContent = Pattern.compile("^/ds/([0-9]+)/(([0-9\\-]+))$");
		Pattern patternSelLogDataContent = Pattern.compile("^/ds/([0-9]+)/sel/(([0-9\\-]+))$");
        private JXTablesProvider jxTableProvider;

		public HttpDataStoreHandler(JXTablesProvider jxTableProvider) {
            super();
			this.jxTableProvider = jxTableProvider;
        }

        public HttpAsyncRequestConsumer<HttpRequest> processRequest(
                final HttpRequest request,
                final HttpContext context) {
            // Buffer request content in memory for simplicity
            return new BasicAsyncRequestConsumer();
        }

        public void handle(
                final HttpRequest request,
                final HttpAsyncExchange httpexchange,
                final HttpContext context) throws HttpException, IOException {
            final HttpResponse response = httpexchange.getResponse();
            try {
				handleInternal(request, response, context);
			} catch (JSONException e) {
				throw new ProtocolException("Error formatting JSON data");
			}
            httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
        }

        private void handleInternal(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException, JSONException {

            final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            final String target = request.getRequestLine().getUri();
            String decodedTarget = URLDecoder.decode(target, "UTF-8");
            Matcher matcherSelectDataStore = patternSelectDataStore.matcher(decodedTarget);
        	Matcher matcherListDataStoreContent = patternListDataStoreContent.matcher(decodedTarget);
        	Matcher matcherGetLogDataContent = patternGetLogDataContent.matcher(decodedTarget);
        	Matcher matcherSelLogDataContent = patternSelLogDataContent.matcher(decodedTarget);
            String jsonReturn = "";
            if(matcherSelectDataStore.find()){
            	JSONObject jsonReply = new JSONObject();
    			JSONArray jsonDataStores = new JSONArray();
            	LOGGER.debug("serving select:");
            	boolean first = true;
    			int tabCount = jxTableProvider.getCount();
    			long minTime = Long.MAX_VALUE;
    			long maxTime = 0L;
    			for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
    				JXTable jxTable = jxTableProvider.getJXTable(tabIndex);
    				LOGGER.debug("Component: {}", jxTable);
    				Component childComponent = jxTable.getComponentAt(100, 100);
    				LOGGER.debug("Child: {}", childComponent);
            		LOGGER.debug("DataStore: {}",jxTable);
            		jsonDataStores.put(tabIndex);
        			for(int row = 0; row < jxTable.getRowCount(); row++){
        				LogDataCollector logDataCollector = (LogDataCollector) jxTable.getModel();
        				LogData logData = logDataCollector.getLogData()[row];
        				long time = logData.getDate().getTime();
        				minTime = time < minTime ? time : minTime;
        				maxTime = time > maxTime ? time : maxTime;
        			}
            	}
    			jsonReply.put("dataStores", jsonDataStores);
    			
    			JSONObject jsonRanges = new JSONObject();
    			JSONObject jsonRangesTime = new JSONObject();
    			jsonRangesTime.put("min", minTime);
    			jsonRangesTime.put("max", maxTime);
    			JSONArray jsonRangesTimeSteps = generateJSONRangesTimeSteps(minTime, maxTime);
    			jsonRangesTime.put("steps", jsonRangesTimeSteps);
    			jsonRanges.put("time",jsonRangesTime);
    			jsonReply.put("ranges", jsonRanges);
    			
            	jsonReturn = jsonReply.toString();
            } else if(matcherListDataStoreContent.find()){
            	StringBuilder sb = new StringBuilder();
            	LOGGER.debug("serving dataStoreContent:");
            	int groups = matcherListDataStoreContent.groupCount();
            	for(int group = 0; group < groups; group++){
            		LOGGER.debug("group(" + group + "): " + matcherListDataStoreContent.group(group));
            	}
            	String indexAsString = matcherListDataStoreContent.group(1);
				int indexDataStore = Integer.valueOf(indexAsString).intValue();
				int fromIndex = 0;
				int toIndex = jxTableProvider.getJXTable(indexDataStore).getRowCount() - 1;
				jsonReturn = generateJSONFromDataStore(indexDataStore, fromIndex, toIndex);
            } else if(matcherGetLogDataContent.find()){
            	LOGGER.debug("serving dataStoreContent:");
            	int groups = matcherGetLogDataContent.groupCount();
            	for(int group = 0; group < groups; group++){
            		LOGGER.debug("group(" + group + "): " + matcherGetLogDataContent.group(group));
            	}
            	String indexDataStoreAsString = matcherGetLogDataContent.group(1);
				int indexDataStore = Integer.valueOf(indexDataStoreAsString).intValue();
				String indexLogDataAsString = matcherGetLogDataContent.group(2);
				String[] tokens = indexLogDataAsString.split("-");
				int fromIndex;
				int toIndex;
				if(tokens.length == 1){
					fromIndex = toIndex = Integer.valueOf(tokens[0]);
				} else {
					fromIndex = Integer.valueOf(tokens[0]).intValue();
					toIndex = Integer.valueOf(tokens[1]).intValue();
				}
				jsonReturn = generateJSONFromLogData(indexDataStore, fromIndex, toIndex);
            } else if(matcherSelLogDataContent.find()){
            	LOGGER.debug("serving dataStoreContent:");
            	int groups = matcherSelLogDataContent.groupCount();
            	for(int group = 0; group < groups; group++){
            		LOGGER.debug("group(" + group + "): " + matcherSelLogDataContent.group(group));
            	}
            	String indexDataStoreAsString = matcherSelLogDataContent.group(1);
            	int indexDataStore = Integer.valueOf(indexDataStoreAsString).intValue();
            	String indexLogDataAsString = matcherSelLogDataContent.group(2);
            	String[] tokens = indexLogDataAsString.split("-");
            	int fromIndex;
            	int toIndex;
            	if(tokens.length == 1){
            		fromIndex = toIndex = Integer.valueOf(tokens[0]);
            	} else {
            		fromIndex = Integer.valueOf(tokens[0]).intValue();
            		toIndex = Integer.valueOf(tokens[1]).intValue();
            	}
            	Runnable doRun = new Runnable() {
					@Override
					public void run() {
		            	final JXTable jxTable = jxTableProvider.getJXTable(indexDataStore);
		            	LOGGER.debug("Clear selection");
		            	jxTable.clearSelection();
		            	LOGGER.debug("Selecting rows from: {} to: {}", fromIndex, toIndex);
		            	jxTable.addRowSelectionInterval(fromIndex, toIndex);
		                Rectangle rect = jxTable.getCellRect(fromIndex, 0, true);
		                LOGGER.debug("Scroll to visible");
		                jxTable.scrollRectToVisible(rect);
					}
				};
				try {
					SwingUtilities.invokeAndWait(doRun);
				} catch (InvocationTargetException | InterruptedException e) {
					LOGGER.error("Selection on UI failed",e);
				}
                LOGGER.debug("Empty return");
            	jsonReturn = generateJSONArrayEmpty();
            }
            final NStringEntity body = new NStringEntity(jsonReturn,ContentType.create("text/json", "UTF-8"));
            response.setEntity(body);
        }

		private String generateJSONArrayEmpty() {
			JSONArray jsonArrayEmpty = new JSONArray();
			return jsonArrayEmpty.toString();
		}

		public JSONArray generateJSONRangesTimeSteps(long minTime, long maxTime) {
			JSONArray jsonRangesTimeSteps = new JSONArray();
			long rangeTime = maxTime - minTime;
			if(rangeTime < 1000){
				jsonRangesTimeSteps.put(100);
				jsonRangesTimeSteps.put(500);
			} else if(rangeTime < 5000){
				jsonRangesTimeSteps.put(100);
				jsonRangesTimeSteps.put(500);
				jsonRangesTimeSteps.put(1000);
			} else if(rangeTime < 60000){
				jsonRangesTimeSteps.put(500);
				jsonRangesTimeSteps.put(1500);
				jsonRangesTimeSteps.put(30000);
			} else if(rangeTime < 600_000){
				jsonRangesTimeSteps.put(1_000);
				jsonRangesTimeSteps.put(15_000);
				jsonRangesTimeSteps.put(30_000);
				jsonRangesTimeSteps.put(60_000);
			} else if(rangeTime < 60 * 60 * 1000){
				jsonRangesTimeSteps.put(20 * 1000);
				jsonRangesTimeSteps.put(5 * 60 * 1000);
				jsonRangesTimeSteps.put(15 * 60 * 1000);
				jsonRangesTimeSteps.put(30 * 60 * 1000);
			} else if(rangeTime < 2 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(30 * 1000);
				jsonRangesTimeSteps.put(15 * 60 * 1000);
				jsonRangesTimeSteps.put(30 * 60 * 1000);
				jsonRangesTimeSteps.put(1 * 60 * 60 * 1000);
			} else if(rangeTime < 6 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(1 * 60 * 1000);
				jsonRangesTimeSteps.put(30 * 60 * 1000);
				jsonRangesTimeSteps.put(1 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(3 * 60 * 60 * 1000);
			} else if(rangeTime < 12 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(15 * 60 * 1000);
				jsonRangesTimeSteps.put(1 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(3 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(6 * 60 * 60 * 1000);
			} else if(rangeTime < 24 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(30 * 60 * 1000);
				jsonRangesTimeSteps.put(2 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(6 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(12 * 60 * 60 * 1000);
			} else if(rangeTime < 2 * 24 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(60 * 60 * 1000);
				jsonRangesTimeSteps.put(4 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(12 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(24 * 60 * 60 * 1000);
			} else if(rangeTime < 4 * 24 * 60 * 60 * 1000){
				jsonRangesTimeSteps.put(2 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(6 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(24 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(2 * 24 * 60 * 60 * 1000);
			} else {
				jsonRangesTimeSteps.put(12 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(24 * 60 * 60 * 1000);
				jsonRangesTimeSteps.put(7 * 24 * 60 * 60 * 1000);
			}
			return jsonRangesTimeSteps;
		}

		public String generateJSONFromLogData(int indexDataStore, int fromIndex, int toIndex) throws JSONException {
			JSONObject jsonReturn = new JSONObject();
			JSONArray jsonLogDatas = new JSONArray();
			jsonReturn.put("logDatas", jsonLogDatas);
			JXTable jxTable = jxTableProvider.getJXTable(indexDataStore);
			LogDataCollector logDataCollector = (LogDataCollector) jxTable.getModel();
			for(int row = fromIndex; row <= toIndex; row++){
				LogData logData = logDataCollector.getLogData()[row];
				JSONObject jsonLogData = generateJSONFromLogDataFull(logData);
				jsonLogDatas.put(jsonLogData);
			}
			return jsonReturn.toString();
		}
		
		public String generateJSONFromDataStore(int indexDataStore, int fromIndex, int toIndex) throws JSONException {
			StringBuilder sb = new StringBuilder();
			LOGGER.debug("DataStore: {} LogData range: {} to {}", indexDataStore, fromIndex, toIndex);
			JXTable jxTable = jxTableProvider.getJXTable(indexDataStore);
			LogDataCollector logDataCollector = (LogDataCollector) jxTable.getModel();
			int columnCount = jxTable.getColumnCount();
			for(int columnIndex = 0; columnIndex < columnCount; columnIndex++){
				TableColumn column = jxTable.getColumn(columnIndex);
				String columnTitle = column.getHeaderValue().toString();
				LOGGER.debug("column: {} ({})", columnTitle, column);
				LOGGER.debug("   column id: {}", column.getIdentifier().toString());
			}
			int rowCount = jxTable.getVisibleRowCount();
			for(int rowIndex = 0; rowIndex < 3; rowIndex++){
				LOGGER.debug("row: {} ({})", jxTable.getModel(), logDataCollector.getLogData()[rowIndex]);
				for(int columnIndex = 0; columnIndex < columnCount; columnIndex++){
					Object value = jxTable.getValueAt(rowIndex, columnIndex);
					String columnTitle = jxTable.getColumn(columnIndex).getHeaderValue().toString();
					LOGGER.debug("value: {} ({}) ({})", value, value == null ? "null" : value.getClass(), columnTitle);
				}
			}
			
			
			HashSet<String> hashsetThreads = new HashSet<>();
			HashSet<String> hashsetClasses = new HashSet<>();
			JSONObject jsonReturn = new JSONObject();
			JSONArray jsonLogDatas = new JSONArray();
			JSONArray jsonThreads = new JSONArray();
			JSONArray jsonClasses = new JSONArray();
			JSONObject jsonRanges = new JSONObject();
			jsonReturn.put("classes", jsonClasses);
			jsonReturn.put("threads", jsonThreads);
			jsonReturn.put("ranges", jsonRanges);
			jsonReturn.put("logDatas", jsonLogDatas);
			long minTime = Long.MAX_VALUE;
			long maxTime = 0L;
			for(int row = fromIndex; row <= toIndex; row++){
				LogData logData = logDataCollector.getLogData()[row];
				long time = logData.getDate().getTime();
				minTime = time < minTime ? time : minTime;
				maxTime = time > maxTime ? time : maxTime;
				String thread = logData.getThread();
				hashsetThreads.add(thread);
				String clazz = logData.getClazz();
				hashsetClasses.add(clazz);
				
				JSONObject jsonLogData = generateJSONFromLogData(logData);
				jsonLogDatas.put(jsonLogData);
			}
			for (String clazz : hashsetClasses) {
				jsonClasses.put(clazz);
			}
			for (String thread : hashsetThreads) {
				jsonThreads.put(thread);
			}
			JSONObject jsonRangeTime = new JSONObject();
			jsonRangeTime.put("min", minTime);
			jsonRangeTime.put("max", maxTime);
			jsonRanges.put("time",jsonRangeTime);
			sb.append(jsonReturn.toString());
			return sb.toString();
		}

		public JSONObject generateJSONFromLogData(LogData logData) throws JSONException {
			JSONObject jsonLogData = new JSONObject();
			jsonLogData.put("file", logData.getFile());
			jsonLogData.put("time", logData.getDate().getTime());
			jsonLogData.put("thread", logData.getThread());
			jsonLogData.put("clazz", logData.getClazz());
			jsonLogData.put("numLevel", translateLevelToNumber(logData.getLevel()));
			MarkerColors markerColors = logData.getMarkerColors();
			Color foreground;
			Color background;
			if(markerColors != null){
				foreground = markerColors.getForeground();
				background = markerColors.getBackground();
			} else {
				foreground = Color.darkGray;
				background = Color.LIGHT_GRAY;
			}
			jsonLogData.put("fg", generateJSONFromColor(foreground));
			jsonLogData.put("bg", generateJSONFromColor(background));
			return jsonLogData;
		}
		
		private int translateLevelToNumber(Level level){
			int number = 0;
			number = level.intValue();
			return number;
		}

		public JSONObject generateJSONFromLogDataFull(LogData logData) throws JSONException {
			JSONObject jsonLogData = generateJSONFromLogData(logData);
			jsonLogData.put("level", logData.getLevel());
			jsonLogData.put("properties", logData.getProperties());
			jsonLogData.put("message", logData.getMessage());
			jsonLogData.put("method", logData.getMethod());
			jsonLogData.put("line", logData.getLine());
			jsonLogData.put("note", logData.getNote());
			return jsonLogData;
		}
		
		private JSONObject generateJSONFromColor(Color color) throws JSONException{
			JSONObject jsonColor = new JSONObject();
			jsonColor.put("r", color.getRed());
			jsonColor.put("g", color.getGreen());
			jsonColor.put("b", color.getBlue());
			return jsonColor;
		}

		private Object jsonString(String string) {
			String escaped = string.replaceAll("\"", "\\\"");
			escaped = escaped.replaceAll("\n", "\\n");
			return escaped;
		}

    }
	
    static class HttpResourceHandler implements HttpAsyncRequestHandler<HttpRequest> {

        public HttpResourceHandler() {
            super();
        }

        public HttpAsyncRequestConsumer<HttpRequest> processRequest(
                final HttpRequest request,
                final HttpContext context) {
            // Buffer request content in memory for simplicity
            return new BasicAsyncRequestConsumer();
        }

        public void handle(
                final HttpRequest request,
                final HttpAsyncExchange httpexchange,
                final HttpContext context) throws HttpException, IOException {
            final HttpResponse response = httpexchange.getResponse();
            handleInternal(request, response, context);
            httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
        }

        private void handleInternal(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            final String target = request.getRequestLine().getUri();
            String targetDecoded = URLDecoder.decode(target, "UTF-8");
            if(targetDecoded.isEmpty() || targetDecoded.equals("/")){
            	targetDecoded = "index.html";
            }
			final File file = new File("web", targetDecoded);
            String resourcePath = file.getPath();
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (resourceAsStream == null) {

                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                final NStringEntity entity = new NStringEntity(
                        "<html><body><h1>File " + file.getPath() +
                        " not found</h1></body></html>",
                        ContentType.create("text/html", "UTF-8"));
                response.setEntity(entity);
                LOGGER.warn("Resource '{}' not found", resourcePath);

            } else {

                final HttpCoreContext coreContext = HttpCoreContext.adapt(context);
                final HttpConnection conn = coreContext.getConnection(HttpConnection.class);
                response.setStatusCode(HttpStatus.SC_OK);
                ContentType contentType = ContentType.create("text/html");
                if(resourcePath.toLowerCase().endsWith(".js")){
                	contentType = ContentType.create("text/javascript");
                }
				final InputStreamEntity body = new InputStreamEntity(resourceAsStream, contentType);
                response.setEntity(body);
                System.out.println(conn + ": serving file " + file.getPath());
            }
        }

    }


}
