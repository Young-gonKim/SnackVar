package com.opaleye.snackvar;

/**
 * Title : NoContigException
 * An exception thrown when there is no overlap between forward trace and reverse trace 
 * @author Young-gon Kim
 *2018.8
 */
public class NoContigException extends Exception {
	public NoContigException() {
		super("Alignment Failed : There is no overlap between forward trace and reverse trace!\n\n"
				+ "* Common causes\n"
				+ "1) Wrong match of forward - reverse traces\n"
				+ "2) Reversed assignment of forward-reverse traces\n"
				+ "    Check for the errors in file naming (R <-> F)\n"
				+ "3) Wrong reference file");
	}
}
