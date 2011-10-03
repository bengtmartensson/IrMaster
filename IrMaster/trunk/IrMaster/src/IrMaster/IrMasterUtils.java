/*
Copyright (C) 2011 Bengt Martensson.

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
package IrMaster;

/**
 * This class ...
 *
 * @author Bengt Martensson
 */
public class IrMasterUtils {
    
    private static String default_configfile = "IrMaster.ini";
    public final static String license_string = "Copyright (C) 2011 Bengt Martensson.\n\n"
            + "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.";
    public final static int main_version = 0;
    public final static int sub_version = 0;
    public final static int subminor_version = 0;
    public final static String version_suffix = "";
    public final static String version_string = "IrMaster version " + main_version
            + "." + sub_version + "." + subminor_version + version_suffix;
    
    public final static String homepageUrl = "www.harctoolbox.org";
    
    public final static long invalid = -1;
    public final static long all = -2;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
}
