/*
Copyright (C) 2011, 2012 Bengt Martensson.

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class contains version and license information and constants.
 */
public class Version {
    /** Verbal description of the license of the current work. */
    public final static String licenseString = "Copyright (C) 2011, 2012, 2013 Bengt Martensson.\n\n" // \u00a9
            + "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.";

    /** Verbal description of licenses of third-party components. */
    public final static String thirdPartyString = "Makehex was written by John S. Fine (see LICENSE_makehex.txt), and has been translated to Java by Bengt Martensson. "
            + "ExchangeIR was written by Graham Dixon and published under GPL3 license. Its Analyze-function has been translated to Java by Bengt Martensson. "
            + "DecodeIR was originally written by John S. Fine, with later contributions from others. It is free software with undetermined license. "
            + "IrpMaster depends on the runtime functions of ANTLR3, which is free software with BSD license. "
            + "IrMaster uses PtPlot for plotting IR sequences; this is free software with UC Berkeley Copyright. "
            + "Icons from the Crystal project are used; these are released under the LGPL license.";
    
    public final static String appName = "IrMaster";
    public final static int mainVersion = 0;
    public final static int subVersion = 3;
    public final static int subminorVersion = 2;
    public final static String versionSuffix = "";
    public final static String versionString = appName + " version " + mainVersion
            + "." + subVersion + "." + subminorVersion + versionSuffix;

    /** Project home page. */
    public final static String homepageUrl = "http://www.harctoolbox.org";

    /** URL containing current official version. */
    public final static String currentVersionUrl = homepageUrl + "/downloads/" + appName + ".version";

    public static void createVersionFile() {
        try {
            PrintStream printStream = new PrintStream(new File(appName + ".version"), IrpUtils.dumbCharsetName);
            printStream.println(versionString);
            printStream.close();
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            // This cannot happen
            assert false;
        }
    }

    public static void main(String[] args) {
        createVersionFile();
    }
}
