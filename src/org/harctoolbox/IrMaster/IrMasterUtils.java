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

/**
 * This class contains some utility definitions.
 *
 */
public class IrMasterUtils {
    public final static String licenseString = "Copyright (C) 2011, 2012 Bengt Martensson.\n\n"
            + "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.";
    public final static String thirdPartyString = "Makehex was written by John S. Fine (see LICENSE_makehex.txt), and has been translated to Java by Bengt Martensson. "
            + "ExchangeIR was written by Graham Dixon and published under GPL3 license. Its Analyze-function has been translated to Java by Bengt Martensson. "
            + "DecodeIR was originally written by John S. Fine, with later contributions from others. It is free software with undetermined license. "
            + "IrpMaster depends on the runtime functions of ANTLR3, which is free software with BSD license.";
    public final static String appName = "IrMaster";
    public final static int mainVersion = 0;
    public final static int subVersion = 1;
    public final static int subminorVersion = 2;
    public final static String versionSuffix = "b";
    public final static String versionString = "IrMaster version " + mainVersion
            + "." + subVersion + "." + subminorVersion + versionSuffix;
    public final static String homepageUrl = "http://www.harctoolbox.org";
    public final static String currentVersionUrl = homepageUrl + "/downloads/irmaster.version";
    public final static long invalid = -1;
}
