/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008-2019  Minnesota Department of Transportation
 * Copyright (C) 2010  AHMCT, University of California
 * Copyright (C) 2017-2018  Iteris Inc.
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
package us.mn.state.dot.tms.client.dms;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.RasterGraphic;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.DeviceManager;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyJList;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A DMS manager is a container for SONAR DMS objects.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class DMSManager extends DeviceManager<DMS> {

	/** Create a proxy descriptor */
	static public ProxyDescriptor<DMS> descriptor(final Session s) {
		return new ProxyDescriptor<DMS>(
			s.getSonarState().getDmsCache().getDMSs(), true
		) {
			@Override
			public DMSProperties createPropertiesForm(
				DMS dms)
			{
				return new DMSProperties(s, dms);
			}
			@Override
			public DMSForm makeTableForm() {
				return new DMSForm(s);
			}
		};
	}

	/** Mapping of DMS to page one rasters */
	private final HashMap<DMS, RasterGraphic> rasters =
		new HashMap<DMS, RasterGraphic>();

	/** Action to blank the selected DMS */
	private BlankDmsAction blankAction;

	/** Action to test DMS */
	private TestDmsAction test_dms_action;

	/** Action to query status of DMS */
	private QueryStatusDmsAction qstatus_dms_action;

	/** Set the blank DMS action */
	public void setBlankAction(BlankDmsAction a) {
		blankAction = a;
	}

	/** Set the test DMS action */
	public void setTestAction(TestDmsAction a) {
		test_dms_action = a;
	}

	/** Set the query status DMS action */
	public void setQueryStatusAction(QueryStatusDmsAction a) {
		qstatus_dms_action = a;
	}

	/** Create a new DMS manager */
	public DMSManager(Session s, GeoLocManager lm) {
		super(s, lm, descriptor(s), 12, ItemStyle.DEPLOYED);
		getSelectionModel().setAllowMultiple(true);
	}

	/** Create a DMS map tab */
	@Override
	public DMSTab createTab() {
		return new DMSTab(session, this);
	}

	/** Create a theme for DMSs */
	@Override
	protected ProxyTheme<DMS> createTheme() {
		// NOTE: the ordering of themes controls which color is used
		//       to render the sign icon on the map
		ProxyTheme<DMS> theme = new ProxyTheme<DMS>(this,
			new DmsMarker());
		theme.addStyle(ItemStyle.AVAILABLE, ProxyTheme.COLOR_AVAILABLE);
		theme.addStyle(ItemStyle.DEPLOYED, ProxyTheme.COLOR_DEPLOYED);
		theme.addStyle(ItemStyle.SCHEDULED, ProxyTheme.COLOR_SCHEDULED);
		theme.addStyle(ItemStyle.MAINTENANCE,
			ProxyTheme.COLOR_UNAVAILABLE);
		theme.addStyle(ItemStyle.FAILED, ProxyTheme.COLOR_FAILED);
		theme.addStyle(ItemStyle.ALL);
		return theme;
	}

	/** Create a list cell renderer */
	@Override
	public ListCellRenderer<DMS> createCellRenderer() {
		return new DmsCellRenderer(getCellSize()) {
			@Override protected RasterGraphic getPageOne(DMS dms) {
				return rasters.get(dms);
			}
		};
	}

	/** Add a proxy to the manager */
	@Override
	protected void proxyAddedSwing(DMS dms) {
		updateRaster(dms);
		super.proxyAddedSwing(dms);
	}

	/** Update one DMS raster */
	private void updateRaster(DMS dms) {
		rasters.put(dms, DMSHelper.getPageOne(dms));
	}

	/** Enumeration complete */
	@Override
	protected void enumerationCompleteSwing(Collection<DMS> proxies) {
		super.enumerationCompleteSwing(proxies);
		for (DMS dms : proxies)
			updateRaster(dms);
	}

	/** Check if an attribute change is interesting */
	@Override
	protected boolean checkAttributeChange(String a) {
		return super.checkAttributeChange(a) || "msgCurrent".equals(a);
	}

	/** Called when a proxy attribute has changed */
	@Override
	protected void proxyChangedSwing(DMS dms, String a) {
		if ("msgCurrent".equals(a))
			updateRaster(dms);
		super.proxyChangedSwing(dms, a);
	}

	/** Check if a DMS style is visible */
	@Override
	protected boolean isStyleVisible(DMS dms) {
		return !DMSHelper.isHidden(dms);
	}

	/** Create a proxy JList */
	@Override
	public ProxyJList<DMS> createList() {
		ProxyJList<DMS> list = super.createList();
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		list.setVisibleRowCount(0);
		return list;
	}

	/** Fill single selection popup */
	@Override
	protected void fillPopupSingle(JPopupMenu p, DMS dms) {
		if (blankAction != null) {
			p.add(blankAction);
			p.addSeparator();
		}
	}

	/** Create a popup menu for multiple objects */
	@Override
	protected JPopupMenu createPopupMulti(int n_selected) {
		JPopupMenu p = new JPopupMenu();
		p.add(new JLabel(I18N.get("dms.title") + ": " +
			n_selected));
		p.addSeparator();
		if (blankAction != null)
			p.add(blankAction);
		if(test_dms_action != null)
			p.add(test_dms_action);
		if(qstatus_dms_action != null)
			p.add(qstatus_dms_action);
		return p;
	}

	/** Find the map geo location for a proxy */
	@Override
	protected GeoLoc getGeoLoc(DMS proxy) {
		return proxy.getGeoLoc();
	}
}
