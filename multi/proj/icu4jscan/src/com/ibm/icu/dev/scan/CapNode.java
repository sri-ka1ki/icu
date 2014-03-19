/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

package com.ibm.icu.dev.scan;

import java.io.PrintWriter;

public interface CapNode extends Comparable {

	void write(PrintWriter pw, int i);

	String getName();

}
