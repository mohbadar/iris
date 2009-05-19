/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008-2009  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.client.toast;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import us.mn.state.dot.sched.ActionJob;
import us.mn.state.dot.sched.ChangeJob;
import us.mn.state.dot.sched.FocusJob;
import us.mn.state.dot.sonar.Checker;
import us.mn.state.dot.sonar.client.ProxyListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.Alarm;
import us.mn.state.dot.tms.Cabinet;
import us.mn.state.dot.tms.CabinetStyle;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CommLink;
import us.mn.state.dot.tms.Controller;
import us.mn.state.dot.tms.ControllerIO;
import us.mn.state.dot.tms.Detector;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.LCSIndication;
import us.mn.state.dot.tms.RampMeter;
import us.mn.state.dot.tms.WarningSign;
import us.mn.state.dot.tms.client.SonarState;
import us.mn.state.dot.tms.client.TmsConnection;
import us.mn.state.dot.tms.client.proxy.ProxyListModel;
import us.mn.state.dot.tms.utils.I18NMessages;

/**
 * ControllerForm is a Swing dialog for editing Controller records
 *
 * @author Douglas Lau
 */
public class ControllerForm extends SonarObjectForm<Controller> {

	/** Table row height */
	static protected final int ROW_HEIGHT = 24;

	/** Frame title */
	static protected final String TITLE = "Controller: ";

	/** Comm link combo box */
	protected final JComboBox comm_link = new JComboBox();

	/** Model for drop address spinner */
	protected DropNumberModel drop_model;

	/** Drop address spinner */
	protected final JSpinner drop_id = new JSpinner();

	/** Controller notes text */
	protected final JTextArea notes = new JTextArea(3, 24);

	/** Active checkbox */
	protected final JCheckBox active = new JCheckBox();

	/** Location panel */
	protected LocationPanel location;

	/** Mile point text field */
	protected final JTextField mile = new JTextField(10);

	/** Cabinet style combo box */
	protected final JComboBox cab_style = new JComboBox();

	/** Cabinet for controller */
	protected final Cabinet cabinet;

	/** Cabinet cache */
	protected final TypeCache<Cabinet> cabinets;

	/** Cabinet listener */
	protected CabinetListener cab_listener;

	/** Controller IO model */
	protected ControllerIOModel io_model;

	/** Status */
	protected final JLabel status = new JLabel();

	/** Error detail */
	protected final JLabel error = new JLabel();

	/** Firmware version */
	protected final JLabel version = new JLabel();

	/** Clear error status button */
	protected final JButton clearErrorBtn = new JButton("Clear Error");

	/** Download button */
	protected final JButton download =
		new JButton(I18NMessages.get("ControllerForm.DownloadButton"));

	/** Reset button */
	protected final JButton reset =
		new JButton(I18NMessages.get("ControllerForm.ResetButton"));

	/** Comm Link list model */
	protected final ProxyListModel<CommLink> link_model;

	/** Cabinet style list model */
	protected final ProxyListModel<CabinetStyle> sty_model;

	/** Create a new controller form */
	public ControllerForm(TmsConnection tc, Controller c) {
		super(TITLE, tc, c);
		link_model = state.getCommLinkModel();
		cabinets = state.getCabinets();
		cabinet = proxy.getCabinet();
		cab_listener = new CabinetListener();
		sty_model = state.getCabinetStyleModel();
	}

	/** Get the SONAR type cache */
	protected TypeCache<Controller> getTypeCache() {
		return state.getControllers();
	}

	/** Get the ControllerIO array */
	protected ControllerIO[] getControllerIO() {
		ControllerIO[] io = new ControllerIO[Controller.ALL_PINS];
		TypeCache<Alarm> alarms = state.getAlarms();
		alarms.findObject(new ControllerIOFinder<Alarm>(io));
		TypeCache<Camera> cams = state.getCameras();
		cams.findObject(new ControllerIOFinder<Camera>(io));
		TypeCache<Detector> dets = state.getDetectors();
		dets.findObject(new ControllerIOFinder<Detector>(io));
		TypeCache<DMS> dmss = state.getDMSs();
		dmss.findObject(new ControllerIOFinder<DMS>(io));
		TypeCache<LCSIndication> lcsi = state.getLCSIndications();
		lcsi.findObject(new ControllerIOFinder<LCSIndication>(io));
		TypeCache<WarningSign> w_signs = state.getWarningSigns();
		w_signs.findObject(new ControllerIOFinder<WarningSign>(io));
		TypeCache<RampMeter> meters = state.getRampMeters();
		meters.findObject(new ControllerIOFinder<RampMeter>(io));
		return io;
	}

	/** A controller IO finder helps locate IO for a controller */
	protected class ControllerIOFinder<T extends ControllerIO>
		implements Checker<T>
	{
		protected final ControllerIO[] io;
		protected ControllerIOFinder(ControllerIO[] _io) {
			io = _io;
		}
		public boolean check(ControllerIO cio) {
			if(cio.getController() == proxy) {
				int pin = cio.getPin();
				if(pin > 0 && pin < io.length)
					io[pin] = cio;
			}
			return false;
		}
	}

