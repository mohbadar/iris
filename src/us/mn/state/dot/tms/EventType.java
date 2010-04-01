/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008-2010  Minnesota Department of Transportation
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

/**
 * This enumeration contains events in the event_description table.
 *
 * @author Douglas Lau
 */
public enum EventType {
	ALARM_TRIGGERED(1), ALARM_CLEARED(2),
	DMS_DEPLOYED(91), DMS_CLEARED(92),
	LCS_DEPLOYED(89), LCS_CLEARED(90),
	DET_CHATTER(96), DET_LOCKED_ON(95), DET_NO_HITS(94),
	QUEUE_DRAINED(10), POLL_TIMEOUT_ERROR(11), PARSING_ERROR(12),
	CHECKSUM_ERROR(13), CONTROLLER_ERROR(14),
	COMM_ERROR(8), COMM_FAILED(65), COMM_RESTORED(9),
	INCIDENT_CLEARED(20), INCIDENT_CRASH(21), INCIDENT_STALL(22),
	INCIDENT_HAZARD(23), INCIDENT_ROADWORK(24), INCIDENT_IMPACT(29);

	/** Event type ID */
	public final int id;

	/** Create an event type */
	private EventType(int i) {
		id = i;
	}

	/** Get an event type from the ID */
	static public EventType fromId(int id) {
		for(EventType et: values()) {
			if(et.id == id)
				return et;
		}
		return null;
	}
}
