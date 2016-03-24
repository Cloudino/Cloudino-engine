/*
 * Copyright 2004 WIT-Software, Lda. 
 * - web: http://www.wit-software.com 
 * - email: info@wit-software.com
 *
 * All rights reserved. Relased under terms of the 
 * Creative Commons' Attribution-NonCommercial-ShareAlike license.
 */
package io.cloudino.server.ssl;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Nuno Santos
 */
public class SSLChannelManager {
	private final static Logger log = Logger.getLogger("handlers");
		
	private final Set readListeners = new HashSet();
	private final Set writeListeners = new HashSet();

	/**
	 * Called by the SelectorThread to give the SecureChannels
	 * a chance to fire the events to its listeners
	 */
	public void fireEvents() {
		while (!readListeners.isEmpty() || !writeListeners.isEmpty()) {
			SSLChannel[] sc;
			if (!readListeners.isEmpty()) {
				// Fire read events
				// Make a copy because the handlers might call one of the register
				// methods of this class, thereby changing the set.
				sc = (SSLChannel[])readListeners.toArray(
						new SSLChannel[readListeners.size()]);
				readListeners.clear();
				for (int i = 0; i < sc.length; i++) {
					sc[i].fireReadEvent();
				}
			}
			
			if (!writeListeners.isEmpty()) {
				// Now the write listeners
				sc = (SSLChannel[])writeListeners.toArray(
						new SSLChannel[writeListeners.size()]);
				writeListeners.clear();
				for (int i = 0; i < sc.length; i++) {
					sc[i].fireWriteEvent();
				}
			}
		}
	}
		
	public void registerForRead(SSLChannel l) {
		log.fine("Registering for read");		
		boolean wasNotPresent = readListeners.add(l);
//		assert wasNotPresent : "SecureChannel was already registered";
	}
	
	public void unregisterForRead(SSLChannel l) {
		log.fine("Unregistering for read");
		boolean wasPresent = readListeners.remove(l);
//		assert wasPresent : "SecureChannel was not registered";
	}
		
	public void registerForWrite(SSLChannel l) {
		log.fine("Registering for write");
		boolean wasNotPresent = writeListeners.add(l);
//		assert wasNotPresent : "SecureChannel was already registered";
	}
	
	public void unregisterForWrite(SSLChannel l) {
		log.fine("Unregistering for write");
		boolean wasPresent = writeListeners.remove(l);
//		assert !wasPresent : "SecureChannel was not registered";
	}
}
