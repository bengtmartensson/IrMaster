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

import org.harctoolbox.harcutils;

/**
 * This class ...
 */
public class IrMaster {

    private static void usage(int exitstatus) {
        System.err.println("Usage: one of");
        System.err.println(helptext);
        System.exit(exitstatus);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }
    private static final String helptext =
            "\tIrMaster [--version|--help]";

    /**
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        int debug = 0;
        int arg_i = 0;
        boolean verbose = false;
        String propsfilename = null;
        String browser = "firefox";

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("--help")) {
                    usage(harcutils.exit_success);
                }
                if (args[arg_i].equals("--version")) {
                    //System.out.println("JVM: "+ System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
                    System.out.println(harcutils.version_string);
                    System.out.println(harcutils.license_string);
                    System.exit(harcutils.exit_success);
                } else if (args[arg_i].equals("-b")) {
                    arg_i++;
                    browser = args[arg_i++];
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-p")) {
                    arg_i++;
                    propsfilename = args[arg_i++];
                } else if (args[arg_i].equals("-v")) {
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
        if (browser != null)
            Props.get_instance().set_browser(browser);
        UserPrefs.get_instance().set_propsfilename(propsfilename);
        UserPrefs.get_instance().set_debug(debug);
        UserPrefs.get_instance().set_verbose(verbose);

        guiExecute();
    }

    /*private void shutdown() {
        try {
            Props.get_instance().save();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_config_write_error);
        }
    }*/

    private static void guiExecute() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new GuiMain().setVisible(true);
            }
        });
    }
}
