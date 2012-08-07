/*
Copyright (C) 2011, 2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published byto
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

import com.hifiremote.exchangeir.Analyzer;
import com.hifiremote.makehex.Makehex;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.IrCalc.HexCalc;
import org.harctoolbox.IrCalc.IrCalc;
import org.harctoolbox.IrCalc.TimeFrequencyCalc;
import org.harctoolbox.IrpMaster.*;
import org.harctoolbox.harchardware.*;

/**
 * This class implements a GUI for several IR programs.
 *
 * Being a user interface, it does not have much of an API itself.
 */

public class GuiMain extends javax.swing.JFrame {

    private final byte[] icondata = {
        // How could a language without unsigned types ever make it?
        -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
        0, 0, 1, 0, 0, 0, 0, -75, 8, 3, 0, 0, 0, -121, -36, -47,
        38, 0, 0, 0, 1, 115, 82, 71, 66, 0, -82, -50, 28, -23, 0, 0,
        0, -28, 80, 76, 84, 69, -1, -1, -1, -3, -3, -3, -5, -5, -5, -7,
        -7, -7, -8, -8, -8, -10, -10, -10, 51, 51, 51, -12, -12, -12, 64, 64,
        64, 58, 58, 58, -14, -14, -14, -23, -23, -23, 113, 113, 113, 83, 83, 83,
        -37, -37, -37, -27, -27, -27, 99, 99, 99, 55, 55, 55, 115, 115, 115, -83,
        -83, -83, 59, 59, 59, 80, 80, 80, -41, -41, -41, -16, -16, -16, -123, -123,
        -123, -70, -70, -70, 120, 120, 120, -125, -125, -125, -95, -95, -95, -51, -51, -51,
        -71, -71, -71, -73, -73, -73, 109, 109, 109, -17, -17, -17, 66, 66, 66, -19,
        -19, -19, 68, 68, 68, -24, -24, -24, 57, 57, 57, 53, 53, 53, -21, -21,
        -21, 70, 70, 70, -26, -26, -26, -28, -28, -28, -120, -120, -120, -114, -114, -114,
        -76, -76, -76, 74, 74, 74, -57, -57, -57, -97, -97, -97, -127, -127, -127, -128,
        -128, -128, 56, 56, 56, -48, -48, -48, 78, 78, 78, -81, -81, -81, -66, -66,
        -66, 86, 86, 86, -90, -90, -90, 96, 96, 96, -30, -30, -30, -31, -31, -31,
        -33, -33, -33, -18, -18, -18, -52, -52, -52, -32, -32, -1, 40, 72, 72, -26,
        -26, -6, 47, 79, 79, -72, -72, -72, 0, 0, 0, -1, 0, 0, 46, -117,
        87, 0, 0, -1, -104, -104, -104, -64, -64, -64, 10, 21, 8, -106, 0, 0,
        0, 9, 112, 72, 89, 115, 0, 0, 11, 19, 0, 0, 11, 19, 1, 0,
        -102, -100, 24, 0, 0, 0, 7, 116, 73, 77, 69, 7, -36, 8, 6, 16,
        45, 17, -22, -113, -69, -124, 0, 0, 1, 102, 73, 68, 65, 84, 120, -38,
        -19, -42, 65, 14, -125, 32, 16, 5, 80, 111, -48, 91, 105, 12, -9, -65,
        79, 81, -85, -123, 110, 26, 109, -63, -59, -68, -65, 32, -114, -118, 99, 30,
        -104, 56, 60, -126, 103, 8, 15, 48, 6, -49, 80, -92, 42, 78, -92, -9,
        -68, 102, 13, 1, 0, 8, 14, -112, 46, 62, -73, -9, -68, 102, 13, 1,
        68, 7, -104, -114, 108, -27, 122, 106, 31, -106, 114, -38, -122, -41, -75, -19,
        84, -66, -104, -34, 85, 125, -41, -87, -9, -103, -113, -12, 5, 88, 126, 6,
        -46, -102, -87, -56, 71, 121, 37, -23, 92, -26, 34, -71, 72, -67, 82, 109,
        -128, -22, -24, 87, -128, -109, 11, -14, 94, -8, 124, -12, 101, 23, 52, -6,
        4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 64, 111, -128, 27, 82, 0, -12, -52, 48, -26, -92, 45, 83, -38, 115,
        -117, -64, -47, -67, 47, 64, -5, -99, -11, -9, 121, -51, 26, 2, -120, 14,
        48, 94, 124, 110, -17, 121, -51, 26, 2, 0, 16, 17, 96, 12, -98, 39,
        43, -16, -16, 27, -68, -73, -119, -3, 0, 0, 0, 0, 73, 69, 78, 68,
        -82, 66, 96, -126
    };

    private class UiFeatures {
        public boolean optionsPane = true;
        public boolean irCalcPane = true;
        public boolean outputPane = true;
        public boolean warDialerPane = true;
        public boolean rendererSelector = true;
        public boolean saveProperties = true;
        public boolean additionalParameters = true;
        public boolean exportFormatSelector = true;
        public boolean irpLine = true;
        public boolean analyzeButton = true;
        public boolean discardRepeatMins = true;
        public boolean lotsOfDocumentation = true;

        public UiFeatures(int userlevel) {
            optionsPane = userlevel > 0;
            irCalcPane = userlevel > 0;
            outputPane = userlevel > 0;
            warDialerPane = userlevel > 0;
            rendererSelector = userlevel > 0;
            saveProperties = userlevel > 0;
            additionalParameters = userlevel > 0;
            exportFormatSelector = userlevel > 0;
            irpLine = userlevel > 0;
            analyzeButton = userlevel > 0;
            discardRepeatMins = userlevel > 0;
            lotsOfDocumentation = userlevel > 0;
        }
    }
    
    private final static String jp1WikiUrl = "http://www.hifi-remote.com/wiki/index.php?title=Main_Page";
    private final static String irpNotationUrl = "http://www.hifi-remote.com/wiki/index.php?title=IRP_Notation";
    private final static String decodeIrUrl = "http://www.hifi-remote.com/wiki/index.php?title=DecodeIR";

    private static final String analyzeHelpText = "This panel can serve two different use cases:\n\n"
            + "1. Generation of IR signals.\n"
            + "Select a protocol name, and enter the desired parameters "
            + "(D, F, sometimes S, sometimed T, in rare cases others). "
            + "Press the button \"Render\". "
            + "The signal will now be computed and the result presented in the middle window."
            + "\n\n"
            + "2. Analysis of IR signals.\n"
            + "The program can analyze the signal in the middle window. "
            + "This can be the result of a computation by \"Render\", can be typed into the window manually, "
            + "pasted from the clipboard, or imported through the \"Import\" feature. "
            + "The signal may be in Pronto CCF format, or in UEI learned format. "
            + "With appropriate knowledge, it is of course also possible to manually modify an already present signal."
            + "By pressing the \"Decode\" button, the signal is sent to the DecodeIR program, "
            + "and the result printed to the console (the lower window). "
            + "The \"Plot\" button produces a graphical plot of the signal."
            + "\n\n"
            + "This help text describes the \"Easy\" mode of the program. The full mode contains some more possibilities.";

    private static final String exportHelpText = 
            "This panel \"exports\" the protocol selected in the upper part of the pane.\n\n"
            + "Where the \"Analyze\" pane generates one single signal, for inspection, modification, analysis,"
            + " this pane instead creates a text file"
            + " containing the rendered signals for several different values of the parameter F."
            + " The format can be read both by humans and programs."
            + " The exported signals will contain all F-values between the one on the upper row, and the one called \"Last F\" (inclusive)."
            + " All the other parameter values will be equal to the parameters from in the top row."
            + "\n\n"
            + "The export file will be placed in the selected export directory. If \"Automatic File Names\" is selected,"
            + " the filename will be selected automatically, otherwise a file selector will be used to allow selection of a file name."
            + " Export can take place in either the Pronto format, and/or so-called raw format."
            + " The button \"Export\" performs the actual export."
            + "\n\n"
            + "This help text describes the \"Easy\" mode of the program. The full mode contains some more possibilities.";

    private static final String warDialerHelpText = "Using this panel, it is possible to search for undocumented IR commands"
            + " by sending whole sequences of IR signals to a device."
            + " It requires hardware support of some sort, see the \"Output HW\" pane and its subpanes."
            + " It will send all signals with F-values between the one in the top row, and the one entered as \"Ending F\","
            + " separated by the delay chosen."
            + "\n\n"
            + "The procedure is stared by the \"Start\" button, and can be stopped with the \"Stop\" button."
            + " The \"Pause\" button will allow to pause and resume, but is not implemented yet."
            + " Also a note-taking facility is planned, but not yet implemented.";

    private static final String globalCacheHelpText = "Using this panel, it is possible to configure support for the"
            + " GC-100 and i-Tach families of IR products from Global Caché."
            + " This may transform an IR signal as computed by this program (i.e. a bunch of numbers),"
            + " to a physical IR signal that can be used to control physical equipment."
            + "\n\n"
            + "In the IP name/address field, either IP address or name (as known on the computer) can be entered."
            + "When pressing the return key in the text filed, it is attempted to identfiy the unit,"
            + " and only the actually available IR modules will be available for selection."
            + " Module, and Port selectors determine exactly where the IR signal will be output."
            + " The exact meaning is described in the Global Caché documentation."
            + "\n\n"
            + "Using the \"Stop\" button, an ongoing transmission can be stopped."
            + " \"Browse\" directs the user's browser to the in the IP name/address selected unit."
            + " \"Ping\" tries to ping the unit, i.e., to determine if it is reachable on the network."
            + "\n\n"
            + "Instead of manually entering IP address or name, pressing the \"Discover\" button tries to discover"
            + " a unit on the LAN, using the Global Caché's discovery beacon. This may take up to 60 seconds,"
            + " and is only implemented on recent firmware."
            + "\n\n"
            + "Settings are save between sessions.";

    private static final String irtransHelpText = "Using this panel, it is possible to configure support for the"
            + " IrTrans WLAN, LAN, Ethernet, and Ethernet PoE Models, with or without data base."
            + " This may transform an IR signal as computed by this program (i.e. a bunch of numbers),"
            + " to a physical IR signal that can be used to control physical equipment."
            + " For devices with a local flash memory (for example IRT-LAN-DB) (called \"data base\" by IrTrans),"
            + " the therein stored signals can be sent."
            + "\n\n"
            + "In the IP name/address field, either IP address or name (as known on the computer) can be entered."
            + " IR Port has to be sensibly selected."
            + " \"Browse\" directs the user's browser to the in the IP name/address selected unit."
            + " \"Ping\" tries to ping the unit, i.e., to determine if it is reachable on the network."
            + "\n\n"
            + "For devices with data base, pressing the \"Read\" button will attempt to read the configured"
            + " remotes into the memory of this program."
            + " These can then be selected in the \"Remote\" combo box,"
            + " and the corresponding commands from the \"Command\" combo box."
            + " By pressing the \"Send\" button, the number of sends from the \"# Sends\" combo box"
            + " will be sent by the IrTrans, using its internal data base."
            + "\n\n"
            + "Settings are save between sessions.";

    private static final String lircHelpText =
            "To be fully usable for IrMaster, the LIRC server has to be extended to be able to cope with CCF signals"
            + " not residing in the local data base, but sent from a client like IrMaster, thus mimicing the function of e.g."
            + " a GlobalCaché. The needed modification is described on the project home page."
            + " However, even without this patch, the configuration page can be used to send the predefined commands"
            + " (i.e. residing it its data base lirc.conf). It can be considered as a GUI version of the irsend command."
            + "\n\n"
            + "The LIRC server needs to be started in network listening mode with the -l or --listen option."
            + " Default TCP port is 8765."
            + "\n\n"
            + "After entering IP-Address or name, and port (stay with 8765 unless a reason to do otherwise),"
            + " press the \"Read\" button. This will query the LIRC server for its version (to replace the"
            + " grayed out \"<unknown>\" of the virgin IrMaster), and its known remotes and their commands."
            + " Thus, the \"Remote\" and \"Command\" combo boxes should now be selectable."
            + " After selecting a remote and one of its command, it can be sent to the LIRC server"
            + " by pressing the \"Send\" button. If (and only if) the LIRC server has the above described patch applied,"
            + " selecting \"LIRC\" on the \"Analyze\" and \"War Dialer\" panes now works.";

    private static final String audioHelpText = 
            "IrMaster can generate wave files, that can be used to control IR-LEDs."
            + " This technique has been described many times in the internet the last few years, see links on the project's home page."
            + " The hardware consists of a pair of anti-paralell IR-LEDs, preferably in series with a resistor."
            + " Theoretically, this corresponds to a full wave rectification of a sine wave. "
            + " Taking advantage of the fact that the LEDs are conducting only for a the time when the forward voltage exceeds a certain threshold,"
            + " it is easy to see that this will generate an on/off signal with the double frequency of the original sine wave."
            + " Thus, a IR carrier of 38kHz (which is fairly typical) can be generated through a 19kHz audio signal,"
            + " which is (as opposed to 38kHz) within the realm of medium quality sound equipment,"
            + " for example using mobile devices."
            + "\n\n"
            + "IrMaster can generate these audio signals as wave files, which can be exported from the export pane, or sent to the local computers sound card."
            + " There are some settings available: Sample frequency (42100, 48000, 96000, 192000Hz),"
            + " sample size (8 or 16 bits) can be selected."
            + " Also \"stereo\" files can be generated by selecting the number of channels to be 2."
            + " The use of this feature is somewhat limited:"
            + " it just generates another channel in opposite phase to the first one,"
            + " for hooking up the IR LEDs to the difference signal between the left and the right channel."
            + " This will buy you double amplitude (6 dB) at the cost of doubling the file sizes."
            + " If the possibility exists, it is better to turn up the volume instead."
            + "\n\n"
            + "Data can be generated in little-endian (default) or big-endian format."
            + " This applies only to 16-bit sample sizes."
            + "\n\n"
            + "As an experimental option, the carrier frequency division as described above can be turned off"
            + " (the \"Divide carrier\" checkbox)."
            + " This is only meaningful for sample frequencies of 96kHz and higher,"
            + " and for \"audio equipment\" able to reproduce frequencies like 36kHz and above."
            + "\n\n"
            + "Most of \"our\" IR sequences ends with a period of silence almost for the half of the total duration."
            + " By selecting the \"Omit trailing gap\"-option, this trailing gap is left out of the generated data"
            + " -- it is just silence anyhow. This is probably a good choice (almost) always."
            + "\n\n"
            + " Finally, the wave form on the modulation signal can be selected to either sine or square wave."
            + " For practical usage, my experiments shown no real performance difference."
            ;

    private static GuiMain instance = null;
    private static IrpMaster irpMaster = null;
    private static HashMap<String, Protocol> protocols = null;
    private final static long invalidParameter = IrpUtils.invalid;
    private int debug = 0;
    private boolean verbose = false;
    private String[] lafNames;
    private UIManager.LookAndFeelInfo[] lafInfo;
    private JRadioButton[] lafRadioButtons;
    private static final String IrpFileExtension = "irp";
    private GlobalcacheThread globalcacheProtocolThread = null;
    private IrtransThread irtransThread = null;
    private File lastExportFile = null;
    private UiFeatures uiFeatures;

    private javax.swing.DefaultComboBoxModel noSendsSignalsComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private javax.swing.DefaultComboBoxModel noSendsLircPredefinedsComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private javax.swing.DefaultComboBoxModel noSendsIrtransFlashedComboBoxModel =
            new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" });
    private GlobalCache gc = null;
    private IrTransIRDB irt = null;
    private LircCcfClient lircClient = null;
    private static final int lircTransmitterDefaultIndex = 1;
    private int lircSelectedTransmitter = -1;
    private int hardwareIndex = 0;
    private final static int hardwareIndexGlobalCache = 0;
    private final static int hardwareIndexIrtrans = 1;
    private final static int hardwareIndexLirc = 2;
    private final static int hardwareIndexAudio = 3;
    private AudioFormat audioFormat = null;
    private SourceDataLine audioLine = null;
    private int plotNumber = 0;
    String codeNotationString = null;

    private HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    // Interfaces to Desktop
    private static void browse(String uri, boolean verbose) {
        browse(URI.create(uri), verbose);
    }

