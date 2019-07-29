/*
 * Copyright 2012 Krzysztof Otrebski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.otros.logview.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.otros.logview.BufferingLogDataCollectorProxy;
import pl.otros.logview.api.OtrosApplication;
import pl.otros.logview.api.gui.Icons;
import pl.otros.logview.api.gui.LogViewPanelWrapper;
import pl.otros.logview.api.gui.OtrosAction;
import pl.otros.logview.api.model.LogDataCollector;
import pl.otros.web.JXTablesProvider;
import pl.otros.web.NHttpLogDataServer;

public class StartWebServer extends OtrosAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartWebServer.class);
	private BufferingLogDataCollectorProxy logDataCollector;

	private JPanel httpServerPanel;
	private static NHttpLogDataServer httpServer;
	private final JTabbedPane tabbedPane;

	public StartWebServer(OtrosApplication otrosApplication) {
		super(otrosApplication);
		tabbedPane = getOtrosApplication().getjTabbedPane();
		initAction();
	}

	private void initAction() {
		String availableOp = "Start";
		if (httpServer != null && httpServer.isStarted()) {
			availableOp = "Stop";
		}
		putValue(Action.NAME, availableOp + " web server");
		putValue(Action.SHORT_DESCRIPTION, availableOp + " web server on port.");
		putValue(Action.LONG_DESCRIPTION, availableOp + " web server on port.");
	    putValue(MNEMONIC_KEY, KeyEvent.VK_W);
		putValue(SMALL_ICON, Icons.PLUGIN_PLUS);
	}

	@Override
	protected void actionPerformedHook(ActionEvent arg0) {

		try {
			initHttpServer();
			if (httpServer.isStarted()) {
				closePanel();
				LOGGER.debug("HttpServer stopping");
				httpServer.stop();
				LOGGER.debug("HttpServer stopped");
			} else {
				initPanel();
				LOGGER.debug("HttpServer starting");
				httpServer.start();
				LOGGER.debug("HttpServer started");
			}
			initAction();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void closePanel() {
		getOtrosApplication().closeTab(httpServerPanel);
	}

	public void initPanel() {
		httpServerPanel = new JPanel();
		getOtrosApplication().addClosableTab("Web server", "Web server", Icons.PLUGIN_CONNECT,
				httpServerPanel, true);
	}

	public void initHttpServer() throws Exception {
//		fillLogDataStores();
		if(httpServer == null){
			JXTablesProvider jxTableProvider = new JXTablesProvider() {
				
				@Override
				public JXTable getJXTable(int index) {
					int browsing = 0;
					JTabbedPane tabbedPane = getOtrosApplication().getjTabbedPane();
					int tabCount = tabbedPane.getTabCount();
					for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
						Component component = tabbedPane.getComponentAt(tabIndex);
						LOGGER.debug("Component: {}", component);
						if (component instanceof LogViewPanelWrapper) {
							LogViewPanelWrapper logViewPanelWrapper = (LogViewPanelWrapper) component;
							if(logViewPanelWrapper.getLogViewPanel().getTable().getModel() instanceof LogDataCollector){
								if(browsing == index){
									return logViewPanelWrapper.getLogViewPanel().getTable();
								}
								browsing++;
							}
						}
					}
					throw new IndexOutOfBoundsException("table index: " + index + " > " + browsing);
				}
				
				@Override
				public int getCount() {
					int count = 0;
					JTabbedPane tabbedPane = getOtrosApplication().getjTabbedPane();
					int tabCount = tabbedPane.getTabCount();
					for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
						Component component = tabbedPane.getComponentAt(tabIndex);
						LOGGER.debug("Component: {}", component);
						if (component instanceof LogViewPanelWrapper) {
							LogViewPanelWrapper logViewPanelWrapper = (LogViewPanelWrapper) component;
							if(logViewPanelWrapper.getLogViewPanel().getTable().getModel() instanceof LogDataCollector){
								count++;
							}
						}
					}
					return count;
				}
			};
			httpServer = new NHttpLogDataServer("localhost", 8082, jxTableProvider);
		}
	}

}
