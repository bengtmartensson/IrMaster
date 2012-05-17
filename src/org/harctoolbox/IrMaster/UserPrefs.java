/*
Copyright (C) 2009-2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.IrMaster;

/**
 * This class handles some user preferences that are not properties.
 * This class is not normally explicitly instantiated, but has a single static instance.
 */
public class UserPrefs {
    private boolean verbose = false;
    private int debug = 0;

    private String propsfilename;
    
    private static UserPrefs instance = new UserPrefs();
    
    /**
     * Returns instance
     * @return instance
     */
    public static UserPrefs getInstance() {
        return instance;
    }
    
    /** Returns property file name */
    public String getPropsfilename() {
       return propsfilename;
    }
    
    /** Returns debug value */
    public int getDebug() {
        return debug;
    }
    
    /** Returns verbosity setting */
    public boolean getVerbose() {
        return verbose;
    }
    
    /** Sets the name of the property file */
    public void setPropsfilename(String propsfilename) {
	this.propsfilename = propsfilename;
    }

    /** Sets the value of debug */
    public void setDebug(int debug) {
	this.debug = debug;
    }

    /** Sets verbosity */
    public void setVerbose(boolean verbose) {
	this.verbose = verbose;
    }
}
