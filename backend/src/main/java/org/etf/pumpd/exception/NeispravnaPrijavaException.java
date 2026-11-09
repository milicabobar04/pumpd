package org.etf.pumpd.exception;

/** Pogrešan email ili lozinka pri prijavi - 401 Unauthorized. */
public class NeispravnaPrijavaException extends RuntimeException {

	public NeispravnaPrijavaException() {
		super("Pogrešan email ili lozinka.");
	}
}
