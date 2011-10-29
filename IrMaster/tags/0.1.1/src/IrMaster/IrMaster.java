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

import IrpMaster.IrpUtils;
import org.harctoolbox.harcutils;

/**
 * This class decodes command line parameters and fires up the GUI.
 */
public class IrMaster {

    private static void usage(int exitstatus) {
        System.err.println("Usage:");
        System.err.println(helptext);
        System.exit(exitstatus);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }
    private static final String helptext =
            "\tIrMaster [-v|--verbose] [-d|--debug debugcode] [-p|--properties propertyfile] [--version|--help]";

    /**
     * IrMaster [-v] [-d debugcode] [-p propertyfile] [--version|--help|-h]"
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        int debug = 0;
        int arg_i = 0;
        boolean verbose = false;
        String propsfilename = null;

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-h") || args[arg_i].equals("--help")) {
                    usage(harcutils.exit_success);
                }
                if (args[arg_i].equals("--version")) {
                    System.out.println(IrMasterUtils.version_string);
                    System.out.println(IrpUtils.version_string);
                    System.out.println(harcutils.version_string);            
                    System.out.println("JVM: "+ System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
                    System.out.println();
                    System.out.println(IrMasterUtils.license_string);
                    System.exit(harcutils.exit_success);
                } else if (args[arg_i].equals("-d") || args[arg_i].equals("--debug")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-p") || args[arg_i].equals("--properties") ) {
                    arg_i++;
                    propsfilename = args[arg_i++];
                } else if (args[arg_i].equals("-v") || args[arg_i].equals("--verbose")) {
                    arg_i++;
                    verbose = true;
                } else {
                    usage();
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            if (debug != 0)
                System.err.println("ArrayIndexOutOfBoundsException");

            usage();
        } catch (NumberFormatException e) {
            if (debug != 0)
                System.err.println("NumberFormatException");

            usage();
        }

        Props.initialize(propsfilename);
        UserPrefs.get_instance().set_propsfilename(propsfilename);

        guiExecute(verbose, debug);
    }

    private static void guiExecute(final boolean verbose, final int debug) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                new GuiMain(verbose, debug).setVisible(true);
            }
        });
    }
}