	/** Initialize the widgets on the form */
	protected void initialize() {
		super.initialize();
		io_model = new ControllerIOModel(proxy, state);
		cabinets.addProxyListener(cab_listener);
		comm_link.setModel(new WrapperComboBoxModel(link_model, false));
		cab_style.setModel(new WrapperComboBoxModel(sty_model, true));
		JTabbedPane tab = new JTabbedPane();
		tab.add("Setup", createSetupPanel());
		tab.add("Cabinet", createCabinetPanel());
		tab.add("I/O", createIOPanel());
		tab.add("Status", createStatusPanel());
		add(tab);
		updateAttribute(null);
		setBackground(Color.LIGHT_GRAY);
	}

	/** Dispose of the form */
	protected void dispose() {
		cabinets.removeProxyListener(cab_listener);
		location.dispose();
		super.dispose();
	}

	/** Create the controller setup panel */
	protected JPanel createSetupPanel() {
		new ActionJob(this, comm_link) {
			public void perform() {
				proxy.setCommLink(
					(CommLink)comm_link.getSelectedItem());
			}
		};
		new ChangeJob(this, drop_id) {
			public void perform() {
				Number n = (Number)drop_id.getValue();
				proxy.setDrop(n.shortValue());
			}
		};
		new ActionJob(this, active) {
			public void perform() {
				proxy.setActive(active.isSelected());
			}
		};
		FormPanel panel = new FormPanel(admin);
		panel.add("Comm Link", comm_link);
		panel.finishRow();
		panel.add("Drop", drop_id);
		panel.finishRow();
		panel.addRow("Notes", notes);
		new FocusJob(notes) {
			public void perform() {
				if(wasLost())
					proxy.setNotes(notes.getText());
			}
		};
		panel.add("Active", active);
		// Add a third column to the grid bag so the drop spinner
		// does not extend across the whole form
		panel.addRow(new javax.swing.JLabel());
		active.setEnabled(connection.isAdmin() ||
			connection.isActivate());
		return panel;
	}

	/** Create the cabinet panel */
	protected JPanel createCabinetPanel() {
		new FocusJob(mile) {
			public void perform() {
				if(wasLost()) {
					String ms = mile.getText();
					try {
						Float m = Float.valueOf(ms);
						cabinet.setMile(m);
					}
					catch(NumberFormatException e) {
						cabinet.setMile(null);
					}
				}
			}
		};
		new ActionJob(this, cab_style) {
			public void perform() {
				cabinet.setStyle((CabinetStyle)
					cab_style.getSelectedItem());
			}
		};
		location = new LocationPanel(admin, cabinet.getGeoLoc(), state);
		location.initialize();
		location.add("Milepoint", mile);
		location.finishRow();
		location.add("Style", cab_style);
		location.finishRow();
		return location;
	}

	/** Listener for cabinet proxy changes */
	protected class CabinetListener implements ProxyListener<Cabinet> {
		public void proxyAdded(Cabinet p) {}
		public void enumerationComplete() { }
		public void proxyRemoved(Cabinet p) {}
		public void proxyChanged(Cabinet p, final String a) {
			if(p == cabinet)
				updateAttribute(a);
		}
	}

	/** Create the I/O panel */
	protected JPanel createIOPanel() {
		ZTable table = new ZTable();
		table.setAutoCreateColumnsFromModel(false);
		table.setModel(io_model);
		table.setColumnModel(io_model.createColumnModel());
		table.setRowHeight(ROW_HEIGHT);
		table.setVisibleRowCount(8);
		FormPanel panel = new FormPanel(true);
		panel.addRow(table);
		return panel;
	}

	/** Create the status panel */
	protected JPanel createStatusPanel() {
		new ActionJob(this, download) {
			public void perform() {
				proxy.setDownload(false);
			}
		};
		new ActionJob(this, reset) {
			public void perform() {
				proxy.setDownload(true);
			}
		};
		new ActionJob(this, clearErrorBtn) {
			public void perform() {
				proxy.setError("");
			}
		};
		JPanel buttonPnl = new JPanel();
		buttonPnl.add(clearErrorBtn);
		buttonPnl.add(download);
		buttonPnl.add(reset);
		FormPanel panel = new FormPanel(admin);
		panel.addRow("Status:", status);
		panel.addRow("Error Detail:", error);
		panel.addRow("Version:", version);
		panel.addRow(buttonPnl);
		return panel;
	}

	/** Update one attribute on the form */
	protected void updateAttribute(String a) {
		if(a == null || a.equals("commLink")) {
			comm_link.setSelectedItem(proxy.getCommLink());
			drop_model = new DropNumberModel(
				proxy.getCommLink(), getTypeCache(),
				proxy.getDrop());
			drop_id.setModel(drop_model);
		}
		if(a == null || a.equals("drop"))
			drop_id.setValue(proxy.getDrop());
		if(a == null || a.equals("notes"))
			notes.setText(proxy.getNotes());
		if(a == null || a.equals("active"))
			active.setSelected(proxy.getActive());
		if(a == null || a.equals("status"))
			status.setText(proxy.getStatus());
		if(a == null || a.equals("error"))
			error.setText(proxy.getError());
		if(a == null || a.equals("version"))
			version.setText(proxy.getVersion());
		if(a == null || a.equals("mile")) {
			Float m = cabinet.getMile();
			if(m == null)
				mile.setText("");
			else
				mile.setText(m.toString());
		}
		if(a == null || a.equals("style"))
			cab_style.setSelectedItem(cabinet.getStyle());
		// FIXME: ControllerIOModel should manage itself
		io_model.setCio(getControllerIO());
	}
}