    private static void browse(URI uri, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            instance.error("Desktop not supported");
            return;
        }
        if (uri == null || uri.toString().isEmpty()) {
            instance.error("No URI.");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
            if (verbose)
                instance.trace("Browsing URI `" + uri.toString() + "'");
        } catch (IOException ex) {
            instance.error("Could not start browser using uri `" + uri.toString() + "'" + ex.getMessage());
        }
    }

    private static void open(String filename, boolean verbose) {
        open(new File(filename), verbose);
    }

    private static void open(File file, boolean verbose) {
        if (! Desktop.isDesktopSupported()) {
            instance.error("Desktop not supported");
            return;
        }

        try {
            Desktop.getDesktop().open(file);
            if (verbose)
                instance.trace("open file `" + file.toString() + "'");
        } catch (IOException ex) {
            instance.error("Could not open file `" + file.toString() + "'");
        }
    }

    private static void edit(File file, boolean verbose) {
        if (!Desktop.isDesktopSupported()) {
            instance.error("Desktop not supported");
            return;
        }

        if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT))
            browse(file.toURI(), verbose);
        else {

            try {
                Desktop.getDesktop().edit(file);
                if (verbose)
                    instance.trace("edit file `" + file.toString() + "'");
            } catch (IOException ex) {
                instance.error("Could not edit file `" + file.toString() + "'");
            }
        }
    }
    
    private File selectFile(String title, boolean save, String defaultdir, String extension, String fileTypeDesc) {
        return selectFile(title, save, defaultdir, new String[]{extension, fileTypeDesc});
    }
    
    private File selectFile(String title, boolean save, String defaultdir, String[]... filetypes) {
        String startdir = filechooserdirs.containsKey(title) ? filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setDialogTitle(title);
        if (filetypes[0][0] == null || filetypes[0][0].equals("")) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter(filetypes[0][1], filetypes[0][0]));
            for (int i = 1; i < filetypes.length; i++)
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(filetypes[i][1], filetypes[i][0]));
        }
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else
            return null;
    }

    private class CopyClipboardText implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void toClipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }

        public String fromClipboard() {
            try {
                return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                error(ex);
            } catch (IOException ex) {
                error(ex);
            }
            return null;
        }
    }

    private static Protocol getProtocol(String name) throws UnassignedException, RecognitionException {
        if (!protocols.containsKey(name)) {
            Protocol protocol = irpMaster.newProtocol(name);
            protocols.put(name, protocol);
        }
        return protocols.get(name);
    }

    /**
     * Main class for the GUI.
     *
     * @param verbose Verbose execution of some commands, dependent on invoked programs.
     * @param debug Debug value handed over to invoked programs/functions.
     */
    public GuiMain(boolean verbose, int debug, int userlevel) {
        if (instance != null) {
            System.err.println("This class can only be instantiated once.");
            return;
        } else
            instance = this;

        this.verbose = verbose;
        this.debug = debug;
        this.uiFeatures = new UiFeatures(userlevel);
        lafInfo = UIManager.getInstalledLookAndFeels();
        lafNames = new String[lafInfo.length];
        for (int i = 0; i < lafInfo.length; i++)
            lafNames[i] = lafInfo[i].getName();

        try {
            UIManager.setLookAndFeel(lafInfo[Props.getInstance().getLookAndFeel()].getClassName());
        } catch (ClassNotFoundException ex) {
            error(ex.getMessage());
        } catch (InstantiationException ex) {
            error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            error(ex.getMessage());
        } catch (UnsupportedLookAndFeelException ex) {
            error(ex.getMessage());
        }

        try {
            irpMaster = new IrpMaster(Props.getInstance().getIrpmasterConfigfile());
        } catch (FileNotFoundException ex) {
            fatal(Props.getInstance().getIrpmasterConfigfile() + " not found, exiting.", IrpUtils.exitConfigReadError);
        } catch (IncompatibleArgumentException ex) {
            error(ex.getMessage());
        }
        protocols = new HashMap<String, Protocol>();

        initComponents();
        if (userlevel == 0)
            setTitle("IrMaster Easy");

        ButtonGroup lafButtonGroup = new ButtonGroup();
        lafRadioButtons = new JRadioButton[lafInfo.length];
        int index = 0;
        for (String laf : lafNames) {
            JRadioButton menu = new JRadioButton(laf);
            lafRadioButtons[index] = menu;
            final int lafIndex = index;
            menu.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    updateLAF(lafIndex);
                }
            });

            lafButtonGroup.add(menu);
            lafMenu.add(menu);
            index++;
        }

        updateLAF(Props.getInstance().getLookAndFeel());
        lafMenu.setVisible(uiFeatures.optionsPane);
        lafSeparator.setVisible(uiFeatures.optionsPane);

        if (uiFeatures.optionsPane) {
            for (int i = 0; i < Debug.item.size(); i++) {
                JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem();
                cbmi.setText(Integer.toString(1 << i) + ": " + Debug.item.helpString(i));
                cbmi.setSelected((debug & (1 << i)) != 0);
                cbmi.addActionListener(new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        evaluateDebug();
                    }
                });
                debugMenu.add(cbmi);
            }
        }

        debugMenu.setVisible(uiFeatures.optionsPane);
        debugSeparator.setVisible(uiFeatures.optionsPane);
        irProtocolDatabaseMenu.setVisible(uiFeatures.optionsPane);
        usePopupsCheckBoxMenuItem.setVisible(uiFeatures.optionsPane);

        protocolAnalyzeButton.setVisible(uiFeatures.analyzeButton);
        rendererComboBox.setEnabled(uiFeatures.rendererSelector);
        if (!uiFeatures.rendererSelector) {
            rendererComboBox.setToolTipText(null);
            rendererLabel.setText(null);
            rendererComboBox.setVisible(uiFeatures.rendererSelector);
        }

        IrpTextField.setVisible(uiFeatures.irpLine);
        analyzeSendPanel.setVisible(uiFeatures.outputPane);
        popupsForHelpCheckBoxMenuItem.setVisible(userlevel > 0);

        exportFormatComboBox.setEnabled(uiFeatures.exportFormatSelector);
        exportRepetitionsComboBox.setVisible(uiFeatures.exportFormatSelector);
        exportNoRepetitionsLabel.setVisible(uiFeatures.exportFormatSelector);

        if (!uiFeatures.outputPane && !uiFeatures.irCalcPane && !uiFeatures.optionsPane) {
            mainTabbedPane.setEnabled(false);
            mainTabbedPane.remove(0);
            mainSplitPane.setTopComponent(this.protocolsPanel);
        }

        toolsMenu.setVisible(uiFeatures.irCalcPane);

        Rectangle bounds = Props.getInstance().getBounds();
        if (bounds != null)
            setBounds(bounds);

        gcModuleComboBox.setSelectedItem(Integer.toString(Props.getInstance().getGlobalcacheModule()));
        gcConnectorComboBox.setSelectedItem(Integer.toString(Props.getInstance().getGlobalcachePort()));

        irtransLedComboBox.setSelectedIndex(Props.getInstance().getIrTransPort());

        disregardRepeatMinsCheckBoxMenuItem.setSelected(Props.getInstance().getDisregardRepeatMins());

        popupsForHelpCheckBoxMenuItem.setSelected(Props.getInstance().getPopupsForHelp());

        ByteArrayInputStream iconStream = new ByteArrayInputStream(icondata);
        try {
            BufferedImage img = ImageIO.read(iconStream);
            setIconImage(img);
        } catch (IOException ex) {
            assert false;
        }

        System.setErr(consolePrintStream);
        System.setOut(consolePrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Props.getInstance().save();
                } catch (Exception e) {
                    System.err.println("Problems saving properties; " + e.getMessage());
                }
                System.err.println("*** Normal GUI shutdown ***");
            }
        });

        protocolComboBox.setSelectedItem(Props.getInstance().getProtocol());
        verboseCheckBoxMenuItem.setSelected(verbose);
        try {
            if (uiFeatures.outputPane) {
                gc = new GlobalCache(Props.getInstance().getGlobalcacheIpName(), verbose);
                updateGlobalCache(false);
            }
        } catch (HarcHardwareException ex) {
            
        }
        irt = new IrTransIRDB("irtrans", verbose);

        exportdirTextField.setText(Props.getInstance().getExportdir());
        hardwareIndex = Integer.parseInt(Props.getInstance().getHardwareIndex());
        protocolOutputhwComboBox.setSelectedIndex(hardwareIndex);
        warDialerOutputhwComboBox.setSelectedIndex(hardwareIndex);
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
        enableExportFormatRelated();
        updateProtocolParameters(true);
    }

    // From Real Gagnon
    class FilteredStream extends FilterOutputStream {

        public FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b);
            consoleTextArea.append(aString);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            consoleTextArea.append(aString);
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        }
    }

    PrintStream consolePrintStream = new PrintStream(
            new FilteredStream(
            new ByteArrayOutputStream()));
 
    private void info(String message) {
         if (Props.getInstance().getUsePopupsForErrors()) {
            JOptionPane.showMessageDialog(this, message, "IrMaster information",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            System.err.println(message);
        }
    }
    private void trace(String message) {
        System.err.println(message);
    }

    // A message is there to be used, not to be clicked away.
    // Do not use popups here.
    private void message(String message) {
        System.err.println(message);
    }
    
    private void warning(String message) {
         if (Props.getInstance().getUsePopupsForErrors()) {
            JOptionPane.showMessageDialog(this, message, "IrMaster warning",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            System.err.println("Warning: " + message);
        }
    }

    private void error(String message) {
        if (Props.getInstance().getUsePopupsForErrors()) {
            JOptionPane.showMessageDialog(this, message, "IrMaster error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            System.err.println("Error: " + message);
        }
    }
    
    private void fatal(String message, int exitstatus) {
        if (Props.getInstance().getUsePopupsForErrors()) {
            JOptionPane.showMessageDialog(this, message, "IrMaster fatal error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            System.err.println("Error: " + message);
        }
        System.exit(exitstatus);
    }
    
    private void error(Exception ex) {
        error(ex.getMessage());
    }

    private void evaluateDebug() {
        debug = 0;
        for (int i = 0; i < Debug.item.size(); i++)
            if (((JCheckBoxMenuItem) debugMenu.getItem(i)).isSelected())
                debug += (1 << i);

        if (verbose)
            trace("debug is now " + debug);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        consolePopupMenu = new javax.swing.JPopupMenu();
        consoleClearMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        consoleCopySelectionMenuItem = new javax.swing.JMenuItem();
        consoleCopyMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        consoleSaveMenuItem = new javax.swing.JMenuItem();
        CCFCodePopupMenu = new javax.swing.JPopupMenu();
        rawCodeClearMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        rawCodeCopyMenuItem = new javax.swing.JMenuItem();
        rawCodeCopyAllMenuItem = new javax.swing.JMenuItem();
        rawCodePasteMenuItem = new javax.swing.JMenuItem();
        rawCodeSelectAllMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        rawCodeSaveMenuItem = new javax.swing.JMenuItem();
        rawCodeImportMenuItem = new javax.swing.JMenuItem();
        copyPopupMenu = new javax.swing.JPopupMenu();
        copyMenuItem = new javax.swing.JMenuItem();
        copyPastePopupMenu = new javax.swing.JPopupMenu();
        cpCopyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        mainSplitPane = new javax.swing.JSplitPane();
        mainTabbedPane = new javax.swing.JTabbedPane();
        protocolsPanel = new javax.swing.JPanel();
        protocolComboBox = new javax.swing.JComboBox();
        devicenoTextField = new javax.swing.JTextField();
        subdeviceTextField = new javax.swing.JTextField();
        commandnoTextField = new javax.swing.JTextField();
        toggleComboBox = new javax.swing.JComboBox();
        protocolParamsTextField = new javax.swing.JTextField();
        protocolsSubPane = new javax.swing.JTabbedPane();
        analyzePanel = new javax.swing.JPanel();
        IrpTextField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        protocolRawTextArea = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        protocolGenerateButton = new javax.swing.JButton();
        protocolAnalyzeButton = new javax.swing.JButton();
        protocolPlotButton = new javax.swing.JButton();
        protocolDecodeButton = new javax.swing.JButton();
        protocolImportButton = new javax.swing.JButton();
        protocolClearButton = new javax.swing.JButton();
        analyzeSendPanel = new javax.swing.JPanel();
        protocolSendButton = new javax.swing.JButton();
        protocolOutputhwComboBox = new javax.swing.JComboBox();
        protocolStopButton = new javax.swing.JButton();
        noSendsProtocolComboBox = new javax.swing.JComboBox();
        analyzeHelpButton = new javax.swing.JButton();
        exportPanel = new javax.swing.JPanel();
        protocolExportButton = new javax.swing.JButton();
        automaticFileNamesCheckBox = new javax.swing.JCheckBox();
        exportGenerateTogglesCheckBox = new javax.swing.JCheckBox();
        lastFTextField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        exportFormatComboBox = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        exportdirTextField = new javax.swing.JTextField();
        exportdirBrowseButton = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        exportRawCheckBox = new javax.swing.JCheckBox();
        exportProntoCheckBox = new javax.swing.JCheckBox();
        viewExportButton = new javax.swing.JButton();
        openExportDirButton = new javax.swing.JButton();
        exportRepetitionsComboBox = new javax.swing.JComboBox();
        exportNoRepetitionsLabel = new javax.swing.JLabel();
        exportHelpButton = new javax.swing.JButton();
        exportUeiLearnedCheckBox = new javax.swing.JCheckBox();
        warDialerPanel = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        notesClearButton = new javax.swing.JButton();
        notesSaveButton = new javax.swing.JButton();
        notesEditButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        pauseButton = new javax.swing.JButton();
        currentFTextField = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel60 = new javax.swing.JLabel();
        endFTextField = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        delayTextField = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        warDialerOutputhwComboBox = new javax.swing.JComboBox();
        jLabel29 = new javax.swing.JLabel();
        warDialerNoSendsComboBox = new javax.swing.JComboBox();
        warDialerHelpButton = new javax.swing.JButton();
        rendererComboBox = new javax.swing.JComboBox();
        rendererLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        deviceNumberLabel = new javax.swing.JLabel();
        subDeviceNumberLabel = new javax.swing.JLabel();
        functionNumberLabel = new javax.swing.JLabel();
        toggleLabel = new javax.swing.JLabel();
        additionalParametersLabel = new javax.swing.JLabel();
        protocolDocButton = new javax.swing.JButton();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        globalcachePanel = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        gcDiscoveredTypeLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        gcAddressTextField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        discoverButton = new javax.swing.JButton();
        jLabel35 = new javax.swing.JLabel();
        gcBrowseButton = new javax.swing.JButton();
        jLabel36 = new javax.swing.JLabel();
        gcConnectorComboBox = new javax.swing.JComboBox();
        globalCachePingButton = new javax.swing.JButton();
        gcModuleComboBox = new javax.swing.JComboBox();
        globalCacheHelpButton = new javax.swing.JButton();
        irtransPanel = new javax.swing.JPanel();
        irtransVersionLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        irtransIPPanel = new javax.swing.JPanel();
        irtransPingButton = new javax.swing.JButton();
        irtransAddressTextField = new javax.swing.JTextField();
        irtransBrowseButton = new javax.swing.JButton();
        jLabel38 = new javax.swing.JLabel();
        irtransLedComboBox = new javax.swing.JComboBox();
        readButton = new javax.swing.JButton();
        jLabel37 = new javax.swing.JLabel();
        irtransPredefinedPanel = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        irtransCommandsComboBox = new javax.swing.JComboBox();
        noSendsIrtransFlashedComboBox = new javax.swing.JComboBox();
        jLabel53 = new javax.swing.JLabel();
        irtransRemotesComboBox = new javax.swing.JComboBox();
        jLabel51 = new javax.swing.JLabel();
        irtransSendFlashedButton = new javax.swing.JButton();
        irtransHelpButton = new javax.swing.JButton();
        lircPanel = new javax.swing.JPanel();
        lircServerVersionText = new javax.swing.JLabel();
        lircServerVersionLabel = new javax.swing.JLabel();
        lircIPPanel = new javax.swing.JPanel();
        jLabel45 = new javax.swing.JLabel();
        LircIPAddressTextField = new javax.swing.JTextField();
        readLircButton = new javax.swing.JButton();
        lircPingButton = new javax.swing.JButton();
        lircPortTextField = new javax.swing.JTextField();
        jLabel44 = new javax.swing.JLabel();
        lircTransmitterComboBox = new javax.swing.JComboBox();
        jLabel46 = new javax.swing.JLabel();
        lircStopIrButton = new javax.swing.JButton();
        lircPredefinedPanel = new javax.swing.JPanel();
        jLabel47 = new javax.swing.JLabel();
        lircCommandsComboBox = new javax.swing.JComboBox();
        jLabel49 = new javax.swing.JLabel();
        lircRemotesComboBox = new javax.swing.JComboBox();
        jLabel50 = new javax.swing.JLabel();
        lircSendPredefinedButton = new javax.swing.JButton();
        jLabel48 = new javax.swing.JLabel();
        noLircPredefinedsComboBox = new javax.swing.JComboBox();
        lircHelpButton = new javax.swing.JButton();
        audioPanel = new javax.swing.JPanel();
        audioGetLineButton = new javax.swing.JButton();
        audioReleaseLineButton = new javax.swing.JButton();
        jLabel59 = new javax.swing.JLabel();
        audioFormatPanel = new javax.swing.JPanel();
        jLabel56 = new javax.swing.JLabel();
        audioSampleSizeComboBox = new javax.swing.JComboBox();
        jLabel55 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        audioSampleFrequencyComboBox = new javax.swing.JComboBox();
        jLabel58 = new javax.swing.JLabel();
        audioChannelsComboBox = new javax.swing.JComboBox();
        audioBigEndianCheckBox = new javax.swing.JCheckBox();
        audioOptionsPanel = new javax.swing.JPanel();
        audioWaveformComboBox = new javax.swing.JComboBox();
        audioOmitCheckBox = new javax.swing.JCheckBox();
        audioDivideCheckBox = new javax.swing.JCheckBox();
        jLabel30 = new javax.swing.JLabel();
        audioHelpButton = new javax.swing.JButton();
        consoleScrollPane = new javax.swing.JScrollPane();
        consoleTextArea = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        consoletextSaveMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        copyConsoleToClipboardMenuItem = new javax.swing.JMenuItem();
        clearConsoleMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        verboseCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        disregardRepeatMinsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        irProtocolDatabaseMenu = new javax.swing.JMenu();
        irpMasterDatabaseMenu = new javax.swing.JMenu();
        irpMasterDbEditMenuItem = new javax.swing.JMenuItem();
        irpMasterDbSelectMenuItem = new javax.swing.JMenuItem();
        makehexDatabaseMenu = new javax.swing.JMenu();
        makehexDbEditMenuItem = new javax.swing.JMenuItem();
        makehexDbSelectMenuItem = new javax.swing.JMenuItem();
        lafSeparator = new javax.swing.JPopupMenu.Separator();
        usePopupsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        popupsForHelpCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        lafMenu = new javax.swing.JMenu();
        debugSeparator = new javax.swing.JPopupMenu.Separator();
        debugMenu = new javax.swing.JMenu();
        toolsMenu = new javax.swing.JMenu();
        IrCalcMenuItem = new javax.swing.JMenuItem();
        FrequencyTimeCalcMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        checkUpdatesMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        browseHomePageMenuItem = new javax.swing.JMenuItem();
        contentMenuItem = new javax.swing.JMenuItem();
        browseIRPMasterMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        browseIRPSpecMenuItem = new javax.swing.JMenuItem();
        browseDecodeIRMenuItem = new javax.swing.JMenuItem();
        browseJP1Wiki = new javax.swing.JMenuItem();

        consoleClearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        consoleClearMenuItem.setText("Clear");
        consoleClearMenuItem.setToolTipText("Discard the content of the console window.");
        consoleClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleClearMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleClearMenuItem);
        consolePopupMenu.add(jSeparator5);

        consoleCopySelectionMenuItem.setText("Copy selection");
        consoleCopySelectionMenuItem.setToolTipText("Copy currently selected text to the clipboard.");
        consoleCopySelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopySelectionMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopySelectionMenuItem);

        consoleCopyMenuItem.setText("Copy all");
        consoleCopyMenuItem.setToolTipText("Copy the content of the console to the clipboard.");
        consoleCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopyMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopyMenuItem);
        consolePopupMenu.add(jSeparator8);

        consoleSaveMenuItem.setText("Save...");
        consoleSaveMenuItem.setToolTipText("Save the content of the console to a text file.");
        consoleSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleSaveMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleSaveMenuItem);

        rawCodeClearMenuItem.setText("Clear");
        rawCodeClearMenuItem.setToolTipText("Clean this area");
        rawCodeClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeClearMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeClearMenuItem);
        CCFCodePopupMenu.add(jSeparator3);

        rawCodeCopyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        rawCodeCopyMenuItem.setText("Copy selection");
        rawCodeCopyMenuItem.setToolTipText("Copy current selection to the clipboard");
        rawCodeCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyMenuItem);

        rawCodeCopyAllMenuItem.setText("Copy all");
        rawCodeCopyAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyAllMenuItem);

        rawCodePasteMenuItem.setText("Paste");
        rawCodePasteMenuItem.setToolTipText("Paste from clipboard");
        rawCodePasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteMenuItem);

        rawCodeSelectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        rawCodeSelectAllMenuItem.setText("Select all");
        rawCodeSelectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSelectAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSelectAllMenuItem);
        CCFCodePopupMenu.add(jSeparator7);

        rawCodeSaveMenuItem.setText("Save...");
        rawCodeSaveMenuItem.setToolTipText("Save current content to text file");
        rawCodeSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSaveMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSaveMenuItem);

        rawCodeImportMenuItem.setText("Import...");
        rawCodeImportMenuItem.setToolTipText("Import from external file");
        rawCodeImportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeImportMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeImportMenuItem);

        copyMenuItem.setMnemonic('C');
        copyMenuItem.setText("Copy");
        copyMenuItem.setToolTipText("Copy content of window to clipboard.");
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        copyPopupMenu.add(copyMenuItem);

        cpCopyMenuItem.setMnemonic('C');
        cpCopyMenuItem.setText("Copy");
        cpCopyMenuItem.setToolTipText("Copy content of window to clipboard.");
        cpCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cpCopyMenuItemActionPerformed(evt);
            }
        });
        copyPastePopupMenu.add(cpCopyMenuItem);

        pasteMenuItem.setMnemonic('P');
        pasteMenuItem.setText("Paste");
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });
        copyPastePopupMenu.add(pasteMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IrMaster"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainSplitPane.setBorder(null);
        mainSplitPane.setDividerLocation(450);
        mainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setToolTipText("splitter tooltext");

        mainTabbedPane.setToolTipText("This tab allows the change of options for the program.");
        mainTabbedPane.setPreferredSize(new java.awt.Dimension(600, 472));

        protocolsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), null));
        protocolsPanel.setToolTipText("This pane deals with generating, sending, exporting, and analyzing of IR protocols.");
        protocolsPanel.setPreferredSize(new java.awt.Dimension(600, 377));

        protocolComboBox.setMaximumRowCount(20);
        protocolComboBox.setModel(new DefaultComboBoxModel(irpMasterProtocols()));
        protocolComboBox.setToolTipText("Protocol name");
        protocolComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        protocolComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        protocolComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        protocolComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolComboBoxActionPerformed(evt);
            }
        });

        devicenoTextField.setToolTipText("D, Device number");
        devicenoTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        devicenoTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        devicenoTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        devicenoTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                devicenoTextFieldActionPerformed(evt);
            }
        });
        devicenoTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                devicenoTextFieldFocusLost(evt);
            }
        });

        subdeviceTextField.setToolTipText("S, Subdevice number");
        subdeviceTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        subdeviceTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        subdeviceTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });

        commandnoTextField.setToolTipText("F, Function number (also called Command number or OBC).");
        commandnoTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        commandnoTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        commandnoTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        commandnoTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandnoTextFieldActionPerformed(evt);
            }
        });
        commandnoTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandnoTextFieldFocusLost(evt);
            }
        });

        toggleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "-" }));
        toggleComboBox.setSelectedIndex(2);
        toggleComboBox.setToolTipText("Toggles to generate");
        toggleComboBox.setMaximumSize(new java.awt.Dimension(50, 32767));

        protocolParamsTextField.setToolTipText("Additional parameters needed for some protocols.");
        protocolParamsTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });

        protocolsSubPane.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED), null));
        protocolsSubPane.setToolTipText("Generate and analyze IR Protocols");

        analyzePanel.setToolTipText("Analyze IR Protocol");

        IrpTextField.setEditable(false);
        IrpTextField.setToolTipText("IRP description of current protocol");
        IrpTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
        });

        protocolRawTextArea.setColumns(20);
        protocolRawTextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 14)); // NOI18N
        protocolRawTextArea.setLineWrap(true);
        protocolRawTextArea.setRows(5);
        protocolRawTextArea.setToolTipText("Pronto CCF code (or UEI learned). May be edited. Press right mouse button for menu.");
        protocolRawTextArea.setWrapStyleWord(true);
        protocolRawTextArea.setMinimumSize(new java.awt.Dimension(240, 17));
        protocolRawTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                protocolRawTextAreaMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                protocolRawTextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                protocolRawTextAreaMouseReleased(evt);
            }
        });
        jScrollPane3.setViewportView(protocolRawTextArea);

        jPanel6.setBorder(null);

        protocolGenerateButton.setMnemonic('R');
        protocolGenerateButton.setText("Render");
        protocolGenerateButton.setToolTipText("Compute Pronto code from upper row protocol description");
        protocolGenerateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolGenerateButtonActionPerformed(evt);
            }
        });

        protocolAnalyzeButton.setMnemonic('A');
        protocolAnalyzeButton.setText("Analyze");
        protocolAnalyzeButton.setToolTipText("Sends content of code windows to Analyze.");
        protocolAnalyzeButton.setEnabled(false);
        protocolAnalyzeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolAnalyzeButtonActionPerformed(evt);
            }
        });

        protocolPlotButton.setMnemonic('P');
        protocolPlotButton.setText("Plot");
        protocolPlotButton.setToolTipText("Graphical display of signal (ccf window or computed).");
        protocolPlotButton.setEnabled(false);
        protocolPlotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolPlotButtonActionPerformed(evt);
            }
        });

        protocolDecodeButton.setMnemonic('D');
        protocolDecodeButton.setText("Decode");
        protocolDecodeButton.setToolTipText("Send content of Code window(s) to DecodeIR");
        protocolDecodeButton.setEnabled(false);
        protocolDecodeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolDecodeButtonActionPerformed(evt);
            }
        });

        protocolImportButton.setText("Import...");
        protocolImportButton.setToolTipText("Import wave file, LIRC Mode2 file (space/pulse), or file from IR WIdget/IRScope");
        protocolImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolImportButtonActionPerformed(evt);
            }
        });

        protocolClearButton.setMnemonic('C');
        protocolClearButton.setText("Clear");
        protocolClearButton.setToolTipText("Clears code text areas");
        protocolClearButton.setEnabled(false);
        protocolClearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolClearButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocolGenerateButton)
            .addComponent(protocolImportButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolDecodeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolAnalyzeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolClearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {protocolAnalyzeButton, protocolClearButton, protocolDecodeButton, protocolGenerateButton, protocolImportButton, protocolPlotButton});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(protocolGenerateButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(protocolImportButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(protocolDecodeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(protocolAnalyzeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(protocolClearButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(protocolPlotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {protocolAnalyzeButton, protocolClearButton, protocolDecodeButton, protocolGenerateButton, protocolImportButton, protocolPlotButton});

        analyzeSendPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Send"));

        protocolSendButton.setMnemonic('S');
        protocolSendButton.setText("Send");
        protocolSendButton.setToolTipText("Send code in Code window, or if empty, render new signal and send it to selected output device.");
        protocolSendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolSendButtonActionPerformed(evt);
            }
        });

        protocolOutputhwComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC", "Audio" }));
        protocolOutputhwComboBox.setToolTipText("Device used for when sending");
        protocolOutputhwComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolOutputhwComboBoxActionPerformed(evt);
            }
        });

        protocolStopButton.setMnemonic('T');
        protocolStopButton.setText("Stop");
        protocolStopButton.setToolTipText("Stop ongoing IR transmission");
        protocolStopButton.setEnabled(false);
        protocolStopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolStopButtonActionPerformed(evt);
            }
        });

        noSendsProtocolComboBox.setMaximumRowCount(20);
        noSendsProtocolComboBox.setModel(noSendsSignalsComboBoxModel);
        noSendsProtocolComboBox.setToolTipText("Number of times to send IR signal");

        javax.swing.GroupLayout analyzeSendPanelLayout = new javax.swing.GroupLayout(analyzeSendPanel);
        analyzeSendPanel.setLayout(analyzeSendPanelLayout);
        analyzeSendPanelLayout.setHorizontalGroup(
            analyzeSendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocolStopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolSendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(noSendsProtocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(protocolOutputhwComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        analyzeSendPanelLayout.setVerticalGroup(
            analyzeSendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzeSendPanelLayout.createSequentialGroup()
                .addComponent(protocolOutputhwComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noSendsProtocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolSendButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolStopButton)
                .addContainerGap())
        );

        analyzeHelpButton.setMnemonic('H');
        analyzeHelpButton.setText("Help");
        analyzeHelpButton.setToolTipText("Display help text for current pane.");
        analyzeHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzeHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout analyzePanelLayout = new javax.swing.GroupLayout(analyzePanel);
        analyzePanel.setLayout(analyzePanelLayout);
        analyzePanelLayout.setHorizontalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(analyzeSendPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(analyzeHelpButton)))
            .addComponent(IrpTextField)
        );
        analyzePanelLayout.setVerticalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(IrpTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(analyzeSendPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(analyzeHelpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        protocolsSubPane.addTab("Analyze", null, analyzePanel, "Pane for generation and analysis of individual IR signals");

        protocolExportButton.setMnemonic('X');
        protocolExportButton.setText("Export");
        protocolExportButton.setToolTipText("Perform actual export.");
        protocolExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolExportButtonActionPerformed(evt);
            }
        });

        automaticFileNamesCheckBox.setMnemonic('A');
        automaticFileNamesCheckBox.setSelected(true);
        automaticFileNamesCheckBox.setText("Automatic File Names");
        automaticFileNamesCheckBox.setToolTipText("Perform the export to a file with automatically generated name. Otherwise, a file browser will be started.");

        exportGenerateTogglesCheckBox.setMnemonic('T');
        exportGenerateTogglesCheckBox.setText("Generate toggle pairs");
        exportGenerateTogglesCheckBox.setToolTipText("For protocol with toggles, generate both versions in the export file.");

        lastFTextField.setToolTipText("Last F to export (inclusive)");
        lastFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        lastFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        lastFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });

        jLabel17.setText("Last F");

        exportFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Text", "XML", "LIRC", "Wave", "Lintronic" }));
        exportFormatComboBox.setToolTipText("Type of export file");
        exportFormatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatComboBoxActionPerformed(evt);
            }
        });

        jLabel20.setText("Export Format");

        exportdirTextField.setMaximumSize(new java.awt.Dimension(300, 27));
        exportdirTextField.setMinimumSize(new java.awt.Dimension(300, 27));
        exportdirTextField.setPreferredSize(new java.awt.Dimension(300, 27));
        exportdirTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        exportdirTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdirTextFieldActionPerformed(evt);
            }
        });
        exportdirTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                exportdirTextFieldFocusLost(evt);
            }
        });

        exportdirBrowseButton.setText("...");
        exportdirBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdirBrowseButtonActionPerformed(evt);
            }
        });

        jLabel21.setText("Export dir.");

        exportRawCheckBox.setMnemonic('R');
        exportRawCheckBox.setText("Raw");
        exportRawCheckBox.setToolTipText("Generate raw codes (timing in microseconds) in export");

        exportProntoCheckBox.setMnemonic('P');
        exportProntoCheckBox.setSelected(true);
        exportProntoCheckBox.setText("Pronto");
        exportProntoCheckBox.setToolTipText("Generate Pronto (CCF) codes in export");

        viewExportButton.setMnemonic('O');
        viewExportButton.setText("Open Last File");
        viewExportButton.setToolTipText("Open last export file (if one exists).");
        viewExportButton.setEnabled(false);
        viewExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewExportButtonActionPerformed(evt);
            }
        });

        openExportDirButton.setMnemonic('O');
        openExportDirButton.setText("Open");
        openExportDirButton.setToolTipText("Shows export directory");
        openExportDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExportDirButtonActionPerformed(evt);
            }
        });

        exportRepetitionsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" }));
        exportRepetitionsComboBox.setSelectedIndex(1);
        exportRepetitionsComboBox.setToolTipText("The number of times the repetition should be included in export. For wave only.");

        exportNoRepetitionsLabel.setText("# Repetitions");

        exportHelpButton.setMnemonic('H');
        exportHelpButton.setText("Help");
        exportHelpButton.setToolTipText("Display help text for current pane.");
        exportHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportHelpButtonActionPerformed(evt);
            }
        });

        exportUeiLearnedCheckBox.setMnemonic('U');
        exportUeiLearnedCheckBox.setText("UEI Learned");
        exportUeiLearnedCheckBox.setToolTipText("Generate UEI learned format in export");

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(protocolExportButton)
                                .addGap(18, 18, 18)
                                .addComponent(viewExportButton))
                            .addComponent(automaticFileNamesCheckBox))
                        .addContainerGap())
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel17)
                            .addComponent(jLabel21))
                        .addGap(18, 18, 18)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(exportdirTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportdirBrowseButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openExportDirButton))
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addComponent(exportGenerateTogglesCheckBox)
                                        .addGap(78, 78, 78))
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addComponent(exportRawCheckBox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(exportProntoCheckBox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(exportUeiLearnedCheckBox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(exportNoRepetitionsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))))
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(exportHelpButton))
        );

        exportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {protocolExportButton, viewExportButton});

        exportPanelLayout.setVerticalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportGenerateTogglesCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportRawCheckBox)
                    .addComponent(exportProntoCheckBox)
                    .addComponent(exportNoRepetitionsLabel)
                    .addComponent(exportRepetitionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportUeiLearnedCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(exportdirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportdirBrowseButton)
                    .addComponent(openExportDirButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(automaticFileNamesCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protocolExportButton)
                    .addComponent(viewExportButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                .addComponent(exportHelpButton))
        );

        exportPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {protocolExportButton, viewExportButton});

        protocolsSubPane.addTab("Export", null, exportPanel, "Pane for exporting several signals into a file");

        warDialerPanel.setToolTipText("Pane for sending multiple IR signals to hardware.");

        jLabel32.setText(" ");

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(214, 213, 212));
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText("Warning: Sending undocumented IR commands to your equipment may damage or even destroy it. By using this program, you agree to take the responsibility for possible damages yourself, and not to hold the author responsible.");
        jTextArea1.setToolTipText("You have been warned!");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextArea1.setFocusable(false);
        jTextArea1.setMinimumSize(new java.awt.Dimension(10, 19));
        jScrollPane2.setViewportView(jTextArea1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Notes"));

        notesClearButton.setText("Clear");
        notesClearButton.setToolTipText("Not yet implemented.");
        notesClearButton.setEnabled(false);

        notesSaveButton.setText("Save");
        notesSaveButton.setToolTipText("Not yet implemented.");
        notesSaveButton.setEnabled(false);

        notesEditButton.setText("Edit");
        notesEditButton.setToolTipText("Not yet implemented.");
        notesEditButton.setEnabled(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(notesEditButton)
                    .addComponent(notesClearButton)
                    .addComponent(notesSaveButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {notesClearButton, notesEditButton, notesSaveButton});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(notesEditButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(notesClearButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(notesSaveButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {notesClearButton, notesEditButton, notesSaveButton});

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Run"));

        pauseButton.setMnemonic('P');
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause transmission, with possibility to resume. Not yet implemented.");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        currentFTextField.setEditable(false);
        currentFTextField.setBackground(new java.awt.Color(255, 254, 253));
        currentFTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        currentFTextField.setText("--");
        currentFTextField.setToolTipText("Value of F in the signal recently sent.");
        currentFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        currentFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        currentFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
        });

        startButton.setMnemonic('S');
        startButton.setText("Start");
        startButton.setToolTipText("Start sending sequence");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setMnemonic('T');
        stopButton.setText("Stop");
        stopButton.setToolTipText("Stop transmission");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pauseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(currentFTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 13, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {pauseButton, startButton, stopButton});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(startButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stopButton)
                .addGap(12, 12, 12)
                .addComponent(pauseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(currentFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {pauseButton, startButton, stopButton});

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));

        jLabel60.setText("# Sends");

        endFTextField.setToolTipText("Ending F to send");
        endFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        endFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        endFTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });

        jLabel27.setText("IR Device");

        delayTextField.setText("2");
        delayTextField.setToolTipText("Delay in seconds between different signals. Decimal number allowed.");
        delayTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        delayTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        delayTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });

        jLabel28.setText("Ending F");

        warDialerOutputhwComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC", "Audio" }));
        warDialerOutputhwComboBox.setToolTipText("Device to use for sending");
        warDialerOutputhwComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                warDialerOutputhwComboBoxActionPerformed(evt);
            }
        });

        jLabel29.setText("Delay (s)");

        warDialerNoSendsComboBox.setMaximumRowCount(20);
        warDialerNoSendsComboBox.setModel(noSendsSignalsComboBoxModel);
        warDialerNoSendsComboBox.setToolTipText("Number of times to send IR signal");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel28)
                    .addComponent(jLabel29)
                    .addComponent(jLabel60))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(warDialerOutputhwComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(warDialerNoSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {delayTextField, endFTextField, warDialerNoSendsComboBox, warDialerOutputhwComboBox});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(warDialerOutputhwComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(endFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(warDialerNoSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel60))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {delayTextField, endFTextField, warDialerNoSendsComboBox, warDialerOutputhwComboBox});

        warDialerHelpButton.setMnemonic('H');
        warDialerHelpButton.setText("Help");
        warDialerHelpButton.setToolTipText("Display help text for current pane.");
        warDialerHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                warDialerHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout warDialerPanelLayout = new javax.swing.GroupLayout(warDialerPanel);
        warDialerPanel.setLayout(warDialerPanelLayout);
        warDialerPanelLayout.setHorizontalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2))
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(106, 106, 106)
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(45, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, warDialerPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(warDialerHelpButton))))
        );
        warDialerPanelLayout.setVerticalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, warDialerPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(warDialerHelpButton))
        );

        if (uiFeatures.warDialerPane)
        protocolsSubPane.addTab("War Dialer", null, warDialerPanel, "Pane for sending multiple IR signals to hardware.");

        rendererComboBox.setMaximumRowCount(2);
        rendererComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "IrpMaster", "MakeHex" }));
        rendererComboBox.setToolTipText("Select IrpMaster (recommended) or MakeHex");
        rendererComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        rendererComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        rendererComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        rendererComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rendererComboBoxActionPerformed(evt);
            }
        });

        rendererLabel.setText("Renderer");

        jLabel3.setText("Protocol");

        deviceNumberLabel.setText("D");

        subDeviceNumberLabel.setText("S");

        functionNumberLabel.setText("F");

        toggleLabel.setText("T");

        additionalParametersLabel.setText("Additional Parameters");

        protocolDocButton.setText("Docu...");
        protocolDocButton.setToolTipText("Display (sometimes slightly cryptical) notes to the selected protocol.");
        protocolDocButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolDocButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout protocolsPanelLayout = new javax.swing.GroupLayout(protocolsPanel);
        protocolsPanel.setLayout(protocolsPanelLayout);
        protocolsPanelLayout.setHorizontalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rendererLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(deviceNumberLabel)
                        .addGap(46, 46, 46)
                        .addComponent(subDeviceNumberLabel)
                        .addGap(47, 47, 47)
                        .addComponent(functionNumberLabel))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(devicenoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(subdeviceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(commandnoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(toggleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(toggleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addComponent(protocolParamsTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocolDocButton))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addComponent(additionalParametersLabel)
                        .addGap(0, 100, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        protocolsPanelLayout.setVerticalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rendererLabel)
                    .addComponent(jLabel3)
                    .addComponent(deviceNumberLabel)
                    .addComponent(subDeviceNumberLabel)
                    .addComponent(functionNumberLabel)
                    .addComponent(toggleLabel)
                    .addComponent(additionalParametersLabel))
                .addGap(5, 5, 5)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(devicenoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subdeviceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandnoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toggleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolParamsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolDocButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(protocolsSubPane))
        );

        protocolsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {commandnoTextField, devicenoTextField, protocolComboBox, protocolParamsTextField, rendererComboBox, subdeviceTextField, toggleComboBox});

        mainTabbedPane.addTab("IR Protocols", null, protocolsPanel, "This pane deals with generating, sending, exporting, and analyzing of IR protocols.");

        outputHWTabbedPane.setToolTipText("This pane sets the properties of the output hardware.");

        globalcachePanel.setToolTipText("This pane sets up Global Caché hardware.");

        jLabel34.setText("Discovered GlobalCaché Type:");

        gcDiscoveredTypeLabel.setText("<unknown>");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        gcAddressTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gcAddressTextField.setText(Props.getInstance().getGlobalcacheIpName());
        gcAddressTextField.setToolTipText("IP-Address/Name of GlobalCache to use");
        gcAddressTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        gcAddressTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        gcAddressTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        gcAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcAddressTextFieldActionPerformed(evt);
            }
        });

        jLabel19.setText("IP Name/Address");

        jButton1.setMnemonic('T');
        jButton1.setText("Stop IR");
        jButton1.setToolTipText("Send the selected GlobalCache the stopir command.");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcStopIrActionPerformed(evt);
            }
        });

        discoverButton.setMnemonic('D');
        discoverButton.setText("Discover");
        discoverButton.setToolTipText("Try to discover a GlobalCache on LAN. Takes up to 60 seconds!");
        discoverButton.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        discoverButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discoverButtonActionPerformed(evt);
            }
        });

        jLabel35.setText("Module");

        gcBrowseButton.setMnemonic('B');
        gcBrowseButton.setText("Browse");
        gcBrowseButton.setToolTipText("Open selected GlobalCache in the browser.");
        gcBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcBrowseButtonActionPerformed(evt);
            }
        });

        jLabel36.setText("Port");

        gcConnectorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        gcConnectorComboBox.setToolTipText("GlobalCache IR Connector to use");
        gcConnectorComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));
        gcConnectorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcConnectorComboBoxActionPerformed(evt);
            }
        });

        globalCachePingButton.setMnemonic('P');
        globalCachePingButton.setText("Ping");
        globalCachePingButton.setToolTipText("Try to ping the device");
        globalCachePingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCachePingButtonActionPerformed(evt);
            }
        });

        gcModuleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "4", "5" }));
        gcModuleComboBox.setToolTipText("GlobalCache IR Module to use");
        gcModuleComboBox.setMaximumSize(new java.awt.Dimension(48, 28));
        gcModuleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcModuleComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(gcAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gcModuleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addGap(27, 27, 27)
                        .addComponent(jLabel35)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel36)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(gcConnectorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(globalCachePingButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(discoverButton))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(gcBrowseButton)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {gcConnectorComboBox, gcModuleComboBox});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {discoverButton, gcBrowseButton, globalCachePingButton, jButton1});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jLabel35)
                    .addComponent(jLabel36))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gcAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gcModuleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gcConnectorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(gcBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(globalCachePingButton)
                    .addComponent(discoverButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {gcAddressTextField, gcConnectorComboBox, gcModuleComboBox});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {discoverButton, gcBrowseButton, globalCachePingButton, jButton1});

        globalCacheHelpButton.setMnemonic('H');
        globalCacheHelpButton.setText("Help");
        globalCacheHelpButton.setToolTipText("Display help text for current pane.");
        globalCacheHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalcachePanelLayout = new javax.swing.GroupLayout(globalcachePanel);
        globalcachePanel.setLayout(globalcachePanelLayout);
        globalcachePanelLayout.setHorizontalGroup(
            globalcachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalcachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(globalcachePanelLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gcDiscoveredTypeLabel)))
                .addContainerGap(204, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, globalcachePanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(globalCacheHelpButton))
        );
        globalcachePanelLayout.setVerticalGroup(
            globalcachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(globalcachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(gcDiscoveredTypeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 159, Short.MAX_VALUE)
                .addComponent(globalCacheHelpButton))
        );

        outputHWTabbedPane.addTab("GlobalCaché", null, globalcachePanel, "This pane sets up Global Caché hardware.");

        irtransPanel.setToolTipText("This pane sets up IrTrans Ethernet connected hardware.");

        irtransVersionLabel.setText("<unknown>");

        jLabel18.setText("IrTrans Version:");

        irtransIPPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        irtransPingButton.setMnemonic('P');
        irtransPingButton.setText("Ping");
        irtransPingButton.setToolTipText("Try to ping the device");
        irtransPingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransPingButtonActionPerformed(evt);
            }
        });

        irtransAddressTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        irtransAddressTextField.setText(Props.getInstance().getIrTransIpName());
        irtransAddressTextField.setToolTipText("IP-Address/Name of IRTrans");
        irtransAddressTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        irtransAddressTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        irtransAddressTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        irtransAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransAddressTextFieldActionPerformed(evt);
            }
        });

        irtransBrowseButton.setMnemonic('B');
        irtransBrowseButton.setText("Browse");
        irtransBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransBrowseButtonActionPerformed(evt);
            }
        });

        jLabel38.setText("IR Port");

        irtransLedComboBox.setMaximumRowCount(12);
        irtransLedComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "intern", "extern", "both", "0", "1", "2", "3", "4", "5", "6", "7", "8" }));
        irtransLedComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransLedComboBoxActionPerformed(evt);
            }
        });

        readButton.setMnemonic('R');
        readButton.setText("Read");
        readButton.setToolTipText("Read version and predefined commands into memory");
        readButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readButtonActionPerformed(evt);
            }
        });

        jLabel37.setText("IP Name/Address");

        javax.swing.GroupLayout irtransIPPanelLayout = new javax.swing.GroupLayout(irtransIPPanel);
        irtransIPPanel.setLayout(irtransIPPanelLayout);
        irtransIPPanelLayout.setHorizontalGroup(
            irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel37)
                    .addComponent(irtransAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irtransIPPanelLayout.createSequentialGroup()
                        .addComponent(irtransLedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(readButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransPingButton))
                    .addComponent(jLabel38))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        irtransIPPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {irtransBrowseButton, irtransPingButton, readButton});

        irtransIPPanelLayout.setVerticalGroup(
            irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel37)
                    .addComponent(jLabel38))
                .addGap(2, 2, 2)
                .addGroup(irtransIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(irtransAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransLedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransBrowseButton)
                    .addComponent(irtransPingButton)
                    .addComponent(readButton))
                .addContainerGap(49, Short.MAX_VALUE))
        );

        irtransIPPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {irtransAddressTextField, irtransBrowseButton, irtransLedComboBox, irtransPingButton, readButton});

        irtransPredefinedPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel33.setText("Flashed Commands (IRDB)");

        jLabel52.setText("Remote");

        irtransCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        irtransCommandsComboBox.setToolTipText("Predefined Command");
        irtransCommandsComboBox.setEnabled(false);
        irtransCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        irtransCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        noSendsIrtransFlashedComboBox.setMaximumRowCount(20);
        noSendsIrtransFlashedComboBox.setModel(noSendsIrtransFlashedComboBoxModel);
        noSendsIrtransFlashedComboBox.setToolTipText("Number of times to send IR signal");

        jLabel53.setText("Command");

        irtransRemotesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        irtransRemotesComboBox.setToolTipText("Predefined Remote");
        irtransRemotesComboBox.setEnabled(false);
        irtransRemotesComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        irtransRemotesComboBox.setPreferredSize(new java.awt.Dimension(140, 28));
        irtransRemotesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransRemotesComboBoxActionPerformed(evt);
            }
        });

        jLabel51.setText("# Sends");

        irtransSendFlashedButton.setMnemonic('S');
        irtransSendFlashedButton.setText("Send");
        irtransSendFlashedButton.setToolTipText("Send selected command/remote from the IRTrans");
        irtransSendFlashedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransSendFlashedButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtransPredefinedPanelLayout = new javax.swing.GroupLayout(irtransPredefinedPanel);
        irtransPredefinedPanel.setLayout(irtransPredefinedPanelLayout);
        irtransPredefinedPanelLayout.setHorizontalGroup(
            irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel33)
                    .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                        .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(noSendsIrtransFlashedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(jLabel51)
                                .addGap(36, 36, 36)
                                .addComponent(jLabel52)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel53)
                            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(irtransSendFlashedButton)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        irtransPredefinedPanelLayout.setVerticalGroup(
            irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel33)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52)
                    .addComponent(jLabel53))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(irtransPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(noSendsIrtransFlashedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransSendFlashedButton))
                .addContainerGap(37, Short.MAX_VALUE))
        );

        irtransPredefinedPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {irtransCommandsComboBox, irtransRemotesComboBox, irtransSendFlashedButton, noSendsIrtransFlashedComboBox});

        irtransHelpButton.setMnemonic('H');
        irtransHelpButton.setText("Help");
        irtransHelpButton.setToolTipText("Display help text for current pane.");
        irtransHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtransHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtransPanelLayout = new javax.swing.GroupLayout(irtransPanel);
        irtransPanel.setLayout(irtransPanelLayout);
        irtransPanelLayout.setHorizontalGroup(
            irtransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irtransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irtransPanelLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(irtransVersionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(irtransHelpButton))
                    .addGroup(irtransPanelLayout.createSequentialGroup()
                        .addGroup(irtransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(irtransPredefinedPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(irtransIPPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 190, Short.MAX_VALUE))))
        );
        irtransPanelLayout.setVerticalGroup(
            irtransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtransIPPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(irtransPredefinedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(61, 61, 61)
                .addGroup(irtransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(irtransVersionLabel))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irtransPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(irtransHelpButton))
        );

        outputHWTabbedPane.addTab("IRTrans", null, irtransPanel, "This pane sets up IrTrans Ethernet connected hardware.");

        lircServerVersionText.setText("<unknown>");
        lircServerVersionText.setEnabled(false);

        lircServerVersionLabel.setText("Lirc Server Version:");
        lircServerVersionLabel.setEnabled(false);

        lircIPPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        lircIPPanel.setToolTipText("IP properties");

        jLabel45.setText("TCP Port");

        LircIPAddressTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        LircIPAddressTextField.setText(Props.getInstance().getLircIpName());
        LircIPAddressTextField.setToolTipText("IP-Address/Name of Lirc Server");
        LircIPAddressTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        LircIPAddressTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        LircIPAddressTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        LircIPAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LircIPAddressTextFieldActionPerformed(evt);
            }
        });

        readLircButton.setMnemonic('R');
        readLircButton.setText("Read");
        readLircButton.setToolTipText("Read version and preprogrammed commands into memory");
        readLircButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readLircButtonActionPerformed(evt);
            }
        });

        lircPingButton.setMnemonic('P');
        lircPingButton.setText("Ping");
        lircPingButton.setToolTipText("Try to ping device");
        lircPingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircPingButtonActionPerformed(evt);
            }
        });

        lircPortTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        lircPortTextField.setText(Props.getInstance().getLircPort());
        lircPortTextField.setToolTipText("Port number of LIRC server to use. Default is 8765.");
        lircPortTextField.setMinimumSize(new java.awt.Dimension(120, 27));
        lircPortTextField.setPreferredSize(new java.awt.Dimension(120, 27));
        lircPortTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyPasteMenu(evt);
            }
        });
        lircPortTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircPortTextFieldActionPerformed(evt);
            }
        });

        jLabel44.setText("IP Name/Address");

        lircTransmitterComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8" }));
        lircTransmitterComboBox.setSelectedIndex(lircTransmitterDefaultIndex);
        lircTransmitterComboBox.setToolTipText("IR Transmitter to use. Not implemented or sensible on many Lirc servers.");
        lircTransmitterComboBox.setEnabled(false);
        lircTransmitterComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));
        lircTransmitterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircTransmitterComboBoxActionPerformed(evt);
            }
        });

        jLabel46.setText("Transm.");

        lircStopIrButton.setMnemonic('T');
        lircStopIrButton.setText("Stop IR");
        lircStopIrButton.setToolTipText("Send the selected LIRC-server a stop command.");
        lircStopIrButton.setEnabled(false);
        lircStopIrButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircStopIrButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircIPPanelLayout = new javax.swing.GroupLayout(lircIPPanel);
        lircIPPanel.setLayout(lircIPPanelLayout);
        lircIPPanelLayout.setHorizontalGroup(
            lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel45)
                    .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lircIPPanelLayout.createSequentialGroup()
                        .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lircStopIrButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(readLircButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lircPingButton))
                    .addComponent(jLabel46))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lircIPPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lircPingButton, lircStopIrButton, readLircButton});

        lircIPPanelLayout.setVerticalGroup(
            lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircIPPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jLabel46)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircIPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lircPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircTransmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircStopIrButton)
                    .addComponent(readLircButton)
                    .addComponent(lircPingButton)
                    .addComponent(LircIPAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        lircIPPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {LircIPAddressTextField, lircPingButton, lircPortTextField, lircStopIrButton, lircTransmitterComboBox, readLircButton});

        lircPredefinedPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel47.setText("Predefined commands");

        lircCommandsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        lircCommandsComboBox.setToolTipText("Predefined Command");
        lircCommandsComboBox.setEnabled(false);
        lircCommandsComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        lircCommandsComboBox.setPreferredSize(new java.awt.Dimension(140, 28));

        jLabel49.setText("Remote");

        lircRemotesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--" }));
        lircRemotesComboBox.setToolTipText("Predefined Remote");
        lircRemotesComboBox.setEnabled(false);
        lircRemotesComboBox.setMinimumSize(new java.awt.Dimension(140, 28));
        lircRemotesComboBox.setPreferredSize(new java.awt.Dimension(140, 28));
        lircRemotesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircRemotesComboBoxActionPerformed(evt);
            }
        });

        jLabel50.setText("Command");

        lircSendPredefinedButton.setMnemonic('S');
        lircSendPredefinedButton.setText("Send");
        lircSendPredefinedButton.setToolTipText("Send selected command/remote from the LIRC server");
        lircSendPredefinedButton.setEnabled(false);
        lircSendPredefinedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircSendPredefinedButtonActionPerformed(evt);
            }
        });

        jLabel48.setText("# Sends");

        noLircPredefinedsComboBox.setMaximumRowCount(20);
        noLircPredefinedsComboBox.setModel(noSendsLircPredefinedsComboBoxModel);
        noLircPredefinedsComboBox.setToolTipText("Number of times to send IR signal");
        noLircPredefinedsComboBox.setEnabled(false);

        javax.swing.GroupLayout lircPredefinedPanelLayout = new javax.swing.GroupLayout(lircPredefinedPanel);
        lircPredefinedPanel.setLayout(lircPredefinedPanelLayout);
        lircPredefinedPanelLayout.setHorizontalGroup(
            lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel48)
                            .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel49))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lircSendPredefinedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                                .addComponent(jLabel50)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                        .addComponent(jLabel47)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        lircPredefinedPanelLayout.setVerticalGroup(
            lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPredefinedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel47)
                .addGap(18, 18, 18)
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(jLabel49)
                    .addComponent(jLabel50))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lircPredefinedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(noLircPredefinedsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircRemotesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircCommandsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lircSendPredefinedButton))
                .addContainerGap())
        );

        lircHelpButton.setMnemonic('H');
        lircHelpButton.setText("Help");
        lircHelpButton.setToolTipText("Display help text for current pane.");
        lircHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircPanelLayout = new javax.swing.GroupLayout(lircPanel);
        lircPanel.setLayout(lircPanelLayout);
        lircPanelLayout.setHorizontalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(lircPanelLayout.createSequentialGroup()
                        .addComponent(lircServerVersionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lircServerVersionText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(lircIPPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lircPredefinedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(92, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lircPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lircHelpButton))
        );
        lircPanelLayout.setVerticalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lircIPPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lircPredefinedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lircServerVersionLabel)
                    .addComponent(lircServerVersionText))
                .addGap(24, 24, 24)
                .addComponent(lircHelpButton))
        );

        outputHWTabbedPane.addTab("LIRC", lircPanel);

        audioPanel.setToolTipText("Parameters for Audio/wave creation");

        audioGetLineButton.setMnemonic('G');
        audioGetLineButton.setText("Get Line");
        audioGetLineButton.setToolTipText("Try to allocate an appropriate audio line to the system's audio mixer.");
        audioGetLineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioGetLineButtonActionPerformed(evt);
            }
        });

        audioReleaseLineButton.setMnemonic('R');
        audioReleaseLineButton.setText("Release Line");
        audioReleaseLineButton.setToolTipText("Release audio line");
        audioReleaseLineButton.setEnabled(false);
        audioReleaseLineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioReleaseLineButtonActionPerformed(evt);
            }
        });

        jLabel59.setText("The settings herein also take effect when generating wave-exports!");

        audioFormatPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        audioFormatPanel.setToolTipText("Parameters of the audio format are set here.");

        jLabel56.setText("Sample size");

        audioSampleSizeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "8", "16" }));
        audioSampleSizeComboBox.setToolTipText("Number of bits in one sample. Normally 8.");

        jLabel55.setText("Sample freq.");

        jLabel57.setText("Channels");

        audioSampleFrequencyComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "44100", "48000", "96000", "192000" }));
        audioSampleFrequencyComboBox.setSelectedIndex(1);
        audioSampleFrequencyComboBox.setToolTipText("The number of samples per second in generated data.");

        jLabel58.setText("Byte order");

        audioChannelsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2" }));
        audioChannelsComboBox.setToolTipText("Number of channels in generated data. Normal setting is 1. If 2 (\"stereo\") the second channel is in opposite phase to the first.");

        audioBigEndianCheckBox.setText("Big endian");
        audioBigEndianCheckBox.setToolTipText("Order of bytes in 16 bit samples. Normally unchecked.");

        javax.swing.GroupLayout audioFormatPanelLayout = new javax.swing.GroupLayout(audioFormatPanel);
        audioFormatPanel.setLayout(audioFormatPanelLayout);
        audioFormatPanelLayout.setHorizontalGroup(
            audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(audioFormatPanelLayout.createSequentialGroup()
                        .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel56)
                                    .addComponent(jLabel57))
                                .addGap(18, 18, 18)
                                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(audioSampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(audioChannelsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                                .addComponent(jLabel55)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(audioSampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(17, 17, Short.MAX_VALUE))
                    .addGroup(audioFormatPanelLayout.createSequentialGroup()
                        .addComponent(jLabel58)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(audioBigEndianCheckBox)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        audioFormatPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {audioChannelsComboBox, audioSampleFrequencyComboBox, audioSampleSizeComboBox});

        audioFormatPanelLayout.setVerticalGroup(
            audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioFormatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel55)
                    .addComponent(audioSampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel56)
                    .addComponent(audioSampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel57)
                    .addComponent(audioChannelsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(audioFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel58)
                    .addComponent(audioBigEndianCheckBox))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        audioFormatPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {audioChannelsComboBox, audioSampleFrequencyComboBox, audioSampleSizeComboBox});

        audioOptionsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        audioOptionsPanel.setToolTipText("Other parameters for generation of wave files");

        audioWaveformComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "square", "sine" }));
        audioWaveformComboBox.setToolTipText("Use sines or square waves in the generated data.");

        audioOmitCheckBox.setMnemonic('T');
        audioOmitCheckBox.setText("Omit trailing gap");
        audioOmitCheckBox.setToolTipText("If checked, do not generate data for the last period of silence.");

        audioDivideCheckBox.setSelected(true);
        audioDivideCheckBox.setText("Divide carrier");
        audioDivideCheckBox.setToolTipText("Divide carrier frequency by two, for the use of 20kHz sound equipment and a pair of IR LEDs in antiparallel. Normally selected.");

        jLabel30.setText("Wave form");

        javax.swing.GroupLayout audioOptionsPanelLayout = new javax.swing.GroupLayout(audioOptionsPanel);
        audioOptionsPanel.setLayout(audioOptionsPanelLayout);
        audioOptionsPanelLayout.setHorizontalGroup(
            audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(audioDivideCheckBox)
                    .addComponent(audioOmitCheckBox)
                    .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                        .addComponent(audioWaveformComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel30)))
                .addContainerGap())
        );
        audioOptionsPanelLayout.setVerticalGroup(
            audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(audioDivideCheckBox)
                .addGap(22, 22, 22)
                .addComponent(audioOmitCheckBox)
                .addGap(18, 18, 18)
                .addGroup(audioOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(audioWaveformComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        audioHelpButton.setMnemonic('H');
        audioHelpButton.setText("Help");
        audioHelpButton.setToolTipText("Display help text for current pane.");
        audioHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout audioPanelLayout = new javax.swing.GroupLayout(audioPanel);
        audioPanel.setLayout(audioPanelLayout);
        audioPanelLayout.setHorizontalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addComponent(audioFormatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(audioOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(audioPanelLayout.createSequentialGroup()
                        .addGap(68, 68, 68)
                        .addComponent(audioGetLineButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, audioPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(audioReleaseLineButton)))
                .addContainerGap())
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel59)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(audioHelpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        audioPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {audioGetLineButton, audioReleaseLineButton});

        audioPanelLayout.setVerticalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(audioPanelLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(audioGetLineButton)
                        .addGap(18, 18, 18)
                        .addComponent(audioReleaseLineButton))
                    .addComponent(audioFormatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(audioOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 110, Short.MAX_VALUE)
                .addGroup(audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, audioPanelLayout.createSequentialGroup()
                        .addComponent(jLabel59)
                        .addGap(24, 24, 24))
                    .addComponent(audioHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)))
        );

        outputHWTabbedPane.addTab("Audio", audioPanel);

        if (uiFeatures.outputPane)
        mainTabbedPane.addTab("Output HW", null, outputHWTabbedPane, "This pane sets the properties of the output hardware.");

        mainSplitPane.setTopComponent(mainTabbedPane);

        consoleTextArea.setEditable(false);
        consoleTextArea.setColumns(20);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setRows(5);
        consoleTextArea.setToolTipText("This is the console, where errors and messages go. Press right mouse button for menu.");
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                consoleTextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                consoleTextAreaMouseReleased(evt);
            }
        });
        consoleScrollPane.setViewportView(consoleTextArea);

        mainSplitPane.setBottomComponent(consoleScrollPane);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save properties");
        saveMenuItem.setToolTipText("Write the current values of the program's properties to disk");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.saveProperties)
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save properties as ...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.saveProperties)
        fileMenu.add(saveAsMenuItem);
        if (uiFeatures.saveProperties)
        fileMenu.add(jSeparator4);

        consoletextSaveMenuItem.setMnemonic('c');
        consoletextSaveMenuItem.setText("Save console text as...");
        consoletextSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletextSaveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(consoletextSaveMenuItem);
        fileMenu.add(jSeparator1);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.setToolTipText("Exists the program, saving the preferences.");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        copyConsoleToClipboardMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyConsoleToClipboardMenuItem.setText("Copy Console to clipboard");
        copyConsoleToClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyConsoleToClipboardMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyConsoleToClipboardMenuItem);

        clearConsoleMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.CTRL_MASK));
        clearConsoleMenuItem.setMnemonic('c');
        clearConsoleMenuItem.setText("Clear console");
        clearConsoleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearConsoleMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(clearConsoleMenuItem);

        menuBar.add(editMenu);

        optionsMenu.setMnemonic('O');
        optionsMenu.setText("Options");

        verboseCheckBoxMenuItem.setMnemonic('v');
        verboseCheckBoxMenuItem.setText("Verbose");
        verboseCheckBoxMenuItem.setToolTipText("Report actual command sent to devices");
        verboseCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verboseCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(verboseCheckBoxMenuItem);

        disregardRepeatMinsCheckBoxMenuItem.setMnemonic('R');
        disregardRepeatMinsCheckBoxMenuItem.setText("disregard repeat mins");
        disregardRepeatMinsCheckBoxMenuItem.setToolTipText("Affects the generation of IR signals, see the documentation of IrpMaster");
        disregardRepeatMinsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disregardRepeatMinsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.discardRepeatMins)
        optionsMenu.add(disregardRepeatMinsCheckBoxMenuItem);

        irProtocolDatabaseMenu.setMnemonic('P');
        irProtocolDatabaseMenu.setText("IR Protocol Database");
        irProtocolDatabaseMenu.setToolTipText("Select, inspect, edit the data base for IR protocols.");

        irpMasterDatabaseMenu.setText("IrpMaster");

        irpMasterDbEditMenuItem.setText("Edit...");
        irpMasterDbEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpMasterDbEditMenuItemActionPerformed(evt);
            }
        });
        irpMasterDatabaseMenu.add(irpMasterDbEditMenuItem);

        irpMasterDbSelectMenuItem.setText("Select...");
        irpMasterDbSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpMasterDbSelectMenuItemActionPerformed(evt);
            }
        });
        irpMasterDatabaseMenu.add(irpMasterDbSelectMenuItem);

        irProtocolDatabaseMenu.add(irpMasterDatabaseMenu);

        makehexDatabaseMenu.setText("MakeHex");

        makehexDbEditMenuItem.setText("Show dir...");
        makehexDbEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexDbEditMenuItemActionPerformed(evt);
            }
        });
        makehexDatabaseMenu.add(makehexDbEditMenuItem);

        makehexDbSelectMenuItem.setText("Select...");
        makehexDbSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexDbSelectMenuItemActionPerformed(evt);
            }
        });
        makehexDatabaseMenu.add(makehexDbSelectMenuItem);

        irProtocolDatabaseMenu.add(makehexDatabaseMenu);

        optionsMenu.add(irProtocolDatabaseMenu);
        optionsMenu.add(lafSeparator);

        usePopupsCheckBoxMenuItem.setSelected(Props.getInstance().getUsePopupsForErrors());
        usePopupsCheckBoxMenuItem.setText("use popups for errors etc.");
        usePopupsCheckBoxMenuItem.setToolTipText("If selected, error-, warning-, and information messages will be shown in (modal) popups (windows style). Otherwise they will go into the console.");
        usePopupsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usePopupsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(usePopupsCheckBoxMenuItem);

        popupsForHelpCheckBoxMenuItem.setText("use popups for help");
        popupsForHelpCheckBoxMenuItem.setToolTipText("Open popup windows for help texts instead of printing to the console.");
        popupsForHelpCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupsForHelpCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(popupsForHelpCheckBoxMenuItem);

        lafMenu.setMnemonic('L');
        lafMenu.setText("Look and Feel");
        lafMenu.setToolTipText("Select look and feel from alternatives");
        optionsMenu.add(lafMenu);
        optionsMenu.add(debugSeparator);

        debugMenu.setText("Debug");
        optionsMenu.add(debugMenu);

        menuBar.add(optionsMenu);

        toolsMenu.setMnemonic('T');
        toolsMenu.setText("Tools");
        toolsMenu.setToolTipText("Invoking tools");

        IrCalcMenuItem.setMnemonic('H');
        IrCalcMenuItem.setText("Hex Calculator...");
        IrCalcMenuItem.setToolTipText("Invoke a hex calculator  in separate window");
        IrCalcMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IrCalcMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(IrCalcMenuItem);

        FrequencyTimeCalcMenuItem.setMnemonic('T');
        FrequencyTimeCalcMenuItem.setText("Time/Frequency Calculator...");
        FrequencyTimeCalcMenuItem.setToolTipText("Invoke a Time/Frequency calculator in a separate window.");
        FrequencyTimeCalcMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FrequencyTimeCalcMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(FrequencyTimeCalcMenuItem);
        toolsMenu.add(jSeparator2);

        checkUpdatesMenuItem.setMnemonic('u');
        checkUpdatesMenuItem.setText("Check for updates");
        checkUpdatesMenuItem.setToolTipText("Checks if a newer version is available");
        checkUpdatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkUpdatesMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(checkUpdatesMenuItem);

        menuBar.add(toolsMenu);

        helpMenu.setMnemonic('P');
        helpMenu.setText("Help");

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
        aboutMenuItem.setToolTipText("The mandatory About popup (version, copyright, etc)");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        browseHomePageMenuItem.setMnemonic('h');
        browseHomePageMenuItem.setText("Project Homepage...");
        browseHomePageMenuItem.setToolTipText("Browse the project's home page");
        browseHomePageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseHomePageMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(browseHomePageMenuItem);

        contentMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        contentMenuItem.setMnemonic('M');
        contentMenuItem.setText("Main documentation");
        contentMenuItem.setToolTipText("Brings up documentation.");
        contentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentMenuItem);

        browseIRPMasterMenuItem.setMnemonic('D');
        browseIRPMasterMenuItem.setText("IRPMaster doc");
        browseIRPMasterMenuItem.setToolTipText("Brings up documentation for IRPMaster, the main rendering engine");
        browseIRPMasterMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIRPMasterMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.lotsOfDocumentation)
        helpMenu.add(browseIRPMasterMenuItem);
        helpMenu.add(jSeparator13);

        browseIRPSpecMenuItem.setMnemonic('I');
        browseIRPSpecMenuItem.setText("IRP Notation Specs");
        browseIRPSpecMenuItem.setToolTipText("Displays the specification of the IRP notation");
        browseIRPSpecMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIRPSpecMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.saveProperties)
        helpMenu.add(browseIRPSpecMenuItem);

        browseDecodeIRMenuItem.setMnemonic('P');
        browseDecodeIRMenuItem.setText("Protocol specs");
        browseDecodeIRMenuItem.setToolTipText("Displays \"Decodeir.html\", containing a description of the protocols");
        browseDecodeIRMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDecodeIRMenuItemActionPerformed(evt);
            }
        });
        if (uiFeatures.saveProperties)
        helpMenu.add(browseDecodeIRMenuItem);

        browseJP1Wiki.setMnemonic('J');
        browseJP1Wiki.setText("JP1 Wiki");
        browseJP1Wiki.setToolTipText("JP1 Wiki: Some important and interesting background info");
        browseJP1Wiki.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseJP1WikiActionPerformed(evt);
            }
        });
        helpMenu.add(browseJP1Wiki);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 703, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class GlobalcacheThread extends Thread {
        private IrSignal code;
        private int module;
        private int connector;
        private int count;
        private JButton startButton;
        private JButton stopButton;

        public GlobalcacheThread(IrSignal code, int module, int connector, int count,
                JButton startButton, JButton stopButton) {
            super("globalcacheThread");
            this.code = code;
            this.module = module;
            this.connector = connector;
            this.count = count;
            this.startButton = startButton;
            this.stopButton = stopButton;
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            boolean success = false;
            try {
                success = gc.sendIr(code, count, module, connector);
            } catch (HarcHardwareException ex) {
                error(ex);
                success = false;
            }

            if (!success)
                error("GlobalCache failed");

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            globalcacheProtocolThread = null;
        }
    }

    private class IrtransThread extends Thread {
        private IrSignal code;
        private IrTrans.Led led;
        private int count;
        private JButton startButton;
        private JButton stopButton;

        public IrtransThread(IrSignal code, IrTrans.Led led, int count,
                JButton startButton, JButton stopButton) {
            super("irtransThread");
            this.code = code;
            this.led = led;
            this.count = count;
            this.startButton = startButton;
            this.stopButton = stopButton;
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            boolean success = false;
            try {

                success = irt.sendIr(code, count, led);
            } catch (IncompatibleArgumentException ex) {
                error(ex);
            } catch (HarcHardwareException ex) {
                error(ex);
            }

            if (!success)
                System.err.println("** Failed **");

            irtransThread = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void doExit() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        releaseAudioLine();
        Props.getInstance().setBounds(getBounds());
        Props.getInstance().setHardwareIndex(Integer.toString(hardwareIndex));
        System.err.println("Exiting...");
        System.exit(0);
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        doExit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            String result = Props.getInstance().save();
            info(result == null ? "No need to save properties." : ("Property file written to " + result + "."));
        } catch (Exception e) {
            error("Problems saving properties: " + e.getMessage());
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            aboutBox = new AboutPopup(this, false);
            aboutBox.setLocationRelativeTo(this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentMenuItemActionPerformed
        browse(Props.getInstance().getHelpfileUrl(), verbose);
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = selectFile("Select properties save", true, null, "xml", "XML Files").getAbsolutePath();
            Props.getInstance().save(props);
            info("Property file written to " + props + ".");
        } catch (IOException e) {
            error(e);
        //} catch (NullPointerException e) {
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void updateVerbosity() {
        UserPrefs.getInstance().setVerbose(verbose);
        if (gc != null)
            gc.setVerbosity(verbose);
        if (irt != null)
            irt.setVerbosity(verbose);
        if (lircClient != null)
            lircClient.setVerbosity(verbose);
        verboseCheckBoxMenuItem.setSelected(verbose);
    }

    private void verboseCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verboseCheckBoxMenuItemActionPerformed
        verbose = verboseCheckBoxMenuItem.isSelected();
        updateVerbosity();
    }//GEN-LAST:event_verboseCheckBoxMenuItemActionPerformed


    private void copyConsoleToClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyConsoleToClipboardMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getText());
    }//GEN-LAST:event_copyConsoleToClipboardMenuItemActionPerformed

    private void clearConsoleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearConsoleMenuItemActionPerformed
        consoleTextArea.setText(null);
    }//GEN-LAST:event_clearConsoleMenuItemActionPerformed

    private File getMakehexIrpFile() {
        String protocolName = (String) protocolComboBox.getModel().getSelectedItem();
        return new File(Props.getInstance().getMakehexIrpdir(), protocolName + "." + IrpFileExtension);
    }

    private String renderMakehexCode(int FOverride) {
        Makehex makehex = new Makehex(getMakehexIrpFile());
        ToggleType toggle = ToggleType.parse((String) toggleComboBox.getModel().getSelectedItem());
        int tog = ToggleType.toInt(toggle);
        int devno = devicenoTextField.getText().trim().isEmpty() ? (int) invalidParameter : (int) IrpUtils.parseLong(devicenoTextField.getText());
        int subDevno = subdeviceTextField.getText().trim().isEmpty() ? (int) invalidParameter : (int) IrpUtils.parseLong(subdeviceTextField.getText());
        int cmdNo = FOverride >= 0 ? FOverride : (int) IrpUtils.parseLong(commandnoTextField.getText());

        return makehex.prontoString(devno, subDevno, cmdNo, tog);
    }

    private IrSignal extractCode() throws NumberFormatException, IrpMasterException, RecognitionException {
        return extractCode((int) invalidParameter);
    }

    private IrSignal extractCode(int FOverride) throws NumberFormatException, IrpMasterException, RecognitionException {
        if (makehexRenderer()) {
            return Pronto.ccfSignal(renderMakehexCode(FOverride));
        } else {
            String protocolName = (String) protocolComboBox.getModel().getSelectedItem();
            long devno = devicenoTextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(devicenoTextField.getText());
            long subDevno = invalidParameter;
            Protocol protocol = getProtocol(protocolName);
            if (protocol.hasParameter("S") && !(protocol.hasParameterDefault("S") && subdeviceTextField.getText().trim().equals("")))
                subDevno = IrpUtils.parseLong(subdeviceTextField.getText());
            long cmdNo = FOverride >= 0 ? (long) FOverride : IrpUtils.parseLong(commandnoTextField.getText());
            String tog = (String) toggleComboBox.getModel().getSelectedItem();
            ToggleType toggle = ToggleType.parse((String) toggleComboBox.getModel().getSelectedItem());
            String addParams = protocolParamsTextField.getText();

            if (protocol == null)
                return null;

            HashMap<String, Long> params = new HashMap<String, Long>();
            if (devno != invalidParameter)
                params.put("D", devno);
            if (subDevno != invalidParameter)
                params.put("S", subDevno);
            if (cmdNo != invalidParameter)
                params.put("F", cmdNo);
            if (toggle != ToggleType.dontCare)
                params.put("T", (long) ToggleType.toInt(toggle));
            if (addParams != null && !addParams.trim().isEmpty()) {
                String[] str = addParams.trim().split("[ \t]+");
                for (String s : str) {
                    String[] q = s.split("=");
                    if (q.length == 2)
                        params.put(q[0], IrpUtils.parseLong(q[1]));
                }
            }
            IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
            codeNotationString = protocolName + ": " + protocol.notationString("=", " "); // Not really too nice :-(
            return irSignal;
        }
    }

    // before calling this function, check that lircExport != null || doRaw || doPronto || doUeiLearned.
    private void exportIrSignal(PrintStream printStream, Protocol protocol, HashMap<String, Long> params,
            boolean doXML, boolean doRaw, boolean doPronto, boolean doUeiLearned, LircExport lircExport)
            throws IrpMasterException {
        IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
        if (lircExport != null) {
            lircExport.addSignal(params, irSignal);
        } else {
            if (doXML)
                protocol.addSignal(params);
            boolean headerWritten = false;
            if (doRaw && irSignal != null) {
                if (doXML) {
                    protocol.addRawSignalRepresentation(irSignal);
                } else {
                    printStream.println(IrpUtils.variableHeader(params));
                    headerWritten = true;
                    printStream.println(irSignal.toPrintString());
                }
            }
            if (doPronto && irSignal != null) {
                if (doXML) {
                    protocol.addXmlNode("pronto", irSignal.ccfString());
                } else {
                    if (!headerWritten)
                        printStream.println(IrpUtils.variableHeader(params));
                    headerWritten = true;
                    printStream.println(irSignal.ccfString());
                }
            }
            if (doUeiLearned && irSignal != null) {
                if (doXML) {
                    protocol.addXmlNode("uei-learned", ExchangeIR.newUeiLearned(irSignal).toString());
                } else {
                    if (!headerWritten)
                        printStream.println(IrpUtils.variableHeader(params));
                    headerWritten = true;
                    printStream.println(ExchangeIR.newUeiLearned(irSignal).toString());
                }
            }
            if (!doXML)
                printStream.println();
        }
    }
    
    // FIXME: this code sucks.
    private boolean export() throws NumberFormatException, IrpMasterException, RecognitionException, FileNotFoundException {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean doXML = format.equalsIgnoreCase("XML");
        boolean doText = format.equalsIgnoreCase("text");
        boolean doLirc = format.equalsIgnoreCase("lirc");
        boolean doWave = format.equalsIgnoreCase("wave");
        boolean doLintronic = format.equalsIgnoreCase("lintronic");
        boolean doRaw = exportRawCheckBox.isSelected();
        boolean doPronto = exportProntoCheckBox.isSelected();
        boolean doUeiLearned = exportUeiLearnedCheckBox.isSelected();
        String protocolName = (String) protocolComboBox.getModel().getSelectedItem();
        long devno = devicenoTextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(devicenoTextField.getText());
        long subDevno = invalidParameter;
        if (!subdeviceTextField.getText().trim().equals(""))
            subDevno = IrpUtils.parseLong(subdeviceTextField.getText());
        long cmdNoLower = devicenoTextField.getText().trim().isEmpty() ? invalidParameter : IrpUtils.parseLong(commandnoTextField.getText());
        long cmdNoUpper = (doWave || lastFTextField.getText().isEmpty()) ? cmdNoLower : IrpUtils.parseLong(lastFTextField.getText());
        ToggleType toggle = ToggleType.parse((String) toggleComboBox.getModel().getSelectedItem());
        String addParams = protocolParamsTextField.getText();
        String extension = doXML ? "xml"
                : doLirc  ? "lirc"
                : doWave  ? "wav"
                : "txt";
        String formatDescription = "Export files";

        if ((doXML || doText) && ! (doRaw || doPronto || doUeiLearned)) {
            error("If selecting Text or XML export, at least one of Raw, Pronto, and UEI Learned must be selected.");
            return false;
        }

        if (automaticFileNamesCheckBox.isSelected()) {
            File exp = new File(Props.getInstance().getExportdir());
            if (!exp.exists()) {
                boolean success = exp.mkdirs();
                if (success)
                    info("Export directory " + exp + " does not exist, created.");
                else {
                    error("Export directory " + exp + " does not exist, attempt to create failed.");
                    return false;
                }
            }
            if (!exp.isDirectory() || !exp.canWrite()) {
                error("Export directory `" + exp + "' is not a writable directory, please correct.");
                return false;
            }
        }

        boolean useCcf = devno == invalidParameter
                && subDevno == invalidParameter
                && cmdNoLower == invalidParameter;
        File file = automaticFileNamesCheckBox.isSelected()
                ? createExportFile(Props.getInstance().getExportdir(),
                    useCcf
                    ? "rawccf"
                    : (protocolName + "_" + devno + (subDevno != invalidParameter ? ("_" + subDevno) : "")
                       + (doWave ? ("_" + cmdNoLower) : "")),
                  extension)
                : selectFile("Select export file", true, Props.getInstance().getExportdir(), extension, formatDescription);

        if (file == null) // user pressed cancel
            return false;

        if (useCcf) {
            IrSignal irSignal = ExchangeIR.interpretString(protocolRawTextArea.getText()); // may throw exceptions, caught by the caller
            int repetitions = Integer.parseInt((String) exportRepetitionsComboBox.getSelectedItem());
            ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(repetitions);
            System.err.println("Exporting raw CCF signal to " + file + ".");
            if (doWave) {
                updateAudioFormat();
                Wave wave = new Wave(irSequence, audioFormat,
                        audioOmitCheckBox.isSelected(),
                        audioWaveformComboBox.getSelectedIndex() == 0, audioDivideCheckBox.isSelected());
                wave.export(file);
            } else if (doLintronic) {
                PrintStream printStream = new PrintStream(file);
                printStream.print(Lintronic.toExport(irSequence));
                printStream.close();
            } else {
                System.err.println("Error: Parameters (D, S, F,...) are missing, and not using wave/Lintronic export.");
                return false;
            }
        } else if (irpmasterRenderer()) {
            Protocol protocol = irpMaster.newProtocol(protocolName);
            HashMap<String, Long> params = Protocol.parseParams((int) devno, (int) subDevno,
                    (int) cmdNoLower, ToggleType.toInt(toggle), addParams);
            if (doWave || doLintronic) {
                int repetitions = Integer.parseInt((String) exportRepetitionsComboBox.getSelectedItem());
                ToggleType tt = ToggleType.parse((String) toggleComboBox.getSelectedItem());
                if (tt != ToggleType.dontCare)
                    params.put("T", (long) ToggleType.toInt(tt));
                IrSignal irSignal = protocol.renderIrSignal(params, !Props.getInstance().getDisregardRepeatMins());
                ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(repetitions);
                if (doWave) {
                    updateAudioFormat();
                    Wave wave = new Wave(irSequence, audioFormat, audioOmitCheckBox.isSelected(),
                        audioWaveformComboBox.getSelectedIndex() == 0, audioDivideCheckBox.isSelected());
                    wave.export(file);
                } else {
                    // doLintronic
                    PrintStream printStream = new PrintStream(file);
                    printStream.print(Lintronic.toExport(irSequence));
                    printStream.close();
                }
                System.err.println("Exporting to " + file + ".");
            } else {
                LircExport lircExport = null;
                if (doXML)
                    protocol.setupDOM();
                if (doLirc)
                    lircExport = new LircExport(protocolName, "Generated by IrMaster", protocol.getFrequency());
                
                PrintStream printStream = new PrintStream(file);
                System.err.println("Exporting to " + file);
            
                    for (long cmdNo = cmdNoLower; cmdNo <= cmdNoUpper; cmdNo++) {
                    params.put("F", cmdNo);
                    if (exportGenerateTogglesCheckBox.isSelected()) {
                        for (long t = 0; t <= 1L; t++) {
                            params.put("T", t);
                            exportIrSignal(printStream, protocol, params, doXML, doRaw, doPronto, doUeiLearned, lircExport);
                        }
                    } else {
                        ToggleType tt = ToggleType.parse((String) toggleComboBox.getSelectedItem());
                        if (tt != ToggleType.dontCare)
                            params.put("T", (long) ToggleType.toInt(tt));
                        exportIrSignal(printStream, protocol, params, doXML, doRaw, doPronto, doUeiLearned, lircExport);
                    }
                }
                if (doXML)
                    protocol.printDOM(printStream);
                if (doLirc)
                    lircExport.write(printStream);
            }
        } else {
            // Makehex
            if (!doText || doRaw || doUeiLearned || doLirc) {
                error("Using Makehex only export in text files using Pronto format is supported");
            } else {
                PrintStream printStream = new PrintStream(file);
                info("Exporting to " + file);
                String selectedProtocolName = (String) protocolComboBox.getModel().getSelectedItem();
                Makehex makehex = new Makehex(new File(Props.getInstance().getMakehexIrpdir(), selectedProtocolName + "." + IrpFileExtension));
                for (int cmdNo = (int) cmdNoLower; cmdNo <= cmdNoUpper; cmdNo++) {
                    String ccf = makehex.prontoString((int)devno, (int)subDevno, (int)cmdNo, ToggleType.toInt(toggle));
                    printStream.println("Device Code: " + devno + (subDevno != invalidParameter ? ("." + subDevno) : "") + ", Function: " + cmdNo);
                    printStream.println(ccf);
                }
                printStream.close();
            }
        }
        
        lastExportFile = file.getAbsoluteFile();
        viewExportButton.setEnabled(true);
        return true;
    }
    
    private File createExportFile(String dir, String base, String extension) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return new File(dir, base + "_" + dateFormat.format(new Date()) + "." + extension);
    }
 
    private void updateProtocolParameters(boolean forceInitialize) {
        String currentProtocol = (String) protocolComboBox.getSelectedItem();
        boolean initialize = forceInitialize || ! Props.getInstance().getProtocol().equalsIgnoreCase(currentProtocol);
        Props.getInstance().setProtocol(currentProtocol.toLowerCase());
        if (irpmasterRenderer()) {
            if (irpMaster == null)
                return;
            try {
                Protocol protocol = getProtocol((String) protocolComboBox.getModel().getSelectedItem());
                devicenoTextField.setEnabled(protocol.hasParameter("D"));
                deviceNumberLabel.setEnabled(protocol.hasParameter("D"));
                subdeviceTextField.setEnabled(protocol.hasParameter("S"));
                subDeviceNumberLabel.setEnabled(protocol.hasParameter("S"));
                commandnoTextField.setEnabled(protocol.hasParameter("F"));
                functionNumberLabel.setEnabled(protocol.hasParameter("F"));
                toggleComboBox.setEnabled(protocol.hasParameter("T"));
                toggleLabel.setEnabled(protocol.hasParameter("T"));
                protocolParamsTextField.setEnabled(protocol.hasAdvancedParameters());
                additionalParametersLabel.setEnabled(protocol.hasAdvancedParameters());
                if (initialize) {
                    devicenoTextField.setText(null);
                    commandnoTextField.setText(null);
                    subdeviceTextField.setText(null);
                    toggleComboBox.setSelectedItem(ToggleType.dontCare);

                    if (protocol.hasParameter("D"))
                        devicenoTextField.setText(Long.toString(protocol.getParameterMin("D")));
                    
                    if (protocol.hasParameter("S") && !protocol.hasParameterDefault("S"))
                        subdeviceTextField.setText(Long.toString(protocol.getParameterMin("S")));
                    else
                        subdeviceTextField.setText(null);
                    if (protocol.hasParameter("F")) {
                        commandnoTextField.setText(Long.toString(protocol.getParameterMin("F")));
                        endFTextField.setText(Long.toString(protocol.getParameterMax("F")));
                        lastFTextField.setText(Long.toString(protocol.getParameterMax("F")));
                    }
                    protocolRawTextArea.setText(null);
                }
                exportGenerateTogglesCheckBox.setEnabled(protocol.hasParameter("T"));
                IrpTextField.setText(uiFeatures.irpLine ? protocol.getIrp() : null);
            } catch (UnassignedException ex) {
                subdeviceTextField.setEnabled(false);
                toggleComboBox.setEnabled(false);
            } catch (RecognitionException ex) {
                subdeviceTextField.setEnabled(false);
                toggleComboBox.setEnabled(false);
            }
        } else {
            // Makehex
            devicenoTextField.setEnabled(true);
            subdeviceTextField.setEnabled(true);
            commandnoTextField.setEnabled(true);
            toggleComboBox.setEnabled(true);
            if (initialize) {
                devicenoTextField.setText("0");
                subdeviceTextField.setText("0");
                commandnoTextField.setText("0");
                toggleComboBox.setSelectedIndex(2);
            }
            IrpTextField.setText(null);
            protocolParamsTextField.setEnabled(false);
            exportGenerateTogglesCheckBox.setEnabled(false);
            exportGenerateTogglesCheckBox.setSelected(false);
        }
    }

    private void consoletextSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoletextSaveMenuItemActionPerformed
        try {
            String filename = selectFile("Save console text as...", true, null, "txt", "Text file").getAbsolutePath();
            PrintStream ps = new PrintStream(new FileOutputStream(filename));
            ps.println(consoleTextArea.getText());
        } catch (FileNotFoundException ex) {
            error(ex);
        //} catch (NullPointerException e) {
        }
    }//GEN-LAST:event_consoletextSaveMenuItemActionPerformed

    private void exportdirBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdirBrowseButtonActionPerformed

        try {
	    String dir = selectFile("Select export directory", false, (new File(Props.getInstance().getExportdir())).getAbsoluteFile().getParent(), null, "Directories").getAbsolutePath();
	    Props.getInstance().setExportdir(dir);
	    exportdirTextField.setText(dir);
	} catch (NullPointerException e) {
	}
    }//GEN-LAST:event_exportdirBrowseButtonActionPerformed

    private void exportdirTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exportdirTextFieldFocusLost
        Props.getInstance().setExportdir(exportdirTextField.getText());
     }//GEN-LAST:event_exportdirTextFieldFocusLost

    private void exportdirTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdirTextFieldActionPerformed
        Props.getInstance().setExportdir(exportdirTextField.getText());
     }//GEN-LAST:event_exportdirTextFieldActionPerformed

    private void lircPortTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircPortTextFieldActionPerformed
        Props.getInstance().setLircPort(lircPortTextField.getText());
        LircIPAddressTextFieldActionPerformed(evt);
    }//GEN-LAST:event_lircPortTextFieldActionPerformed

    private void lircStopIrButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircStopIrButtonActionPerformed
        try {
            lircClient.stopIr(lircSelectedTransmitter);
        } catch (HarcHardwareException ex) {
            error(ex);
        }
	}//GEN-LAST:event_lircStopIrButtonActionPerformed

        private void LircIPAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LircIPAddressTextFieldActionPerformed
            String lircIp = LircIPAddressTextField.getText();
            Props.getInstance().setLircIpName(lircIp);
            lircClient = new LircCcfClient(lircIp, verbose, Integer.parseInt(lircPortTextField.getText()));
            try {
                lircServerVersionText.setText(lircClient.getVersion());
                String[] remotes = lircClient.getRemotes().toArray(new String[0]);
                Arrays.sort(remotes, String.CASE_INSENSITIVE_ORDER);
                lircRemotesComboBox.setModel(new DefaultComboBoxModel(remotes));
                lircRemotesComboBox.setEnabled(true);
                lircRemotesComboBoxActionPerformed(null);
                lircTransmitterComboBox.setEnabled(true);
                lircSendPredefinedButton.setEnabled(true);
                lircStopIrButton.setEnabled(true);
                noLircPredefinedsComboBox.setEnabled(true);
                lircServerVersionLabel.setEnabled(true);
                lircServerVersionText.setEnabled(true);
            } catch (HarcHardwareException ex) {
                error(ex.getMessage());
            }
	}//GEN-LAST:event_LircIPAddressTextFieldActionPerformed

    private void irtransBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransBrowseButtonActionPerformed
        browse(URI.create("http://" + irtransAddressTextField.getText()), verbose);
     }//GEN-LAST:event_irtransBrowseButtonActionPerformed

    private void irtransAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransAddressTextFieldActionPerformed
        Props.getInstance().setIrTransIpName(irtransAddressTextField.getText());
        irt = new IrTransIRDB(irtransAddressTextField.getText(), verbose);
        try {
            irtransVersionLabel.setText(irt.getVersion());
            String[] remotes = irt.getRemotes().toArray(new String[0]);
            Arrays.sort(remotes, String.CASE_INSENSITIVE_ORDER);
            irtransRemotesComboBox.setModel(new DefaultComboBoxModel(remotes));
            irtransRemotesComboBox.setEnabled(true);
            irtransRemotesComboBoxActionPerformed(null);
        } catch (HarcHardwareException ex) {
            error(ex.getMessage());
        }
     }//GEN-LAST:event_irtransAddressTextFieldActionPerformed

    private class GlobalcacheDiscoverThread extends Thread {
        public GlobalcacheDiscoverThread() {
        }

        @Override
        public void run() {
            //discoverButton.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            discoverButton.setEnabled(false);
            AmxBeaconListener.Result beacon = GlobalCache.listenBeacon();
            if (beacon != null) {
                String gcHostname = beacon.addr.getCanonicalHostName();
                gcAddressTextField.setText(gcHostname);
                gcDiscoveredTypeLabel.setText(beacon.table.get("-Model"));
                info("A GlobalCaché " +  beacon.table.get("-Model") + " was found at " + gcHostname);
                gcAddressTextFieldActionPerformed(null);
            } else
                warning("No GlobalCache was found.");

            discoverButton.setEnabled(true);
        }
    }
    
    private void updateGlobalCache(boolean force) {
        try {
            if (gc == null || force)
                gc = new GlobalCache(gcAddressTextField.getText(), verboseCheckBoxMenuItem.getState());
	    gcModuleComboBox.setEnabled(false);
	    gcConnectorComboBox.setEnabled(false);
            ArrayList<Integer> modules = gc.getIrModules();
            String[] modulesStrings;
            if (modules.isEmpty())
                modulesStrings = new String[]{"-"};
            else {
                modulesStrings = new String[modules.size()];
                for (int i = 0; i < modules.size(); i++)
                    modulesStrings[i] = Integer.toString(modules.get(i));
            }
            gcModuleComboBox.setModel(new DefaultComboBoxModel(modulesStrings));
	    gcModuleComboBox.setEnabled(modulesStrings.length > 0);
	    gcConnectorComboBox.setEnabled(modulesStrings.length > 0);
	} catch (HarcHardwareException e) {
	    gc = null;
	    error(e.getMessage());
	}
	protocolSendButton.setEnabled(gc != null);
    }

    private void discoverButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discoverButtonActionPerformed
        info("Now trying to discover a GlobalCache on LAN. This may take up to 60 seconds.");
	GlobalcacheDiscoverThread thread = new GlobalcacheDiscoverThread();
        thread.start();
    }//GEN-LAST:event_discoverButtonActionPerformed

    private void gcStopIrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcStopIrActionPerformed
        try {
	    gc.stopIr(getGcModule(), getGcConnector());
	} catch (HarcHardwareException ex) {
	    error(ex);
	}
    }//GEN-LAST:event_gcStopIrActionPerformed

    private void gcBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcBrowseButtonActionPerformed
        browse(URI.create("http://" + gcAddressTextField.getText()), verbose);
    }//GEN-LAST:event_gcBrowseButtonActionPerformed

    private void gcAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcAddressTextFieldActionPerformed
        Props.getInstance().setGlobalcacheIpName(gcAddressTextField.getText());
        updateGlobalCache(true);
    }//GEN-LAST:event_gcAddressTextFieldActionPerformed

    private void protocolExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolExportButtonActionPerformed
        try {
	    export();
	} catch (NumberFormatException ex) {
	    error(ex);
	} catch (IrpMasterException ex) {
	    error(ex);
	} catch (RecognitionException ex) {
	    error(ex);
	} catch (FileNotFoundException ex) {
	    error(ex);
	}
    }//GEN-LAST:event_protocolExportButtonActionPerformed

    private void protocolImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolImportButtonActionPerformed
     File file = selectFile("Select import file", false, null,
                new String[]{"ict", "ict Files"},
                new String[]{"txt", "Text Files"},
                new String[]{"wav", "Wave Files"});
	if (file != null) {
	    try {
		if (verbose)
                    trace("Imported " + file.getName());

                IrSignal ip;
                if (file.getName().endsWith(".wav")) {
                    Wave wave = new Wave(file);
                    ip = wave.analyze(this.audioDivideCheckBox.isSelected());
                } else
                    ip = ICT.parse(file);

		protocolRawTextArea.setText(ip.ccfString());
                enableProtocolButtons(true);
	    } catch (UnsupportedAudioFileException ex) {
		error(ex);
            } catch (IncompatibleArgumentException ex) {
		error(ex);
	    } catch (FileNotFoundException ex) {
		error(ex);
	    } catch (IOException ex) {
		error(ex);
	    }
	}
    }//GEN-LAST:event_protocolImportButtonActionPerformed

    private void protocolClearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolClearButtonActionPerformed
        protocolRawTextArea.setText(null);
        enableProtocolButtons(false);
    }//GEN-LAST:event_protocolClearButtonActionPerformed

    private void protocolStopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolStopButtonActionPerformed
        try {
	    if (globalcacheProtocolThread != null)
		globalcacheProtocolThread.interrupt();
	    gc.stopIr(getGcModule(), getGcConnector());
	} catch (HarcHardwareException ex) {
            error(ex);
	}
    }//GEN-LAST:event_protocolStopButtonActionPerformed

    private void protocolDecodeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolDecodeButtonActionPerformed
        String code = protocolRawTextArea.getText();
        try {
             DecodeIR.DecodedSignal[] result = DecodeIR.decode(code);
             if (result == null || result.length == 0) {
                warning("DecodeIR failed (but was found).");
                return;
            }
            for (int i = 0; i < result.length; i++) {
                System.err.println(result[i]);
            }
        } catch (IrpMasterException ex) {
            error(ex);
        } catch (UnsatisfiedLinkError e) {
	    error("DecodeIR not found.");
	} catch (NumberFormatException e) {
	    error("Parse error in string; " + e.getMessage());
	}
    }//GEN-LAST:event_protocolDecodeButtonActionPerformed

    private void protocolGenerateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolGenerateButtonActionPerformed
        try {
	    IrSignal code = extractCode();
	    if (code == null)
		return;
	    protocolRawTextArea.setText(code.ccfString());
            enableProtocolButtons(true);
	} catch (RecognitionException ex) {
	    error(ex);
	} catch (IrpMasterException ex) {
	    error(ex);
	} catch (NumberFormatException e) {
	    error("Parse error " + e.getMessage());
	}
    }//GEN-LAST:event_protocolGenerateButtonActionPerformed

    private void protocolSendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolSendButtonActionPerformed
        int count = Integer.parseInt((String) noSendsProtocolComboBox.getModel().getSelectedItem());
        boolean useGlobalcache = protocolOutputhwComboBox.getSelectedIndex() == hardwareIndexGlobalCache;
        boolean useIrtrans = protocolOutputhwComboBox.getSelectedIndex() == hardwareIndexIrtrans;
        boolean useLirc = protocolOutputhwComboBox.getSelectedIndex() == hardwareIndexLirc;
        boolean useAudio = protocolOutputhwComboBox.getSelectedIndex() == hardwareIndexAudio;

        String ccf = protocolRawTextArea.getText();
        /* If raw code null, take code from the upper row, ignoring text areas*/
        IrSignal code = null;
        try {
            code = (ccf == null || ccf.trim().equals("")) ? extractCode() : ExchangeIR.interpretString(ccf);
        } catch (NumberFormatException ex) {
            error(ex);
        } catch (IrpMasterException ex) {
            error(ex);
        } catch (RecognitionException ex) {
            error(ex);
        }
        if (code == null)
            return;
        if (useGlobalcache) {
            if (globalcacheProtocolThread != null) {
                warning("globalcacheProtocolThread != null, waiting...");
                try {
                    globalcacheProtocolThread.join();
                } catch (InterruptedException ex) {
                    info("***** Interrupted *********");
                }
            }
            globalcacheProtocolThread = new GlobalcacheThread(code, getGcModule(), getGcConnector(), count, protocolSendButton, protocolStopButton);
            globalcacheProtocolThread.start();
        } else if (useIrtrans) {
            if (irtransThread != null) {
                warning("irtransThread != null, waiting...");
                try {
                    irtransThread.join();
                } catch (InterruptedException ex) {
                    info("***** Interrupted *********");
                }
            }
            irtransThread = new IrtransThread(code, getIrtransLed(), count, protocolSendButton, protocolStopButton);
            irtransThread.start();

        } else if (useLirc) {
            if (lircClient == null) {
                warning("No LIRC server initialized, blindly trying...");
                LircIPAddressTextFieldActionPerformed(null);
            }
            if (lircClient == null) {
                error("No LIRC server defined.");
                return;
            }
            try {
                boolean success = lircClient.sendIr(code, count, lircClient.newTransmitter(lircSelectedTransmitter));
                if (!success)
                    error("sendir failed");
            } catch (HarcHardwareException ex) {
                error(ex);
            } catch (IrpMasterException ex) {
                error(ex);
            }
        } else if (useAudio) {
            getAudioLine();
            if (audioLine == null)
                return;
            try {
                Wave wave = new Wave(code.toModulatedIrSequence(count-1), audioFormat,
                        audioOmitCheckBox.isSelected(), audioWaveformComboBox.getSelectedIndex() == 0,
                        audioDivideCheckBox.isSelected());
                wave.play(audioLine);
            } catch (LineUnavailableException ex) {
                error(ex);
            } catch (IOException ex) {
                error(ex);
            } catch (IncompatibleArgumentException ex) {
                error(ex);
                //return;
            }
        } else
            error("This cannot happen, internal error.");
    }//GEN-LAST:event_protocolSendButtonActionPerformed

    private void commandnoTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandnoTextFieldFocusLost
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_commandnoTextFieldFocusLost

    private void commandnoTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandnoTextFieldActionPerformed
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_commandnoTextFieldActionPerformed

    private void devicenoTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_devicenoTextFieldFocusLost
	possiblyEnableEncodeSend();
    }//GEN-LAST:event_devicenoTextFieldFocusLost

    private void devicenoTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_devicenoTextFieldActionPerformed
        possiblyEnableEncodeSend();
    }//GEN-LAST:event_devicenoTextFieldActionPerformed

    private void protocolComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolComboBoxActionPerformed
        updateProtocolParameters(false);
    }//GEN-LAST:event_protocolComboBoxActionPerformed

    private boolean irpmasterRenderer() {
        return rendererComboBox.getSelectedIndex() == 0;
    }

    private boolean makehexRenderer() {
        return rendererComboBox.getSelectedIndex() == 1;
    }

    private String[] irpMasterProtocols() {
        if (irpMaster == null)
            return new String[]{"--"};

        String[] protocolList = irpMaster.getNames().toArray(new String[0]);
        java.util.Arrays.sort(protocolList, String.CASE_INSENSITIVE_ORDER);
        return protocolList;
    }

    public class IrpExtensionFilter implements FilenameFilter {

        public IrpExtensionFilter() {
        }

        @Override
        public boolean accept(File directory, String name) {
            return name.toLowerCase().endsWith(IrpFileExtension);
        }
    }

    private String[] makehexProtocols() {
        File dir = new File(Props.getInstance().getMakehexIrpdir());
        if (!dir.isDirectory())
            return null;

        String[] files = dir.list(new IrpExtensionFilter());

        for (int i =0; i < files.length; i++)
            files[i] = files[i].substring(0, files[i].lastIndexOf(IrpFileExtension)-1);
        java.util.Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    private void rendererComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rendererComboBoxActionPerformed
        if (irpmasterRenderer()) {
            // IrpMaster
            protocolComboBox.setModel(new DefaultComboBoxModel(irpMasterProtocols()));
            protocolComboBox.setSelectedItem(Props.getInstance().getProtocol());
            exportFormatComboBox.setEnabled(true);
            exportRawCheckBox.setEnabled(true);
            exportProntoCheckBox.setEnabled(true);
        } else {
            // Makehex
            String[] filenames = makehexProtocols();
            protocolComboBox.setModel(new DefaultComboBoxModel(filenames));
            String oldProtocol = Props.getInstance().getProtocol();
            for (int i = 0; i < filenames.length; i++)
                if (filenames[i].equalsIgnoreCase(oldProtocol)) {
                    protocolComboBox.setSelectedIndex(i);
                    break;
                }

            exportFormatComboBox.setSelectedIndex(0);
            exportFormatComboBox.setEnabled(false);
            exportRawCheckBox.setSelected(false);
            exportRawCheckBox.setEnabled(false);
            exportProntoCheckBox.setSelected(true);
            exportProntoCheckBox.setEnabled(false);
        }
        
        IrpTextField.setEnabled(irpmasterRenderer());
        updateProtocolParameters(false);
        protocolParamsTextField.setText(null);
        protocolRawTextArea.setText(null);
        enableProtocolButtons(false);
    }//GEN-LAST:event_rendererComboBoxActionPerformed

    private void protocolAnalyzeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolAnalyzeButtonActionPerformed
        String code = protocolRawTextArea.getText().trim();
	try {
            IrSignal irSignal = ExchangeIR.interpretString(code);
            if (irSignal != null) {
                Analyzer analyzer = ExchangeIR.newAnalyzer(irSignal);
                message("Analyzer result: " + analyzer.getIrpWithAltLeadout());
            } else
                message("Analyzer could not interpret the signal");
            //} catch (IrpMasterException e) {
            //    System.err.println(e.getMessage());
        } catch (NumberFormatException e) {
            error("Parse error in string; " + e.getMessage());
        }
    }//GEN-LAST:event_protocolAnalyzeButtonActionPerformed

    private void protocolPlotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolPlotButtonActionPerformed
        String ccf = null;
        String legend = null;
        IrSignal irSignal = null;
        try {
            ccf = protocolRawTextArea.getText();
            if (!ccf.isEmpty()) {
                irSignal = ExchangeIR.interpretString(ccf);
                legend = ccf.substring(0, Math.min(40, ccf.length()));
            } else {
                irSignal = extractCode();
                legend = codeNotationString;
            }
        } catch (IrpMasterException ex) {
            error(ex);
        } catch (NumberFormatException ex) {
            error("Could not parse input: " + ex.getMessage());
        } catch (RecognitionException ex) {
            error(ex);
        }
        if (irSignal == null) {
            error("Rendering failed, plot skipped.");
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Plotter plotter = new Plotter(irSignal, false, "IrMaster plot #" + ++plotNumber
                + " (" + dateFormat.format(new Date()) + ")", legend);
        
        // The autors of PLPlot thinks that "Java look is pretty lame", so they tinker
        // with the UIManager, grrr. This is bad for a program like this one.
        // I have therefore commented out the line
        // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        // in PlotFrame(String title, PlotBox plotArg),
        // (line 133 in PlotFrame.java).
    }//GEN-LAST:event_protocolPlotButtonActionPerformed

    private class WarDialerThread extends Thread {
        public WarDialerThread() {
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            //pauseButton.setEnabled(true);
            int beg = -1;
            int end = -1;
            int delay = -9999;
            try {
                beg = Integer.parseInt(commandnoTextField.getText());
                end = Integer.parseInt(endFTextField.getText());
                delay = Math.round((int) (Double.parseDouble(delayTextField.getText()) * 1000));
            } catch (NumberFormatException ex) {
                error(ex);
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                pauseButton.setEnabled(false);
                warDialerThread = null;
                return;
            }
            int hwIndex = warDialerOutputhwComboBox.getSelectedIndex();
            int noSends = Integer.parseInt((String)warDialerNoSendsComboBox.getSelectedItem());
            for (int cmd = beg; cmd <= end; cmd++) {
                currentFTextField.setText(Integer.toString(cmd));
                try {
                    IrSignal code = extractCode(cmd);
                    boolean success;
                    switch (hwIndex) {
                        case hardwareIndexGlobalCache:
                            success = gc.sendIr(code, noSends, getGcModule(), getGcConnector());
                            break;
                        case hardwareIndexIrtrans:
                            success = irt.sendIr(code, noSends, getIrtransLed());
                            break;
                        case hardwareIndexLirc:
                            success = lircClient.sendCcf(code.ccfString(), noSends, lircSelectedTransmitter);
                            break;
                        case hardwareIndexAudio:
                            Wave wave = new Wave(code.toModulatedIrSequence(noSends - 1), audioFormat,
                                    audioOmitCheckBox.isSelected(),
                                    audioWaveformComboBox.getSelectedIndex() == 0,
                                    audioDivideCheckBox.isSelected());
                            wave.play(audioLine);
                            success = true;
                            break;
                        default:
                            error("Internal error, sorry.");
                            success = false;
                            break;
                    }
                    if (!success)
                        break;
                    Thread.sleep(delay);
                } catch (LineUnavailableException ex) {
                    error(ex);
                } catch (HarcHardwareException ex) {
                    error(ex);
                } catch (IOException ex) {
                    error(ex);
                } catch (NumberFormatException ex) {
                    error(ex);
                } catch (IrpMasterException ex) {
                    error(ex);
                } catch (RecognitionException ex) {
                    error(ex);
                } catch (InterruptedException ex) {
                    info("*** Interrupted ***");
                    break;
                }
            }
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false);
            warDialerThread = null;
        }
    }

    private static WarDialerThread warDialerThread = null;

    private void enableProtocolButtons(boolean state) {
        protocolClearButton.setEnabled(state);
        protocolAnalyzeButton.setEnabled(state);
        protocolDecodeButton.setEnabled(state);
        possiblyEnableEncodeSend();
        //protocolPlotButton.setEnabled(state);
    }

    private void enableProtocolButtons() {
        enableProtocolButtons(!protocolRawTextArea.getText().isEmpty());
    }

    private void consoleClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleClearMenuItemActionPerformed
        consoleTextArea.setText(null);
    }//GEN-LAST:event_consoleClearMenuItemActionPerformed

    private void consoleTextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleTextAreaMousePressed
        if (evt.isPopupTrigger())
           consolePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_consoleTextAreaMousePressed

    private void consoleTextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_consoleTextAreaMouseReleased
        consoleTextAreaMousePressed(evt);
    }//GEN-LAST:event_consoleTextAreaMouseReleased

    private void consoleCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopyMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getText());
    }//GEN-LAST:event_consoleCopyMenuItemActionPerformed

    private void consoleSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleSaveMenuItemActionPerformed
        consoletextSaveMenuItemActionPerformed(evt);
    }//GEN-LAST:event_consoleSaveMenuItemActionPerformed

    private void consoleCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleCopySelectionMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(consoleTextArea.getSelectedText());
    }//GEN-LAST:event_consoleCopySelectionMenuItemActionPerformed

    private void protocolRawTextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocolRawTextAreaMousePressed
        if (evt.isPopupTrigger())
           CCFCodePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_protocolRawTextAreaMousePressed

    private void protocolRawTextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocolRawTextAreaMouseReleased
        protocolRawTextAreaMousePressed(evt);
    }//GEN-LAST:event_protocolRawTextAreaMouseReleased

    private void rawCodeClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeClearMenuItemActionPerformed
        protocolRawTextArea.setText("");
    }//GEN-LAST:event_rawCodeClearMenuItemActionPerformed

    private void rawCodeCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(protocolRawTextArea.getSelectedText());
    }//GEN-LAST:event_rawCodeCopyMenuItemActionPerformed

    private void rawCodePasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteMenuItemActionPerformed
        protocolRawTextArea.setText((new CopyClipboardText()).fromClipboard());
        enableProtocolButtons();
    }//GEN-LAST:event_rawCodePasteMenuItemActionPerformed

    private void rawCodeSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSaveMenuItemActionPerformed
        if (protocolRawTextArea.getText().isEmpty()) {
            error("Nothing to save.");
            return;
        }
        File export = selectFile("Select file to save", true, Props.getInstance().getExportdir(), null, null);
        if (export != null) {
            try {
                PrintStream printStream = new PrintStream(export);
                printStream.println(protocolRawTextArea.getText());
                printStream.close();
            } catch (FileNotFoundException ex) {
                error(ex);
            }
        }
    }//GEN-LAST:event_rawCodeSaveMenuItemActionPerformed

    private void rawCodeImportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeImportMenuItemActionPerformed
        protocolImportButtonActionPerformed(evt);
    }//GEN-LAST:event_rawCodeImportMenuItemActionPerformed

    private void viewExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewExportButtonActionPerformed
        open(lastExportFile, verbose);
    }//GEN-LAST:event_viewExportButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        doExit();
    }//GEN-LAST:event_formWindowClosing

    private void checkUpdatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpdatesMenuItemActionPerformed
        BufferedReader in = null;
        try {
            URL url = new URL(Version.currentVersionUrl);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String current = in.readLine().trim();
            info(current.equals(Version.versionString)
                    ? "You are using the latest version of IrMaster, " + Version.versionString
                    : "Current version is " + current + ", your version is " + Version.versionString);
        } catch (IOException ex) {
            error("Problem getting current version: " + ex.getMessage());
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                error("Problem closing version check Url");
            }
        }
    }//GEN-LAST:event_checkUpdatesMenuItemActionPerformed

    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        JMenuItem jmi = (JMenuItem)evt.getSource();
        JPopupMenu jpm = (JPopupMenu)jmi.getParent();
        JTextField jtf = (JTextField) jpm.getInvoker();
        (new CopyClipboardText()).toClipboard(jtf.getText());
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void cpCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cpCopyMenuItemActionPerformed
        copyMenuItemActionPerformed(evt);
    }//GEN-LAST:event_cpCopyMenuItemActionPerformed

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        JMenuItem jmi = (JMenuItem)evt.getSource();
        JPopupMenu jpm = (JPopupMenu)jmi.getParent();
        JTextField jtf = (JTextField) jpm.getInvoker();
        if (jtf.isEditable()) {
            jtf.setText((new CopyClipboardText()).fromClipboard());
            jtf.postActionEvent();
        }
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void genericCopyPasteMenu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_genericCopyPasteMenu
        if (evt.isPopupTrigger())
           copyPastePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_genericCopyPasteMenu

    private void genericCopyMenu(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_genericCopyMenu
        if (evt.isPopupTrigger())
           copyPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_genericCopyMenu

    private void openExportDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExportDirButtonActionPerformed
        open(Props.getInstance().getExportdir(), verbose);
    }//GEN-LAST:event_openExportDirButtonActionPerformed

    private void exportFormatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatComboBoxActionPerformed
        enableExportFormatRelated();
    }//GEN-LAST:event_exportFormatComboBoxActionPerformed

    private void enableExportFormatRelated() {
        String format = (String) exportFormatComboBox.getSelectedItem();
        boolean multiFormExport = format.equalsIgnoreCase("text") || format.equalsIgnoreCase("xml");
        boolean multiSignalExport = format.equalsIgnoreCase("text") || format.equalsIgnoreCase("xml") || format.equalsIgnoreCase("lirc");
        exportRawCheckBox.setEnabled(multiFormExport);
        exportProntoCheckBox.setEnabled(multiFormExport);
        exportUeiLearnedCheckBox.setEnabled(multiFormExport);
        lastFTextField.setEnabled(multiSignalExport);
        exportGenerateTogglesCheckBox.setEnabled(multiSignalExport && toggleComboBox.isEnabled());
        exportRepetitionsComboBox.setEnabled(!multiSignalExport);
    }

    private void irtransRemotesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransRemotesComboBoxActionPerformed
        try {
            String[] commands = irt.getCommands((String)irtransRemotesComboBox.getModel().getSelectedItem()).toArray(new String[0]);
            java.util.Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
            irtransCommandsComboBox.setModel(new DefaultComboBoxModel(commands));
            irtransCommandsComboBox.setEnabled(true);
        } catch (HarcHardwareException ex) {
            error(ex);
        }
    }//GEN-LAST:event_irtransRemotesComboBoxActionPerformed

    private void irtransSendFlashedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransSendFlashedButtonActionPerformed
        try {
            irt.sendIrCommand((String)irtransRemotesComboBox.getModel().getSelectedItem(),
                    (String) irtransCommandsComboBox.getModel().getSelectedItem(),
                    Integer.parseInt((String) noSendsIrtransFlashedComboBox.getModel().getSelectedItem()),
                    getIrtransLed());
        } catch (HarcHardwareException ex) {
            error(ex);
        }
    }//GEN-LAST:event_irtransSendFlashedButtonActionPerformed

    private void lircRemotesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircRemotesComboBoxActionPerformed
        String remote = (String) lircRemotesComboBox.getModel().getSelectedItem();
        try {
            String[] commands = lircClient.getCommands(remote).toArray(new String[0]);
            if (commands == null) {
                error("Getting commands failed. Try again.");
                lircCommandsComboBox.setEnabled(false);
            } else {
                Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
                lircCommandsComboBox.setModel(new DefaultComboBoxModel(commands));
                lircCommandsComboBox.setEnabled(true);
            }
        } catch (HarcHardwareException ex) {
            error("LIRC failed: " + ex.getMessage());
        }
    }//GEN-LAST:event_lircRemotesComboBoxActionPerformed

    private void lircSendPredefinedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircSendPredefinedButtonActionPerformed
        try {
            lircClient.sendIrCommand((String) lircRemotesComboBox.getModel().getSelectedItem(),
                    (String) lircCommandsComboBox.getModel().getSelectedItem(),
                    Integer.parseInt((String) noLircPredefinedsComboBox.getModel().getSelectedItem()),
                    lircSelectedTransmitter);
        } catch (HarcHardwareException ex) {
            error(ex);
        }
    }//GEN-LAST:event_lircSendPredefinedButtonActionPerformed

    private void lircTransmitterComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircTransmitterComboBoxActionPerformed
        // Selecting more than one transmitter is not supported.
        if (lircClient == null) {
            error("No Lirc Server selected.");
            return;
        }
        lircSelectedTransmitter = Integer.parseInt((String) lircTransmitterComboBox.getModel().getSelectedItem());
        try {
            lircClient.setTransmitters(lircSelectedTransmitter);
        } catch (HarcHardwareException ex) {
            error(ex);
        }
    }//GEN-LAST:event_lircTransmitterComboBoxActionPerformed

    private void browseHomePageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseHomePageMenuItemActionPerformed
        browse(Version.homepageUrl, verbose);
    }//GEN-LAST:event_browseHomePageMenuItemActionPerformed

    private void protocolOutputhwComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolOutputhwComboBoxActionPerformed
        hardwareIndex = protocolOutputhwComboBox.getSelectedIndex();
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
    }//GEN-LAST:event_protocolOutputhwComboBoxActionPerformed

    private void disregardRepeatMinsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disregardRepeatMinsCheckBoxMenuItemActionPerformed
        Props.getInstance().setDisregardRepeatMins(disregardRepeatMinsCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_disregardRepeatMinsCheckBoxMenuItemActionPerformed

    private void readButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readButtonActionPerformed
        irtransAddressTextFieldActionPerformed(null);
    }//GEN-LAST:event_readButtonActionPerformed

    private void readLircButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readLircButtonActionPerformed
        LircIPAddressTextFieldActionPerformed(null);
    }//GEN-LAST:event_readLircButtonActionPerformed

    private void gcModuleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcModuleComboBoxActionPerformed
        Props.getInstance().setGlobalcacheModule(Integer.parseInt((String)gcModuleComboBox.getSelectedItem()));
    }//GEN-LAST:event_gcModuleComboBoxActionPerformed

    private void gcConnectorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcConnectorComboBoxActionPerformed
        Props.getInstance().setGlobalcachePort(Integer.parseInt((String)gcConnectorComboBox.getSelectedItem()));
    }//GEN-LAST:event_gcConnectorComboBoxActionPerformed

    private void irtransLedComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransLedComboBoxActionPerformed
        Props.getInstance().setIrTransPort(irtransLedComboBox.getSelectedIndex());
    }//GEN-LAST:event_irtransLedComboBoxActionPerformed

    private void protocolRawTextAreaMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_protocolRawTextAreaMouseExited
        enableProtocolButtons();
    }//GEN-LAST:event_protocolRawTextAreaMouseExited

    private void browseIRPMasterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPMasterMenuItemActionPerformed
        browse(Props.getInstance().getIrpmasterUrl(), verbose);
    }//GEN-LAST:event_browseIRPMasterMenuItemActionPerformed

    private void browseJP1WikiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseJP1WikiActionPerformed
        browse(jp1WikiUrl, verbose);
    }//GEN-LAST:event_browseJP1WikiActionPerformed

    private void browseIRPSpecMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIRPSpecMenuItemActionPerformed
        browse(GuiMain.irpNotationUrl, verbose);
    }//GEN-LAST:event_browseIRPSpecMenuItemActionPerformed

    private void browseDecodeIRMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDecodeIRMenuItemActionPerformed
        browse(decodeIrUrl, verbose);
    }//GEN-LAST:event_browseDecodeIRMenuItemActionPerformed

    private void rawCodeSelectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSelectAllMenuItemActionPerformed
        protocolRawTextArea.selectAll();
    }//GEN-LAST:event_rawCodeSelectAllMenuItemActionPerformed

    private void rawCodeCopyAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyAllMenuItemActionPerformed
        (new CopyClipboardText()).toClipboard(protocolRawTextArea.getText());
    }//GEN-LAST:event_rawCodeCopyAllMenuItemActionPerformed

    private void audioGetLineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioGetLineButtonActionPerformed
        getAudioLine();
    }//GEN-LAST:event_audioGetLineButtonActionPerformed

    private void audioReleaseLineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioReleaseLineButtonActionPerformed
        releaseAudioLine();
    }//GEN-LAST:event_audioReleaseLineButtonActionPerformed

    private void lircPingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircPingButtonActionPerformed
        ping(LircIPAddressTextField);
    }//GEN-LAST:event_lircPingButtonActionPerformed

    private void irtransPingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransPingButtonActionPerformed
        ping(irtransAddressTextField);
    }//GEN-LAST:event_irtransPingButtonActionPerformed

    private void globalCachePingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCachePingButtonActionPerformed
        ping(gcAddressTextField);
    }//GEN-LAST:event_globalCachePingButtonActionPerformed

    private void popupsForHelpCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupsForHelpCheckBoxMenuItemActionPerformed
        Props.getInstance().setPopupsForHelp(popupsForHelpCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_popupsForHelpCheckBoxMenuItemActionPerformed

    private void protocolDocButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolDocButtonActionPerformed
        if (irpmasterRenderer()) {
            String protocolName = (String) protocolComboBox.getSelectedItem();
            help((this.uiFeatures.irpLine ? (irpMaster.getIrp(protocolName) + "\n\n") : "")
                    + irpMaster.getDocumentation(protocolName));
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(getMakehexIrpFile())));
                protocolRawTextArea.setText("");
                String line;
                StringBuilder payload = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    payload.append(line + "\n");
                }
                help(payload.toString());
            } catch (FileNotFoundException ex) {
                error("IRP file " + getMakehexIrpFile() + " not found.");
            } catch (IOException ex) {
                error(ex);
            }
        }
    }//GEN-LAST:event_protocolDocButtonActionPerformed

    private void analyzeHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzeHelpButtonActionPerformed
        help(analyzeHelpText);
    }//GEN-LAST:event_analyzeHelpButtonActionPerformed

    private void exportHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportHelpButtonActionPerformed
        help(exportHelpText);
    }//GEN-LAST:event_exportHelpButtonActionPerformed

    private void globalCacheHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheHelpButtonActionPerformed
        help(globalCacheHelpText);
    }//GEN-LAST:event_globalCacheHelpButtonActionPerformed

    private void irtransHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtransHelpButtonActionPerformed
        help(irtransHelpText);
    }//GEN-LAST:event_irtransHelpButtonActionPerformed

    private void lircHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircHelpButtonActionPerformed
        help(lircHelpText);
    }//GEN-LAST:event_lircHelpButtonActionPerformed

    private void audioHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioHelpButtonActionPerformed
        help(audioHelpText);
    }//GEN-LAST:event_audioHelpButtonActionPerformed

    private void warDialerHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_warDialerHelpButtonActionPerformed
        help(warDialerHelpText);
    }//GEN-LAST:event_warDialerHelpButtonActionPerformed

    private void warDialerOutputhwComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_warDialerOutputhwComboBoxActionPerformed
        hardwareIndex = warDialerOutputhwComboBox.getSelectedIndex();
        outputHWTabbedPane.setSelectedIndex(hardwareIndex);
    }//GEN-LAST:event_warDialerOutputhwComboBoxActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        warDialerThread.interrupt();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (warDialerThread != null)
            error("warDialerThread != null");

        int hwIndex = warDialerOutputhwComboBox.getSelectedIndex();
        if (hwIndex == hardwareIndexAudio) {
            updateAudioFormat();
            getAudioLine();
            if (audioLine == null) {
                error("Could not get an audio line.");
                return;
            }
        }
        warDialerThread = new WarDialerThread();
        warDialerThread.start();
    }//GEN-LAST:event_startButtonActionPerformed

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
        
    }//GEN-LAST:event_pauseButtonActionPerformed

    private void usePopupsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usePopupsCheckBoxMenuItemActionPerformed
        Props.getInstance().setUsePopupsForErrors(usePopupsCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_usePopupsCheckBoxMenuItemActionPerformed

    private void irpMasterDbEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpMasterDbEditMenuItemActionPerformed
        open(Props.getInstance().getIrpmasterConfigfile(), verbose);
        warning("If editing the file, changes will not take effect before you save the file AND restart IrMaster!");
    }//GEN-LAST:event_irpMasterDbEditMenuItemActionPerformed

    private void irpMasterDbSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpMasterDbSelectMenuItemActionPerformed
        String oldDir = (new File(Props.getInstance().getIrpmasterConfigfile())).getAbsoluteFile().getParent();
        File f = selectFile("Select protocol file for IrpMaster", false, oldDir, "ini", "Configuration files (*.ini)");
        if (f != null)
            Props.getInstance().setIrpmasterConfigfile(f.getAbsolutePath());
    }//GEN-LAST:event_irpMasterDbSelectMenuItemActionPerformed

    private void makehexDbEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexDbEditMenuItemActionPerformed
        open(Props.getInstance().getMakehexIrpdir(), verbose);
    }//GEN-LAST:event_makehexDbEditMenuItemActionPerformed

    private void makehexDbSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexDbSelectMenuItemActionPerformed
        String oldDir = (new File(Props.getInstance().getIrpmasterConfigfile())).getAbsoluteFile().getParent();
        File f = selectFile("Select direcory containing IRP files for Makehex", false, oldDir, null, "Directories");
        if (f != null)
            Props.getInstance().setMakehexIrpdir(f.getAbsolutePath());
    }//GEN-LAST:event_makehexDbSelectMenuItemActionPerformed

    private void IrCalcMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IrCalcMenuItemActionPerformed
        IrCalc irCalc = new HexCalc(false, lafInfo[Props.getInstance().getLookAndFeel()].getClassName());
        irCalc.setLocationRelativeTo(this);
        irCalc.setVisible(true);
    }//GEN-LAST:event_IrCalcMenuItemActionPerformed

    private void FrequencyTimeCalcMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FrequencyTimeCalcMenuItemActionPerformed
        IrCalc irCalc = new TimeFrequencyCalc(false, lafInfo[Props.getInstance().getLookAndFeel()].getClassName());
        irCalc.setLocationRelativeTo(this);
        irCalc.setVisible(true);
    }//GEN-LAST:event_FrequencyTimeCalcMenuItemActionPerformed

    private void help(String helpText) {
        if (popupsForHelpCheckBoxMenuItem.isSelected())
            HelpPopup.newHelpPopup(this, helpText);
        else
            System.err.println(helpText);
    }

    private boolean ping(JTextField jTextField) {
        String host = jTextField.getText();
        boolean success = false;
        try {
            success = InetAddress.getByName(host).isReachable(Props.getInstance().getPingTimeout());
            info(host + " is reachable");
        } catch (IOException ex) {
            info(host + " is not reachable (using Java's isReachable): " + ex.getMessage());
        }
        return success;
    }

    private void getAudioLine() {
        if (audioLine != null)
            return;
        updateAudioFormat();
        try {
            audioLine = Wave.getLine(audioFormat);
            info("Got an audio line for " + audioFormat.toString());
            audioGetLineButton.setEnabled(false);
            audioReleaseLineButton.setEnabled(true);
        } catch (LineUnavailableException ex) {
            error(ex);
            audioLine = null;
            //audioGetLineButton.setSelected(false);
            audioGetLineButton.setEnabled(true);//;.setSelected(true);
            audioReleaseLineButton.setEnabled(false);
        }
    }

    private void releaseAudioLine() {
        if (audioLine != null)
            audioLine.close();
        audioLine = null;
        audioGetLineButton.setEnabled(true);
        audioReleaseLineButton.setEnabled(false);
    }

    private void updateAudioFormat() {
        int sampleFrequency = Integer.parseInt((String) audioSampleFrequencyComboBox.getSelectedItem());
        int sampleSize = Integer.parseInt((String) audioSampleSizeComboBox.getSelectedItem());
        int channels = audioChannelsComboBox.getSelectedIndex() + 1;
        boolean bigEndian = audioBigEndianCheckBox.isSelected();
        //boolean square = ((String) this.audioWaveformComboBox.getSelectedItem()).equalsIgnoreCase("square");
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) sampleFrequency,
                sampleSize, channels, sampleSize/8*channels, (float) sampleFrequency, bigEndian);
    }

    private void updateLAF(int index) {
        try {
            UIManager.setLookAndFeel(lafInfo[index].getClassName());
            Props.getInstance().setLookAndFeel(index);
            for (int i = 0; i < lafInfo.length; i++)
                lafRadioButtons[i].setSelected(i == index);
        } catch (ClassNotFoundException ex) {
            error(ex.getMessage());
        } catch (InstantiationException ex) {
            error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            error(ex.getMessage());
        } catch (UnsupportedLookAndFeelException ex) {
            error(ex.getMessage());
        }
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }

    private void possiblyEnableEncodeSend() {
        boolean looksOk = !commandnoTextField.getText().isEmpty();
        protocolSendButton.setEnabled(looksOk || !protocolRawTextArea.getText().isEmpty());
        protocolPlotButton.setEnabled(looksOk || !protocolRawTextArea.getText().isEmpty());
        protocolGenerateButton.setEnabled(looksOk);
    }

    private int getGcModule() {
        return Integer.parseInt((String)gcModuleComboBox.getSelectedItem());
    }

    private int getGcConnector() {
        return Integer.parseInt((String) gcConnectorComboBox.getSelectedItem());
    }

    private IrTrans.Led getIrtransLed() {
        return IrTrans.Led.parse((String)irtransLedComboBox.getSelectedItem());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu CCFCodePopupMenu;
    private javax.swing.JMenuItem FrequencyTimeCalcMenuItem;
    private javax.swing.JMenuItem IrCalcMenuItem;
    private javax.swing.JTextField IrpTextField;
    private javax.swing.JTextField LircIPAddressTextField;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JLabel additionalParametersLabel;
    private javax.swing.JButton analyzeHelpButton;
    private javax.swing.JPanel analyzePanel;
    private javax.swing.JPanel analyzeSendPanel;
    private javax.swing.JCheckBox audioBigEndianCheckBox;
    private javax.swing.JComboBox audioChannelsComboBox;
    private javax.swing.JCheckBox audioDivideCheckBox;
    private javax.swing.JPanel audioFormatPanel;
    private javax.swing.JButton audioGetLineButton;
    private javax.swing.JButton audioHelpButton;
    private javax.swing.JCheckBox audioOmitCheckBox;
    private javax.swing.JPanel audioOptionsPanel;
    private javax.swing.JPanel audioPanel;
    private javax.swing.JButton audioReleaseLineButton;
    private javax.swing.JComboBox audioSampleFrequencyComboBox;
    private javax.swing.JComboBox audioSampleSizeComboBox;
    private javax.swing.JComboBox audioWaveformComboBox;
    private javax.swing.JCheckBox automaticFileNamesCheckBox;
    private javax.swing.JMenuItem browseDecodeIRMenuItem;
    private javax.swing.JMenuItem browseHomePageMenuItem;
    private javax.swing.JMenuItem browseIRPMasterMenuItem;
    private javax.swing.JMenuItem browseIRPSpecMenuItem;
    private javax.swing.JMenuItem browseJP1Wiki;
    private javax.swing.JMenuItem checkUpdatesMenuItem;
    private javax.swing.JMenuItem clearConsoleMenuItem;
    private javax.swing.JTextField commandnoTextField;
    private javax.swing.JMenuItem consoleClearMenuItem;
    private javax.swing.JMenuItem consoleCopyMenuItem;
    private javax.swing.JMenuItem consoleCopySelectionMenuItem;
    private javax.swing.JPopupMenu consolePopupMenu;
    private javax.swing.JMenuItem consoleSaveMenuItem;
    private javax.swing.JScrollPane consoleScrollPane;
    private javax.swing.JTextArea consoleTextArea;
    private javax.swing.JMenuItem consoletextSaveMenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copyConsoleToClipboardMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JPopupMenu copyPastePopupMenu;
    private javax.swing.JPopupMenu copyPopupMenu;
    private javax.swing.JMenuItem cpCopyMenuItem;
    private javax.swing.JTextField currentFTextField;
    private javax.swing.JMenu debugMenu;
    private javax.swing.JPopupMenu.Separator debugSeparator;
    private javax.swing.JTextField delayTextField;
    private javax.swing.JLabel deviceNumberLabel;
    private javax.swing.JTextField devicenoTextField;
    private javax.swing.JButton discoverButton;
    private javax.swing.JCheckBoxMenuItem disregardRepeatMinsCheckBoxMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTextField endFTextField;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JComboBox exportFormatComboBox;
    private javax.swing.JCheckBox exportGenerateTogglesCheckBox;
    private javax.swing.JButton exportHelpButton;
    private javax.swing.JLabel exportNoRepetitionsLabel;
    private javax.swing.JPanel exportPanel;
    private javax.swing.JCheckBox exportProntoCheckBox;
    private javax.swing.JCheckBox exportRawCheckBox;
    private javax.swing.JComboBox exportRepetitionsComboBox;
    private javax.swing.JCheckBox exportUeiLearnedCheckBox;
    private javax.swing.JButton exportdirBrowseButton;
    private javax.swing.JTextField exportdirTextField;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel functionNumberLabel;
    private javax.swing.JTextField gcAddressTextField;
    private javax.swing.JButton gcBrowseButton;
    private javax.swing.JComboBox gcConnectorComboBox;
    private javax.swing.JLabel gcDiscoveredTypeLabel;
    private javax.swing.JComboBox gcModuleComboBox;
    private javax.swing.JButton globalCacheHelpButton;
    private javax.swing.JButton globalCachePingButton;
    private javax.swing.JPanel globalcachePanel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenu irProtocolDatabaseMenu;
    private javax.swing.JMenu irpMasterDatabaseMenu;
    private javax.swing.JMenuItem irpMasterDbEditMenuItem;
    private javax.swing.JMenuItem irpMasterDbSelectMenuItem;
    private javax.swing.JTextField irtransAddressTextField;
    private javax.swing.JButton irtransBrowseButton;
    private javax.swing.JComboBox irtransCommandsComboBox;
    private javax.swing.JButton irtransHelpButton;
    private javax.swing.JPanel irtransIPPanel;
    private javax.swing.JComboBox irtransLedComboBox;
    private javax.swing.JPanel irtransPanel;
    private javax.swing.JButton irtransPingButton;
    private javax.swing.JPanel irtransPredefinedPanel;
    private javax.swing.JComboBox irtransRemotesComboBox;
    private javax.swing.JButton irtransSendFlashedButton;
    private javax.swing.JLabel irtransVersionLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JMenu lafMenu;
    private javax.swing.JPopupMenu.Separator lafSeparator;
    private javax.swing.JTextField lastFTextField;
    private javax.swing.JComboBox lircCommandsComboBox;
    private javax.swing.JButton lircHelpButton;
    private javax.swing.JPanel lircIPPanel;
    private javax.swing.JPanel lircPanel;
    private javax.swing.JButton lircPingButton;
    private javax.swing.JTextField lircPortTextField;
    private javax.swing.JPanel lircPredefinedPanel;
    private javax.swing.JComboBox lircRemotesComboBox;
    private javax.swing.JButton lircSendPredefinedButton;
    private javax.swing.JLabel lircServerVersionLabel;
    private javax.swing.JLabel lircServerVersionText;
    private javax.swing.JButton lircStopIrButton;
    private javax.swing.JComboBox lircTransmitterComboBox;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JMenu makehexDatabaseMenu;
    private javax.swing.JMenuItem makehexDbEditMenuItem;
    private javax.swing.JMenuItem makehexDbSelectMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox noLircPredefinedsComboBox;
    private javax.swing.JComboBox noSendsIrtransFlashedComboBox;
    private javax.swing.JComboBox noSendsProtocolComboBox;
    private javax.swing.JButton notesClearButton;
    private javax.swing.JButton notesEditButton;
    private javax.swing.JButton notesSaveButton;
    private javax.swing.JButton openExportDirButton;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JButton pauseButton;
    private javax.swing.JCheckBoxMenuItem popupsForHelpCheckBoxMenuItem;
    private javax.swing.JButton protocolAnalyzeButton;
    private javax.swing.JButton protocolClearButton;
    private javax.swing.JComboBox protocolComboBox;
    private javax.swing.JButton protocolDecodeButton;
    private javax.swing.JButton protocolDocButton;
    private javax.swing.JButton protocolExportButton;
    private javax.swing.JButton protocolGenerateButton;
    private javax.swing.JButton protocolImportButton;
    private javax.swing.JComboBox protocolOutputhwComboBox;
    private javax.swing.JTextField protocolParamsTextField;
    private javax.swing.JButton protocolPlotButton;
    private javax.swing.JTextArea protocolRawTextArea;
    private javax.swing.JButton protocolSendButton;
    private javax.swing.JButton protocolStopButton;
    private javax.swing.JPanel protocolsPanel;
    private javax.swing.JTabbedPane protocolsSubPane;
    private javax.swing.JMenuItem rawCodeClearMenuItem;
    private javax.swing.JMenuItem rawCodeCopyAllMenuItem;
    private javax.swing.JMenuItem rawCodeCopyMenuItem;
    private javax.swing.JMenuItem rawCodeImportMenuItem;
    private javax.swing.JMenuItem rawCodePasteMenuItem;
    private javax.swing.JMenuItem rawCodeSaveMenuItem;
    private javax.swing.JMenuItem rawCodeSelectAllMenuItem;
    private javax.swing.JButton readButton;
    private javax.swing.JButton readLircButton;
    private javax.swing.JComboBox rendererComboBox;
    private javax.swing.JLabel rendererLabel;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JLabel subDeviceNumberLabel;
    private javax.swing.JTextField subdeviceTextField;
    private javax.swing.JComboBox toggleComboBox;
    private javax.swing.JLabel toggleLabel;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JCheckBoxMenuItem usePopupsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem verboseCheckBoxMenuItem;
    private javax.swing.JButton viewExportButton;
    private javax.swing.JButton warDialerHelpButton;
    private javax.swing.JComboBox warDialerNoSendsComboBox;
    private javax.swing.JComboBox warDialerOutputhwComboBox;
    private javax.swing.JPanel warDialerPanel;
    // End of variables declaration//GEN-END:variables
    private AboutPopup aboutBox;
}
