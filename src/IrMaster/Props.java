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
 *
 */

// FIXME:
// This code has the problem that if a property is not found, null is returned
// instead of a sensible default, or a warning.
// Rewrite the get* set* function to call a helper function, possibly with default value.

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

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

    private void setup_defaults() {
        String irmasterHome = appendable("IRMASTERHOME");

        update("makehex_irpdir",	irmasterHome + "irps");
        update("irpmaster_configfile",	irmasterHome + "IrpProtocols.ini");
        update("exportdir",	irmasterHome + "exports");
        update("browser",	"firefox");
        update("appname",	"irmaster");
        update("helpfilename" , irmasterHome + "docs" + File.separator + "irmaster.html");
    }

    public Properties get_props() {
        return props;
    }

    public Props(String filename) {
        this.filename = filename;
        need_save = false;
        props = new Properties();
        FileInputStream f;
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

    public boolean save(String filename) throws IOException,FileNotFoundException {
        if (!need_save && filename.equals(this.filename))
            return false;
        
        FileOutputStream f = new FileOutputStream(filename);

        if (use_xml) {
            props.storeToXML(f, "Harc Properties, feel free to hand edit if desired");
        } else {
            props.store(f, "Harc Properties, feel free to hand edit if desired");
        }
        need_save = false;
        return true;
    }

    public String save() throws IOException {
        boolean result = save(filename);
        return result ? filename : null;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }
    
    public String get_irpmaster_configfile() {
        return props.getProperty("irpmaster_configfile");
    }
    
    public void set_irpmaster_configfile(String s) {
        props.setProperty("irpmaster_configfile", s);
        need_save = true;
    }

    public String get_makehex_irpdir() {
        return props.getProperty("makehex_irpdir");
    }

    public void set_makehex_irpdir(String s) {
        props.setProperty("makehex_irpdir", s);
        need_save = true;
    }

    public String get_exportdir() {
        return props.getProperty("exportdir");
    }

    public void set_exportdir(String dir) {
        props.setProperty("exportdir", dir);
        need_save = true;
    }

    public String get_browser() {
        return props.getProperty("browser");
    }

    public void set_browser(String s) {
        props.setProperty("browser", s);
        need_save = true;
    }

    public String get_appname() {
        return props.getProperty("appname");
    }

    public String get_helpfilename() {
        return props.getProperty("helpfilename");
    }

    private static Props instance = null;

    public static void initialize(String filename) {
        if (filename == null)
            filename = default_propsfilename;
        if (instance == null)
            instance = new Props(filename);
    }

    public static void initialize() {
        initialize(null);
    }

    public static void finish() throws IOException {
        instance.save();
    }

    public static Props get_instance() {
        initialize();
        return instance;
    }

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
