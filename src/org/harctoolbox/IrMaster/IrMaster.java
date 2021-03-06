/*
Copyright (C) 2011, 2012, 2013 Bengt Martensson.

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

import java.io.FileNotFoundException;
import org.harctoolbox.IrpMaster.IrpMaster;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class decodes command line parameters and fires up the GUI.
 */
public class IrMaster {

    /** Number indicating invalid value. */
    public final static long invalid = -1;
    private final static int defaultPortNumber = 9997;

    private static int portNo = (int) invalid;

    // Just to have the Javadoc API look minimal and pretty :-)
    private IrMaster() {
    }

    private static void usage(int exitstatus) {
        System.err.println("Usage:");
        System.err.println(helptext);
        System.exit(exitstatus);
    }

    private static void usage() {
        usage(IrpUtils.exitUsageError);
    }
    private static final String helptext =
            "\tirmaster [-v|--verbose] [-d|--debug debugcode] [-p|--properties propertyfile] [--version|--help] [-s|--server [portno]]\n"
            + "or\n"
            + "\tirmaster IrpMaster <IrpMaster-options-and-arguments>";

    /**
     * IrMaster [-v] [-d debugcode] [-p propertyfile] [--version|--help|-h]"
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        int debug = 0;
        int arg_i = 0;
        boolean verbose = false;
        String propsfilename = null;
        int userlevel = 99;

        if (args.length > 0 && args[0].equalsIgnoreCase("IrpMaster")) {
            String[] newArgs = new String[args.length-1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            IrpMaster.main(newArgs);
            System.exit(IrpUtils.exitSuccess); // just to be safe
        }
        
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-h") || args[arg_i].equals("--help")) {
                    usage(IrpUtils.exitSuccess);
                } else if (args[arg_i].equals("--version")) {
                    System.out.println(Version.versionString);
                    System.out.println(org.harctoolbox.IrpMaster.Version.versionString);
                    System.out.println(org.harctoolbox.harchardware.Version.versionString);
                    System.out.println("JVM: "+ System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
                    System.out.println("os.name:" + System.getProperty("os.name") + "; os.arch: " + System.getProperty("os.arch"));
                    System.out.println();
                    System.out.println(Version.licenseString);
                    System.exit(IrpUtils.exitSuccess);
                } else if (args[arg_i].equals("-d") || args[arg_i].equals("--debug")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-e") || args[arg_i].equals("--easy")) {
                    arg_i++;
                    userlevel = 0;
                } else if (args[arg_i].equals("-p") || args[arg_i].equals("--properties") ) {
                    arg_i++;
                    propsfilename = args[arg_i++];
                } else if (args[arg_i].equals("-s") || args[arg_i].equals("--server") ) {
                    arg_i++;
                    portNo = (args.length > arg_i && args[arg_i].charAt(0) != '-')
                            ? Integer.parseInt(args[arg_i++]) : defaultPortNumber;
                } else if (args[arg_i].equals("-v") || args[arg_i].equals("--verbose")) {
                    arg_i++;
                    verbose = true;
                } else {
                    usage();
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            if (debug != 0)
                System.err.println("ArrayIndexOutOfBoundsException: " + e.getMessage());

            usage();
        } catch (NumberFormatException ex) {
            if (debug != 0)
                System.err.println("NumberFormatException " + ex.getMessage());

            usage();
        }

        guiExecute(propsfilename, verbose, debug, userlevel);
    }

    private static void guiExecute(final String propsfilename, final boolean verbose, final int debug, final int userlevel) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                try {
                    GuiMain gui = new GuiMain(propsfilename, verbose, debug, userlevel);
                    gui.setVisible(true);
                    if (portNo > 0)
                        gui.startSocketThread(portNo);
                } catch (FileNotFoundException ex) {
                    System.exit(IrpUtils.exitConfigReadError);
                }

            }
        });
    }
}
