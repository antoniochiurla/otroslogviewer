package pl.otros.logview.api.gui;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import pl.otros.logview.api.OtrosApplication;
import pl.otros.web.NHttpLogDataServer;

public class WebServerPanel extends JPanel {
	private static NHttpLogDataServer httpServer;
	private final JTabbedPane tabbedPane;
	private OtrosApplication otrosApplication;
	
	public WebServerPanel(OtrosApplication otrosApplication) {
		this.otrosApplication = otrosApplication;
		tabbedPane = otrosApplication.getjTabbedPane();
	}

}
