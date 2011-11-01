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

// FIXME:
// This code has the problem that if a property is not found, null is returned
// instead of a sensible default, or a warning.
// Rewrite the get* set* function to call a helper function, possibly with default value.

import java.awt.Desktop;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/**
 * This class handles the properties of the program.
 */
public class Props {

    private Properties props;
    private String filename;
    private final static boolean use_xml = true;
    private boolean need_save;
    public static final String default_propsfilename = "IrMaster.properties.xml";

    private String appendable(String env) {
        String str = System.getenv(env);
        return str == null ? "" : str.endsWith(File.separator) ? str : (str + File.separator);
    }

    private void update(String key, String value) {
        if (props.getProperty(key) == null) {
            props.setProperty(key, value);
            need_save = true;
        }
    }

    private static URI pathnameToURI(String pathname) {
        return pathnameToURI(new File(pathname));
    }

    private static URI pathnameToURI(File file) {
        return file.toURI();
    }

    public static void browse(String uri, boolean verbose) {
        browse(URI.create(uri), verbose);
    }

    public static void browse(URI uri, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }
        if (uri == null || uri.toString().isEmpty()) {
            System.err.println("No URI.");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
            if (verbose)
                System.err.println("Browsing URI `" + uri.toString() + "'");
        } catch (IOException ex) {
            System.err.println("Could not start browser using uri `" + uri.toString() + "'" + ex.getMessage());
        }
    }

    public static void open(String filename, boolean verbose) {
        open(new File(filename), verbose);
    }

    public static void open(File file, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }

        try {
            Desktop.getDesktop().open(file);
            if (verbose)
                System.err.println("open file `" + file.toString() + "'");
        } catch (IOException ex) {
            System.err.println("Could not open file `" + file.toString() + "'");
        }
    }

    public static void edit(File file, boolean verbose) {
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop not supported");
            return;
        }

        if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT))
            browse(pathnameToURI(file), verbose);
        else {

            try {
                Desktop.getDesktop().edit(file);
                if (verbose)
                    System.err.println("edit file `" + file.toString() + "'");
            } catch (IOException ex) {
                System.err.println("Could not edit file `" + file.toString() + "'");
            }
        }
    }

    private void setup_defaults() {
        String irmasterHome = appendable("IRMASTERHOME");
        update("makehex_irpdir",	irmasterHome + "irps");
        update("irpmaster_configfile",	irmasterHome + "IrpProtocols.ini");
        update("exportdir",	System.getProperty("java.io.tmpdir") + File.separator + "exports");
        update("helpfileUrl" ,  pathnameToURI(irmasterHome + "docs" + File.separator + "irmaster.html").toString());
    }

    /**
     * Sets up a Props instance from a given file name.
     * @param filename File to read from and, later, save to. Need not exist.
     */
    public Props(String filename) {
        this.filename = filename;
        need_save = false;
        props = new Properties();
        FileInputStream f;
        if (filename == null || filename.isEmpty()) {
            System.err.println("Fatal error: Props filename is empty.");
            return;
        }
        try {
            f = new FileInputStream(filename);
            if (use_xml)
                props.loadFromXML(f);
            else
                props.load(f);
        } catch (FileNotFoundException e) {
            System.err.println("Property File " + filename + " not found, using builtin defaults.");
            f = null;
            setup_defaults();
            need_save = true;
        } catch (IOException e) {
            System.err.println("Property File " + filename + " could not be read, using builtin defaults.");
            f = null;
            setup_defaults();
            need_save = true;
        }
        setup_defaults();
    }

    /**
     * Save instance to given file name.
     *
     * @param filename Filename to be saved to.
     * @return success of operation
     * @throws IOException
     * @throws FileNotFoundException
     */
    public boolean save(String filename) throws IOException,FileNotFoundException {
        if (!need_save && filename.equals(this.filename))
            return false;

        FileOutputStream f = new FileOutputStream(filename);

        if (use_xml) {
            props.storeToXML(f, "IrMaster Properties, feel free to hand edit if desired");
        } else {
            props.store(f, "IrMaster Properties, feel free to hand edit if desired");
        }
        need_save = false;
        return true;
    }

    /**
     * Saves the properties to the default, stored, file name.
     *
     * @return success of operation
     * @throws IOException
     */
    public String save() throws IOException {
        boolean result = save(filename);
        return result ? filename : null;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

    /** Returns the property */
    public String get_irpmaster_configfile() {
        return props.getProperty("irpmaster_configfile");
    }

    /** Sets the property */
    public void set_irpmaster_configfile(String s) {
        props.setProperty("irpmaster_configfile", s);
        need_save = true;
    }

    /** Returns the property */
    public String get_makehex_irpdir() {
        return props.getProperty("makehex_irpdir");
    }

    /** Sets the property */
    public void set_makehex_irpdir(String s) {
        props.setProperty("makehex_irpdir", s);
        need_save = true;
    }

    /** Returns the property */
    public String get_exportdir() {
        return props.getProperty("exportdir");
    }

    /** Sets the property */
    public void set_exportdir(String dir) {
        props.setProperty("exportdir", dir);
        need_save = true;
    }

    /** Returns the property */
    public String get_helpfileUrl() {
        return props.getProperty("helpfileUrl");
    }

    /** Returns the property */
    public Rectangle get_bounds() {
        String str = props.getProperty("bounds");
        if (str == null || str.isEmpty())
            return null;
        String[] arr = str.trim().split(" +");
        return new Rectangle(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]),
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
    }

    /** Sets the property */
    public void set_bounds(Rectangle bounds) {
        props.setProperty("bounds", String.format("%d %d %d %d", bounds.x, bounds.y, bounds.width, bounds.height));
        need_save = true;
    }

    private static Props instance = null;

    /**
     * Initialize a static instance, unless already initialized.
     * @param filename
     */
    public static void initialize(String filename) {
        if (filename == null)
            filename = default_propsfilename;
        if (instance == null)
            instance = new Props(filename);
    }

    /**
     * Initialize a static instance, unless already initialized.
     */
    public static void initialize() {
        initialize(null);
    }

    /**
     * Finish the static instance.
     *
     * @throws IOException
     */
    public static void finish() throws IOException {
        instance.save();
    }

    /**
     * Returns the static instance.
     * @return instance
     */
    public static Props get_instance() {
        initialize();
        return instance;
    }

    /**
     * Just for testing and debugging.
     * @param args
     */
    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : null;
        Props p = new Props(filename);
        p.list();
        try {
            p.save();
        } catch (IOException e) {
        }
    }
}
