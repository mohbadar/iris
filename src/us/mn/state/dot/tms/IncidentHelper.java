/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2019  Minnesota Department of Transportation
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
package us.mn.state.dot.tms;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import us.mn.state.dot.sched.TimeSteward;

/**
 * Helper class for Incident.
 *
 * @author Douglas Lau
 */
public class IncidentHelper extends BaseHelper {

	/** Don't instantiate */
	private IncidentHelper() {
		assert false;
	}

	/** Lookup the Incident with the specified name */
	static public Incident lookup(String name) {
		return (Incident) namespace.lookupObject(Incident.SONAR_TYPE,
			name);
	}

	/** Get an incident iterator */
	static public Iterator<Incident> iterator() {
		return new IteratorWrapper<Incident>(namespace.iterator(
			Incident.SONAR_TYPE));
	}

	/** Lookup an incident by the original name */
	static public Incident lookupOriginal(String name) {
		if (null == name)
			return null;
		Incident inc = lookup(name);
		if (inc != null)
			return inc;
		Iterator<Incident> it = iterator();
		while (it.hasNext()) {
			inc = it.next();
			if (name.equals(inc.getReplaces()))
				return inc;
		}
		return null;
	}

	/** Lookup the camera for an incident */
	static public Camera getCamera(Incident inc) {
		return (inc != null) ? inc.getCamera() : null;
	}

	/** Create a unique incident name */
	static public String createUniqueName() {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String name = f.format(TimeSteward.currentTimeMillis());
		return name.substring(0, 16);
	}

	/** Get the severity of an incident */
	static public IncSeverity getSeverity(Incident inc) {
		return IncImpact.getImpact(inc).severity();
	}
}
