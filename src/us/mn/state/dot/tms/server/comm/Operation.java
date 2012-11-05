/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2002-2012  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm;

import java.io.IOException;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.server.IDebugLog;

/**
 * An operation to be performed on a field controller.  Each message
 * poller maintains a prioritized queue of all outstanding operations. When an
 * operation gets to the head of the queue, the next phase is performed by
 * calling the poll method.
 *
 * @author Douglas Lau
 */
abstract public class Operation<T extends ControllerProperty> {

	/** Operation error log */
	static protected final IDebugLog OP_LOG = new IDebugLog("operation");

	/** Systemwide count of operations */
	static private int n_operations = 0;

	/** Increment the count of operations */
	static private int incrementCount() {
		synchronized(OP_LOG) {
			n_operations++;
			return n_operations;
		}
	}

	/** Decrement the count of operations */
	static private int decrementCount() {
		synchronized(OP_LOG) {
			n_operations--;
			return n_operations;
		}
	}

	/** Write a message to the operation log */
	protected void log(String msg) {
		if(OP_LOG.isOpen())
			OP_LOG.log(getOpName() + " " + msg);
	}

	/** Write a message to the operation log */
	protected void log(String msg, int n_ops) {
		if(OP_LOG.isOpen())
			log(msg + ": " + n_ops);
	}

	/** Priority of the operation */
	private PriorityLevel priority;

	/** Get the priority of the operation.
	 * @return Priority of the operation (@see PriorityLevel) */
	public PriorityLevel getPriority() {
		return priority;
	}

	/** Set the priority of the operation */
	public void setPriority(PriorityLevel p) {
		if(p.ordinal() < priority.ordinal())
			priority = p;
	}

	/** Current phase of the operation */
	private Phase<T> phase;

	/** Create a new I/O operation */
	public Operation(PriorityLevel prio) {
		priority = prio;
		log("created");
	}

	/** Create the first phase of the operation.  This method cannot be
	 * called in the Operation constructor, because the object may not
	 * have been fully constructed yet (subclass initialization). */
	abstract protected Phase<T> phaseOne();

	/** Operation equality test */
	public boolean equals(Object o) {
		return this == o;
	}

	/** Get a string description of the operation */
	public String toString() {
		String name;
		Phase<T> p = phase;
		if(p != null)
			name = p.getClass().getName();
		else
			name = getClass().getName();
		int i = name.lastIndexOf('.');
		if(i >= 0)
			return name.substring(i + 1);
		else
			return name;
	}

	/** Get the operation name */
	protected String getOpName() {
		String name = getClass().getName();
		int i = name.lastIndexOf('.');
		if(i >= 0)
			return name.substring(i + 1);
		else
			return name;
	}

	/** Success or failure of operation */
	private boolean success = true;

	/** Check if the operation succeeded */
	public boolean isSuccess() {
		return success;
	}

	/** Set the success flag */
	public void setSuccess(boolean s) {
		success = s;
	}

	/** Set the operation to failed */
	public synchronized void setFailed() {
		setSuccess(false);
		phase = null;
	}

	/** Set the operation to succeeded */
	public synchronized void setSucceeded() {
		setSuccess(true);
		phase = null;
	}

	/** Begin the operation.  The operation begins when it is queued for
	 * processing. */
	public boolean begin() {
		phase = phaseOne();
		log("begin", incrementCount());
		return true;
	}

	/** Cleanup the operation.  The operation gets cleaned up after
	 * processing is complete and it is removed from the queue. */
	public void cleanup() {
		log("cleanup", decrementCount());
	}

	/** Handle a communication error */
	public void handleCommError(EventType et, String msg) {
		setFailed();
	}

	/** Check if the operation is done */
	public boolean isDone() {
		return phase == null;
	}

	/** 
	 * Perform a poll with an addressed message. Called by 
	 * MessagePoller.doPoll(). Processing stops when phase is
	 * assigned null.
	 * @see MessagePoller.performOperations
	 */
	public void poll(CommMessage<T> mess) throws IOException,
		DeviceContentionException
	{
		final Phase<T> p = phase;
		if(p != null) {
			Phase<T> np = p.poll(mess);
			// Need to synchronize against setFailed / setSucceeded
			synchronized(this) {
				if(!isDone())
					phase = np;
			}
		}
	}

	/** Base class for operation phases */
	abstract protected class Phase<T extends ControllerProperty> {

		/** Perform a poll.
		 * @return The next phase of the operation */
		abstract protected Phase<T> poll(CommMessage<T> mess)
			throws IOException, DeviceContentionException;
	}

	/** Get a description of the operation */
	public String getOperationDescription() {
		return getOpName();
	}
}
