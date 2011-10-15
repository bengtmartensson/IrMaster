/*
Copyright (C) 2011 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published byto
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope thlat it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package IrMaster;

import IrpMaster.DecodeIR;
import IrpMaster.ICT;
import IrpMaster.IncompatibleArgumentException;
import IrpMaster.IrSignal;
import IrpMaster.IrpMaster;
import IrpMaster.IrpMasterException;
import IrpMaster.IrpUtils;
import IrpMaster.Pronto;
import IrpMaster.Protocol;
import IrpMaster.UnassignedException;
import exchangeir.Analyzer;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.amx_beacon;
import org.harctoolbox.globalcache;
import org.harctoolbox.harcutils;
import org.harctoolbox.irtrans;
// do not import org.harctoolbox.protocol;
import org.harctoolbox.toggletype;

/**
 * This class implements a GUI for ...
 */

public class GuiMain extends javax.swing.JFrame {
    private static IrpMaster irpMaster = null;
    private static HashMap<String, Protocol> protocols = null;
    private final static short invalid_parameter = -1;
    private int debug = 0;
    private boolean verbose = false;
    private DefaultComboBoxModel gc_modules_dcbm;
    private DefaultComboBoxModel rendererDcbm;
    private String[] prontomodelnames;
    //private String[] button_remotenames;
    private static final String dummy_no_selection = "--------";

    private globalcache_thread the_globalcache_device_thread = null;
    private globalcache_thread the_globalcache_protocol_thread = null;
    private irtrans_thread the_irtrans_thread = null;
    
    private globalcache gc = null;
    private irtrans irt = null;

    private HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    private File select_file(String title, String extension, String file_type_desc, boolean save, String defaultdir) {
        String startdir = this.filechooserdirs.containsKey(title) ? this.filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setDialogTitle(title);
        if (extension == null) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else
            chooser.setFileFilter(new FileNameExtensionFilter(file_type_desc, extension));

        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else
            return null;
    }

    private class copy_clipboard_text implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void to_clipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }
    }
    
    private static Protocol get_protocol(String name) throws UnassignedException, RecognitionException {
        if (!protocols.containsKey(name)) {
            Protocol protocol = irpMaster.newProtocol(name);
            protocols.put(name, protocol);
        }
        return protocols.get(name);            
    }

    /** Creates new form gui_main */
    public GuiMain() {
        gc_modules_dcbm = new DefaultComboBoxModel(new String[]{"2"}); // ?

        // TODO: check behavior in abscense of tonto
        com.neuron.app.tonto.ProntoModel[] prontomodels = com.neuron.app.tonto.ProntoModel.getModels();
        prontomodelnames = new String[prontomodels.length];
        for (int i = 0; i < prontomodels.length; i++)
            prontomodelnames[i] = prontomodels[i].toString();

        try {
            irpMaster = new IrpMaster(Props.get_instance().get_irpmaster_configfile());
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
        }
        protocols = new HashMap<String, Protocol>();
        
        initComponents();

        System.setErr(console_PrintStream);
        System.setOut(console_PrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Props.get_instance().save();
                    //socket_storage.dispose_sockets(true);
                } catch (Exception e) {
                    System.out.println("Problems saving properties; " + e.getMessage());
                }
                System.out.println("*************** This is GUI shutdown **********");
            }
        });

        update_protocol_parameters();
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
        //browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), commandtype_t.www, command_t.browse));

        gc = new globalcache("globalcache", globalcache.gc_model.gc_unknown, verbose);
        irt = new irtrans("irtrans", verbose);

        browser_TextField.setText(Props.get_instance().get_browser());
        exportdir_TextField.setText(Props.get_instance().get_exportdir());
        update_from_frequency();
    }

    // From Real Gagnon        
    class FilteredStream extends FilterOutputStream {

        public FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b);
            console_TextArea.append(aString);
            //console_TextArea.repaint();
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            console_TextArea.append(aString);
            console_TextArea.setCaretPosition(console_TextArea.getDocument().getLength());
            //console_TextArea.repaint();
        }
    }
    
    PrintStream console_PrintStream = new PrintStream(
            new FilteredStream(
            new ByteArrayOutputStream()));

    //TODO: boolean logFile;
    private void warning(String message) {
        System.err.println("Warning: " + message);
    }

    //private void do_something() {
        
    //}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainTabbedPane = new javax.swing.JTabbedPane();
        protocolsPanel = new javax.swing.JPanel();
        protocol_ComboBox = new javax.swing.JComboBox();
        deviceno_TextField = new javax.swing.JTextField();
        subdevice_TextField = new javax.swing.JTextField();
        commandno_TextField = new javax.swing.JTextField();
        toggle_ComboBox = new javax.swing.JComboBox();
        protocol_params_TextField = new javax.swing.JTextField();
        DecodeIRVersion = new javax.swing.JLabel();
        protocolsSubPane = new javax.swing.JTabbedPane();
        analyzePanel = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        IRP_TextField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        protocol_raw_TextArea = new javax.swing.JTextArea();
        protocol_generate_Button = new javax.swing.JButton();
        protocol_decode_Button = new javax.swing.JButton();
        protocol_clear_Button = new javax.swing.JButton();
        icf_import_Button = new javax.swing.JButton();
        protocol_outputhw_ComboBox = new javax.swing.JComboBox();
        no_sends_protocol_ComboBox = new javax.swing.JComboBox();
        protocol_send_Button = new javax.swing.JButton();
        protocol_stop_Button = new javax.swing.JButton();
        protocolAnalyzeButton = new javax.swing.JButton();
        protocolCopyButton = new javax.swing.JButton();
        exportPanel = new javax.swing.JPanel();
        protocolExportButton = new javax.swing.JButton();
        automaticFileNamesCheckBox = new javax.swing.JCheckBox();
        exportTogglesGenerateCheckBox = new javax.swing.JCheckBox();
        lastFTextField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        exportdir_TextField = new javax.swing.JTextField();
        exportdir_browse_Button = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        warDialerPanel = new javax.swing.JPanel();
        protocol_outputhw_ComboBox1 = new javax.swing.JComboBox();
        commandno_TextField1 = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        commandno_TextField2 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jLabel30 = new javax.swing.JLabel();
        commandno_TextField3 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        rendererComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        globalcache_Panel = new javax.swing.JPanel();
        gc_address_TextField = new javax.swing.JTextField();
        gc_module_ComboBox = new javax.swing.JComboBox();
        gc_connector_ComboBox = new javax.swing.JComboBox();
        gc_browse_Button = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        discoverButton = new javax.swing.JButton();
        gcDiscoveredTypejTextField = new javax.swing.JTextField();
        irtrans_Panel = new javax.swing.JPanel();
        irtrans_address_TextField = new javax.swing.JTextField();
        irtrans_led_ComboBox = new javax.swing.JComboBox();
        irtrans_browse_Button = new javax.swing.JButton();
        globalcache_Panel1 = new javax.swing.JPanel();
        LIRC_address_TextField = new javax.swing.JTextField();
        LIRCStopIrButton = new javax.swing.JButton();
        LIRC_address_TextField1 = new javax.swing.JTextField();
        hexcalcPanel = new javax.swing.JPanel();
        decimal_TextField = new javax.swing.JTextField();
        hex_TextField = new javax.swing.JTextField();
        complement_decimal_TextField = new javax.swing.JTextField();
        complement_hex_TextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        reverse_hex_TextField = new javax.swing.JTextField();
        reverse_decimal_TextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        no_periods_TextField = new javax.swing.JTextField();
        frequency_TextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        time_TextField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        prontocode_TextField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        period_selection_enable_CheckBox = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        optsTabbedPane = new javax.swing.JTabbedPane();
        general_Panel = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        homeconf_TextField = new javax.swing.JTextField();
        home_select_Button = new javax.swing.JButton();
        home_browse_Button = new javax.swing.JButton();
        home_load_Button = new javax.swing.JButton();
        makehexIrpDir_TextField = new javax.swing.JTextField();
        browser_TextField = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        macro_select_Button = new javax.swing.JButton();
        browser_select_Button = new javax.swing.JButton();
        macro_browse_Button = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        exportopts_TabbedPane = new javax.swing.JTabbedPane();
        general_export_opts_Panel = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        ccf_export_opts_Panel = new javax.swing.JPanel();
        ccf_export_prontomodel_ComboBox = new javax.swing.JComboBox();
        ccf_export_raw_CheckBox = new javax.swing.JCheckBox();
        ccf_export_screenwidth_TextField = new javax.swing.JTextField();
        ccf_export_screenheight_TextField = new javax.swing.JTextField();
        ccf_export_buttonwidth_TextField = new javax.swing.JTextField();
        ccf_export_buttonheight_TextField = new javax.swing.JTextField();
        ccf_export_export_Button = new javax.swing.JButton();
        debug_Panel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        debug_TextField = new javax.swing.JTextField();
        verbose_CheckBox = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        console_TextArea = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        copy_console_to_clipboard_MenuItem = new javax.swing.JMenuItem();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        verbose_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IrMaster -- GUI for several IR programs");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        mainTabbedPane.setPreferredSize(new java.awt.Dimension(600, 472));

        protocolsPanel.setPreferredSize(new java.awt.Dimension(600, 377));

        protocol_ComboBox.setMaximumRowCount(20);
        protocol_ComboBox.setModel(new DefaultComboBoxModel(irpMaster == null ? new String[]{"--"} : harcutils.sort_unique(irpMaster.getNames().toArray(new String[0]))));
        protocol_ComboBox.setToolTipText("Protocol name");
        protocol_ComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_ComboBoxActionPerformed(evt);
            }
        });

        deviceno_TextField.setToolTipText("D, Device number");
        deviceno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceno_TextFieldActionPerformed(evt);
            }
        });
        deviceno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                deviceno_TextFieldFocusLost(evt);
            }
        });

        subdevice_TextField.setToolTipText("S, Subdevice number");
        subdevice_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        subdevice_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        commandno_TextField.setToolTipText("F, Function number (also called Command number or OBC).");
        commandno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextFieldActionPerformed(evt);
            }
        });
        commandno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextFieldFocusLost(evt);
            }
        });

        toggle_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1", "-" }));
        toggle_ComboBox.setSelectedIndex(2);
        toggle_ComboBox.setToolTipText("Toggles to generate");
        toggle_ComboBox.setMaximumSize(new java.awt.Dimension(50, 32767));

        protocol_params_TextField.setToolTipText("Additional parameters needed for some protocols.");
        protocol_params_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_params_TextFieldActionPerformed(evt);
            }
        });
        protocol_params_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                protocol_params_TextFieldFocusLost(evt);
            }
        });

        jLabel26.setText("IRP");

        IRP_TextField.setEditable(false);

        protocol_raw_TextArea.setColumns(20);
        protocol_raw_TextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 14)); // NOI18N
        protocol_raw_TextArea.setLineWrap(true);
        protocol_raw_TextArea.setRows(5);
        protocol_raw_TextArea.setToolTipText("Pronto code; may be edited if desired");
        protocol_raw_TextArea.setWrapStyleWord(true);
        protocol_raw_TextArea.setMinimumSize(new java.awt.Dimension(240, 17));
        protocol_raw_TextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                protocol_raw_TextAreaFocusLost(evt);
            }
        });
        jScrollPane3.setViewportView(protocol_raw_TextArea);

        protocol_generate_Button.setMnemonic('R');
        protocol_generate_Button.setText("Render");
        protocol_generate_Button.setToolTipText("Compute Pronto code from upper row protocol description");
        protocol_generate_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_generate_ButtonActionPerformed(evt);
            }
        });

        protocol_decode_Button.setMnemonic('D');
        protocol_decode_Button.setText("Decode");
        protocol_decode_Button.setToolTipText("Send content of Code window(s) to DecodeIR");
        protocol_decode_Button.setEnabled(false);
        protocol_decode_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_decode_ButtonActionPerformed(evt);
            }
        });

        protocol_clear_Button.setText("Clear");
        protocol_clear_Button.setToolTipText("Clears code text areas");
        protocol_clear_Button.setEnabled(false);
        protocol_clear_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_clear_ButtonActionPerformed(evt);
            }
        });

        icf_import_Button.setText("Import...");
        icf_import_Button.setToolTipText("Import file from IR WIdget/IRScope");
        icf_import_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                icf_import_ButtonActionPerformed(evt);
            }
        });

        protocol_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC" }));
        protocol_outputhw_ComboBox.setToolTipText("Device used for when sending");

        no_sends_protocol_ComboBox.setMaximumRowCount(20);
        no_sends_protocol_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" }));
        no_sends_protocol_ComboBox.setToolTipText("Number of times to send IR signal");

        protocol_send_Button.setMnemonic('S');
        protocol_send_Button.setText("Send");
        protocol_send_Button.setToolTipText("Send code in Code window to output device selected.");
        protocol_send_Button.setEnabled(false);
        protocol_send_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_send_ButtonActionPerformed(evt);
            }
        });

        protocol_stop_Button.setText("Stop");
        protocol_stop_Button.setToolTipText("Stop ongoing IR transmission");
        protocol_stop_Button.setEnabled(false);
        protocol_stop_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_stop_ButtonActionPerformed(evt);
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

        protocolCopyButton.setMnemonic('C');
        protocolCopyButton.setText("Copy");
        protocolCopyButton.setToolTipText("Copy computed code to the clipboard.");
        protocolCopyButton.setEnabled(false);
        protocolCopyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolCopyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout analyzePanelLayout = new javax.swing.GroupLayout(analyzePanel);
        analyzePanel.setLayout(analyzePanelLayout);
        analyzePanelLayout.setHorizontalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(protocol_stop_Button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(protocol_send_Button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, 80, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(protocol_generate_Button)
                    .addComponent(icf_import_Button)
                    .addComponent(protocol_decode_Button)
                    .addComponent(protocolAnalyzeButton)
                    .addComponent(protocol_clear_Button)
                    .addComponent(protocolCopyButton)))
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addComponent(jLabel26)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(IRP_TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE))
        );
        analyzePanelLayout.setVerticalGroup(
            analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(analyzePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IRP_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(analyzePanelLayout.createSequentialGroup()
                        .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(analyzePanelLayout.createSequentialGroup()
                                .addComponent(protocol_generate_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(icf_import_Button))
                            .addGroup(analyzePanelLayout.createSequentialGroup()
                                .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(16, 16, 16)
                                .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(analyzePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(analyzePanelLayout.createSequentialGroup()
                                .addComponent(protocol_decode_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(protocolAnalyzeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(protocol_clear_Button))
                            .addGroup(analyzePanelLayout.createSequentialGroup()
                                .addComponent(protocol_send_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(protocol_stop_Button)))
                        .addGap(10, 10, 10)
                        .addComponent(protocolCopyButton))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        protocolsSubPane.addTab("Analyze", analyzePanel);

        protocolExportButton.setText("Export");
        protocolExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolExportButtonActionPerformed(evt);
            }
        });

        automaticFileNamesCheckBox.setText("Automatic File Names");
        automaticFileNamesCheckBox.setToolTipText("Perform export to a file with automatically generated name, Otherwise a file browser will be started.");

        exportTogglesGenerateCheckBox.setText("Generate toggle pairs");
        exportTogglesGenerateCheckBox.setToolTipText("For protocol with toggles, generate both versions in the export file.");

        lastFTextField.setToolTipText("Last F to export (inclusive)");
        lastFTextField.setMinimumSize(new java.awt.Dimension(35, 27));
        lastFTextField.setPreferredSize(new java.awt.Dimension(35, 27));
        lastFTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastFTextFieldActionPerformed(evt);
            }
        });
        lastFTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                lastFTextFieldFocusLost(evt);
            }
        });

        jLabel17.setText("Last F");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Text/Pronto", "Text/Raw", "XML", "LIRC", "Pronto CCF" }));

        jLabel20.setText("Export Format");

        exportdir_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_TextFieldActionPerformed(evt);
            }
        });
        exportdir_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                exportdir_TextFieldFocusLost(evt);
            }
        });

        exportdir_browse_Button.setText("...");
        exportdir_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel21.setText("Export directory");

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exportTogglesGenerateCheckBox)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel17)
                            .addComponent(jLabel21))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(protocolExportButton)
                                .addGroup(exportPanelLayout.createSequentialGroup()
                                    .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(exportdir_browse_Button)))))
                    .addComponent(automaticFileNamesCheckBox))
                .addContainerGap(91, Short.MAX_VALUE))
        );
        exportPanelLayout.setVerticalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(lastFTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportdir_browse_Button))
                .addGap(10, 10, 10)
                .addComponent(exportTogglesGenerateCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(automaticFileNamesCheckBox)
                    .addComponent(protocolExportButton))
                .addGap(79, 79, 79))
        );

        protocolsSubPane.addTab("Export", exportPanel);

        protocol_outputhw_ComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)", "LIRC" }));
        protocol_outputhw_ComboBox1.setToolTipText("Device used for when sending");

        commandno_TextField1.setToolTipText("F, Function number (also called Command number or OBC).");
        commandno_TextField1.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField1.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextField1ActionPerformed(evt);
            }
        });
        commandno_TextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextField1FocusLost(evt);
            }
        });

        jLabel27.setText("IR Device");

        jLabel28.setText("Last F");

        jLabel29.setText("Delay (s)");

        commandno_TextField2.setToolTipText("F, Function number (also called Command number or OBC).");
        commandno_TextField2.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField2.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextField2ActionPerformed(evt);
            }
        });
        commandno_TextField2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextField2FocusLost(evt);
            }
        });

        jButton2.setText("Start");

        jButton3.setText("Stop");

        jButton4.setText("Pause");

        jLabel30.setText("Current F");

        commandno_TextField3.setEditable(false);
        commandno_TextField3.setToolTipText("F, Function number (also called Command number or OBC).");
        commandno_TextField3.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField3.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextField3ActionPerformed(evt);
            }
        });
        commandno_TextField3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextField3FocusLost(evt);
            }
        });

        jButton5.setText("Edit");

        jButton6.setText("Clear");

        jButton7.setText("Save");

        jLabel31.setText("Notes:");

        jLabel32.setText(" ");

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText("Warning: Sending undocumented IR commands to your equipment may damage or even destroy it. By using this program, you agree to take the responsibility for possible damages yourself, and not to hold the author responsible.");
        jTextArea1.setToolTipText("You have been warned!");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout warDialerPanelLayout = new javax.swing.GroupLayout(warDialerPanel);
        warDialerPanel.setLayout(warDialerPanelLayout);
        warDialerPanelLayout.setHorizontalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addGap(18, 18, 18)
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton7))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel28)
                            .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel29))
                        .addGap(41, 41, 41)
                        .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(protocol_outputhw_ComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(warDialerPanelLayout.createSequentialGroup()
                                    .addComponent(commandno_TextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(146, 146, 146))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, warDialerPanelLayout.createSequentialGroup()
                                    .addComponent(commandno_TextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(97, 97, 97)
                                    .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(warDialerPanelLayout.createSequentialGroup()
                                            .addComponent(jLabel30)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(commandno_TextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(warDialerPanelLayout.createSequentialGroup()
                                            .addComponent(jButton2)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jButton3)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jButton4)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))))
                        .addGap(62, 62, 62))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 631, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(jLabel32))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE))
                .addContainerGap())
        );
        warDialerPanelLayout.setVerticalGroup(
            warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(warDialerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(protocol_outputhw_ComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(commandno_TextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30)
                    .addComponent(commandno_TextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(commandno_TextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(jButton5)
                    .addComponent(jButton6)
                    .addComponent(jButton7))
                .addGroup(warDialerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel32)
                        .addContainerGap(55, Short.MAX_VALUE))
                    .addGroup(warDialerPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))))
        );

        protocolsSubPane.addTab("War Dialer", warDialerPanel);

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

        jLabel2.setText("Renderer");

        jLabel3.setText("Protocol");

        jLabel4.setText("D");

        jLabel5.setText("S");

        jLabel10.setText("F");

        jLabel12.setText("T");

        jLabel13.setText("Additional Parameters");

        javax.swing.GroupLayout protocolsPanelLayout = new javax.swing.GroupLayout(protocolsPanel);
        protocolsPanel.setLayout(protocolsPanelLayout);
        protocolsPanelLayout.setHorizontalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, protocolsPanelLayout.createSequentialGroup()
                .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 645, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(DecodeIRVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(516, 516, 516))
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jLabel4)))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel5)))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel10)))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(protocol_params_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(648, 648, 648))
        );
        protocolsPanelLayout.setVerticalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel13)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rendererComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_params_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(DecodeIRVersion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(292, 292, 292))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocolsSubPane, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        mainTabbedPane.addTab("IR Protocols", protocolsPanel);

        gc_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gc_address_TextField.setText("192.168.1.70");
        gc_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        gc_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_address_TextFieldActionPerformed(evt);
            }
        });

        gc_module_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2" }));
        gc_module_ComboBox.setToolTipText("GlobalCache IR Module to use");
        gc_module_ComboBox.setMaximumSize(new java.awt.Dimension(40, 27));
        gc_module_ComboBox.setMinimumSize(new java.awt.Dimension(40, 27));
        gc_module_ComboBox.setPreferredSize(new java.awt.Dimension(40, 27));

        gc_connector_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        gc_connector_ComboBox.setToolTipText("GlobalCache IR Connector to use");
        gc_connector_ComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));

        gc_browse_Button.setText("Browse");
        gc_browse_Button.setToolTipText("Open selected GlobalCache in the browser.");
        gc_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_browse_ButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Stop IR");
        jButton1.setToolTipText("Send the selected GlobalCache the stopir command.");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_stop_ir_ActionPerformed(evt);
            }
        });

        discoverButton.setText("Discover");
        discoverButton.setToolTipText("Try to discover a GlobalCache on LAN. Takes up to 60 seconds!");
        discoverButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discoverButtonActionPerformed(evt);
            }
        });

        gcDiscoveredTypejTextField.setText("<unknown>");
        gcDiscoveredTypejTextField.setToolTipText("Type of discovered GlobalCache");
        gcDiscoveredTypejTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcDiscoveredTypejTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalcache_PanelLayout = new javax.swing.GroupLayout(globalcache_Panel);
        globalcache_Panel.setLayout(globalcache_PanelLayout);
        globalcache_PanelLayout.setHorizontalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_browse_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addGap(78, 78, 78)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(gcDiscoveredTypejTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                    .addComponent(discoverButton))
                .addContainerGap())
        );
        globalcache_PanelLayout.setVerticalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_browse_Button)
                    .addComponent(jButton1)
                    .addComponent(discoverButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gcDiscoveredTypejTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(204, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("GlobalCache", globalcache_Panel);

        irtrans_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        irtrans_address_TextField.setText("192.168.1.71");
        irtrans_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        irtrans_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_address_TextFieldActionPerformed(evt);
            }
        });

        irtrans_led_ComboBox.setMaximumRowCount(12);
        irtrans_led_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "intern", "extern", "both", "0", "1", "2", "3", "4", "5", "6", "7", "8" }));

        irtrans_browse_Button.setText("Browse");
        irtrans_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_browse_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtrans_PanelLayout = new javax.swing.GroupLayout(irtrans_Panel);
        irtrans_Panel.setLayout(irtrans_PanelLayout);
        irtrans_PanelLayout.setHorizontalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(irtrans_browse_Button)
                .addContainerGap(299, Short.MAX_VALUE))
        );
        irtrans_PanelLayout.setVerticalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_browse_Button))
                .addContainerGap(242, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("IRTrans", irtrans_Panel);

        LIRC_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        LIRC_address_TextField.setText("192.168.1.5");
        LIRC_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        LIRC_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        LIRC_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        LIRC_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LIRC_address_TextFieldActionPerformed(evt);
            }
        });

        LIRCStopIrButton.setText("Stop IR");
        LIRCStopIrButton.setToolTipText("Send the selected GlobalCache the stopir command.");
        LIRCStopIrButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LIRCStopIrButtongc_stop_ir_ActionPerformed(evt);
            }
        });

        LIRC_address_TextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        LIRC_address_TextField1.setText("8765");
        LIRC_address_TextField1.setToolTipText("Port number of LIRC server to use. Default is 8765.");
        LIRC_address_TextField1.setMinimumSize(new java.awt.Dimension(120, 27));
        LIRC_address_TextField1.setPreferredSize(new java.awt.Dimension(120, 27));
        LIRC_address_TextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LIRC_address_TextField1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalcache_Panel1Layout = new javax.swing.GroupLayout(globalcache_Panel1);
        globalcache_Panel1.setLayout(globalcache_Panel1Layout);
        globalcache_Panel1Layout.setHorizontalGroup(
            globalcache_Panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_Panel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(LIRC_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addComponent(LIRC_address_TextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(124, 124, 124)
                .addComponent(LIRCStopIrButton)
                .addContainerGap(230, Short.MAX_VALUE))
        );
        globalcache_Panel1Layout.setVerticalGroup(
            globalcache_Panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_Panel1Layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(globalcache_Panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LIRC_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LIRCStopIrButton)
                    .addComponent(LIRC_address_TextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(244, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("LIRC", globalcache_Panel1);

        mainTabbedPane.addTab("Output HW", outputHWTabbedPane);

        decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        decimal_TextField.setText("0");
        decimal_TextField.setToolTipText("Enter decimal number here, then press return.");
        decimal_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        decimal_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decimal_TextFieldActionPerformed(evt);
            }
        });
        decimal_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                decimal_TextFieldFocusLost(evt);
            }
        });

        hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        hex_TextField.setText("0");
        hex_TextField.setToolTipText("Enter hexadecimal number here, then press return.");
        hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        hex_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hex_TextFieldActionPerformed(evt);
            }
        });
        hex_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                hex_TextFieldFocusLost(evt);
            }
        });

        complement_decimal_TextField.setEditable(false);
        complement_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_decimal_TextField.setText("255");
        complement_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        complement_hex_TextField.setEditable(false);
        complement_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_hex_TextField.setText("FF");
        complement_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        complement_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        jLabel6.setText("Decimal");

        jLabel7.setText("Hex");

        jLabel8.setText("Complement");

        jLabel9.setText("Complement");

        jLabel14.setText("Reverse");

        reverse_hex_TextField.setEditable(false);
        reverse_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_hex_TextField.setText("FF");
        reverse_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        reverse_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        reverse_decimal_TextField.setEditable(false);
        reverse_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_decimal_TextField.setText("255");
        reverse_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        jLabel15.setText("Reverse");

        no_periods_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        no_periods_TextField.setText("1");
        no_periods_TextField.setToolTipText("Number of periods");
        no_periods_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                no_periods_TextFieldActionPerformed(evt);
            }
        });
        no_periods_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                no_periods_TextFieldFocusLost(evt);
            }
        });

        frequency_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        frequency_TextField.setText("40000");
        frequency_TextField.setToolTipText("Enter modulation frequency here, then press return.");
        frequency_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        frequency_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        frequency_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frequency_TextFieldActionPerformed(evt);
            }
        });
        frequency_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                frequency_TextFieldFocusLost(evt);
            }
        });

        jLabel22.setText("Frequency (Hz)");

        jLabel23.setText("# Periods");

        time_TextField.setEditable(false);
        time_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        time_TextField.setText("0");
        time_TextField.setToolTipText("Time interval = # periods/frequency");
        time_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        time_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        time_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                time_TextFieldActionPerformed(evt);
            }
        });
        time_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                time_TextFieldFocusLost(evt);
            }
        });

        jLabel24.setText("Time (us)");

        prontocode_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        prontocode_TextField.setText("0");
        prontocode_TextField.setToolTipText("Enter Pronto frequency code here, then press return.");
        prontocode_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontocode_TextFieldActionPerformed(evt);
            }
        });
        prontocode_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontocode_TextFieldFocusLost(evt);
            }
        });

        jLabel25.setText("Prontocode");

        period_selection_enable_CheckBox.setSelected(true);
        period_selection_enable_CheckBox.setText("Enable");
        period_selection_enable_CheckBox.setToolTipText("Select periods or time interval");
        period_selection_enable_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                period_selection_enable_CheckBoxActionPerformed(evt);
            }
        });

        jSeparator6.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout hexcalcPanelLayout = new javax.swing.GroupLayout(hexcalcPanel);
        hexcalcPanel.setLayout(hexcalcPanelLayout);
        hexcalcPanelLayout.setHorizontalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jLabel9)
                            .addComponent(jLabel7)
                            .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25)
                            .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(period_selection_enable_CheckBox))
                    .addComponent(jLabel23)
                    .addComponent(jLabel24)
                    .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(171, Short.MAX_VALUE))
        );
        hexcalcPanelLayout.setVerticalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(jSeparator6, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE))
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel22)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(jLabel9))
                    .addComponent(jLabel23))
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 132, Short.MAX_VALUE)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(period_selection_enable_CheckBox))
                        .addGap(5, 5, 5)
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(75, 75, 75))
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())))
        );

        mainTabbedPane.addTab("IRcalc", hexcalcPanel);

        jLabel16.setText("IRP Protocols");

        homeconf_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeconf_TextFieldActionPerformed(evt);
            }
        });
        homeconf_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                homeconf_TextFieldFocusLost(evt);
            }
        });

        home_select_Button.setText("...");
        home_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsSelect(evt);
            }
        });

        home_browse_Button.setText("Browse");
        home_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsBrowse(evt);
            }
        });

        home_load_Button.setText("Load");
        home_load_Button.setEnabled(false);

        makehexIrpDir_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        makehexIrpDir_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        makehexIrpDir_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        makehexIrpDir_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                makehexIrpDir_TextFieldFocusLost(evt);
            }
        });

        browser_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        browser_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        browser_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        browser_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browser_TextFieldActionPerformed(evt);
            }
        });
        browser_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                browser_TextFieldFocusLost(evt);
            }
        });

        jLabel18.setText("Browser");

        macro_select_Button.setText("...");
        macro_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makehexIrpDirSelect(evt);
            }
        });

        browser_select_Button.setText("...");
        browser_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browser_select_ButtonActionPerformed(evt);
            }
        });

        macro_browse_Button.setText("Browse");
        macro_browse_Button.setEnabled(false);

        jLabel1.setText("Makehex IRP dir.");

        javax.swing.GroupLayout general_PanelLayout = new javax.swing.GroupLayout(general_Panel);
        general_Panel.setLayout(general_PanelLayout);
        general_PanelLayout.setHorizontalGroup(
            general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(15, 15, 15))
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(homeconf_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_select_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_browse_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_load_Button))
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(makehexIrpDir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(browser_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(general_PanelLayout.createSequentialGroup()
                                .addComponent(macro_select_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(macro_browse_Button))
                            .addComponent(browser_select_Button))))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        general_PanelLayout.setVerticalGroup(
            general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(homeconf_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(home_select_Button)
                    .addComponent(home_browse_Button)
                    .addComponent(home_load_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(makehexIrpDir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(macro_select_Button)
                    .addComponent(macro_browse_Button)
                    .addComponent(jLabel1))
                .addGap(39, 39, 39)
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(browser_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(browser_select_Button))
                .addContainerGap(174, Short.MAX_VALUE))
        );

        optsTabbedPane.addTab("General", general_Panel);

        jLabel19.setText("Export dir");

        javax.swing.GroupLayout general_export_opts_PanelLayout = new javax.swing.GroupLayout(general_export_opts_Panel);
        general_export_opts_Panel.setLayout(general_export_opts_PanelLayout);
        general_export_opts_PanelLayout.setHorizontalGroup(
            general_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_export_opts_PanelLayout.createSequentialGroup()
                .addComponent(jLabel19)
                .addContainerGap(547, Short.MAX_VALUE))
        );
        general_export_opts_PanelLayout.setVerticalGroup(
            general_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_export_opts_PanelLayout.createSequentialGroup()
                .addGap(298, 298, 298)
                .addComponent(jLabel19)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exportopts_TabbedPane.addTab("General", general_export_opts_Panel);

        ccf_export_prontomodel_ComboBox.setMaximumRowCount(14);
        ccf_export_prontomodel_ComboBox.setModel(new DefaultComboBoxModel(prontomodelnames));
        ccf_export_prontomodel_ComboBox.setToolTipText("Pronto Model");
        ccf_export_prontomodel_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccf_export_prontomodel_ComboBoxActionPerformed(evt);
            }
        });

        ccf_export_raw_CheckBox.setSelected(true);
        ccf_export_raw_CheckBox.setText("Raw Codes");
        ccf_export_raw_CheckBox.setToolTipText("Prohibit cooked codes in CCF export");

        ccf_export_screenwidth_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_screenwidth_TextField.setText("240");
        ccf_export_screenwidth_TextField.setToolTipText("Screen width (pixels)");
        ccf_export_screenwidth_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenwidth_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenwidth_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        ccf_export_screenheight_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_screenheight_TextField.setText("220");
        ccf_export_screenheight_TextField.setToolTipText("Screen height (pixels)");
        ccf_export_screenheight_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenheight_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenheight_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        ccf_export_buttonwidth_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_buttonwidth_TextField.setText("60");
        ccf_export_buttonwidth_TextField.setToolTipText("Button width (pixels)");
        ccf_export_buttonwidth_TextField.setMaximumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonwidth_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonwidth_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        ccf_export_buttonheight_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_buttonheight_TextField.setText("30");
        ccf_export_buttonheight_TextField.setToolTipText("Button height (pixels)");
        ccf_export_buttonheight_TextField.setMaximumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonheight_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonheight_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        ccf_export_export_Button.setText("Export");

        javax.swing.GroupLayout ccf_export_opts_PanelLayout = new javax.swing.GroupLayout(ccf_export_opts_Panel);
        ccf_export_opts_Panel.setLayout(ccf_export_opts_PanelLayout);
        ccf_export_opts_PanelLayout.setHorizontalGroup(
            ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccf_export_opts_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ccf_export_prontomodel_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_screenwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_screenheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(ccf_export_buttonwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_buttonheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ccf_export_raw_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 167, Short.MAX_VALUE)
                .addComponent(ccf_export_export_Button)
                .addContainerGap())
        );
        ccf_export_opts_PanelLayout.setVerticalGroup(
            ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccf_export_opts_PanelLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ccf_export_prontomodel_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_screenwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_screenheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_buttonwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_buttonheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_raw_CheckBox)
                    .addComponent(ccf_export_export_Button))
                .addContainerGap(222, Short.MAX_VALUE))
        );

        exportopts_TabbedPane.addTab("CCF", ccf_export_opts_Panel);

        optsTabbedPane.addTab("Exportopts", exportopts_TabbedPane);

        jLabel11.setText("Debugcode");

        debug_TextField.setText("0");
        debug_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        verbose_CheckBox.setText("Verbose");
        verbose_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout debug_PanelLayout = new javax.swing.GroupLayout(debug_Panel);
        debug_Panel.setLayout(debug_PanelLayout);
        debug_PanelLayout.setHorizontalGroup(
            debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debug_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(debug_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(verbose_CheckBox))
                .addContainerGap(475, Short.MAX_VALUE))
        );
        debug_PanelLayout.setVerticalGroup(
            debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debug_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(verbose_CheckBox)
                .addContainerGap(248, Short.MAX_VALUE))
        );

        optsTabbedPane.addTab("Debug", debug_Panel);

        mainTabbedPane.addTab("Options", optsTabbedPane);

        console_TextArea.setColumns(20);
        console_TextArea.setEditable(false);
        console_TextArea.setLineWrap(true);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is the console, where errors and messages go, instead of annoying you with popups.");
        console_TextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(console_TextArea);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save properties");
        saveMenuItem.setToolTipText("Save properites");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save properties as ...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(jSeparator4);

        consoletext_save_MenuItem.setMnemonic('c');
        consoletext_save_MenuItem.setText("Save console text as...");
        consoletext_save_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletext_save_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(consoletext_save_MenuItem);
        fileMenu.add(jSeparator1);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        copy_console_to_clipboard_MenuItem.setText("Copy Console to clipboard");
        copy_console_to_clipboard_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_console_to_clipboard_MenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copy_console_to_clipboard_MenuItem);

        clear_console_MenuItem.setText("Clear console");
        clear_console_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_console_MenuItemActionPerformed(evt);
            }
        });
        editMenu.add(clear_console_MenuItem);

        menuBar.add(editMenu);

        jMenu1.setMnemonic('O');
        jMenu1.setText("Options");

        verbose_CheckBoxMenuItem.setMnemonic('v');
        verbose_CheckBoxMenuItem.setText("Verbose");
        verbose_CheckBoxMenuItem.setToolTipText("Report actual command sent to devices");
        verbose_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(verbose_CheckBoxMenuItem);

        menuBar.add(jMenu1);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        contentMenuItem.setMnemonic('C');
        contentMenuItem.setText("Content...");
        contentMenuItem.setToolTipText("Brings up documentation.");
        contentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentMenuItem);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
        aboutMenuItem.setToolTipText("The mandatory About popup");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 655, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 650, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 409, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class globalcache_thread extends Thread {
        private IrSignal code;
        private int module;
        private int connector;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public globalcache_thread(IrSignal code, int module, int connector, int count,
                JButton start_button, JButton stop_button) {
            super("globalcache_thread");
            this.code = code;
            this.module = module;
            this.connector = connector;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
            boolean success = false;
            try {
                success = gc.send_ir(code, module, connector, count);
            } catch (UnknownHostException ex) {
                System.err.println("Globalcache hostname is not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted *** ");
                success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            //the_globalcache_thread = null;
            start_button.setEnabled(true);
            stop_button.setEnabled(false);
        }
    }

    private class irtrans_thread extends Thread {
        private String remote;
        private String commandname;
        private irtrans.led_t led;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public irtrans_thread(String remote, String commandname, irtrans.led_t led, int count,
                JButton start_button, JButton stop_button) {
            super("irtrans_thread");
            this.remote = remote;
            this.commandname = commandname;
            this.led = led;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
            boolean success = false;
            try {
                success = irt.send_flashed_command(remote, commandname, led, count);
            } catch (UnknownHostException ex) {
                System.err.println("IRTrans hostname not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted *** ");
                success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            the_irtrans_thread = null;
            start_button.setEnabled(true);
            stop_button.setEnabled(false);
        }
    }

    private void do_exit() {
        System.out.println("Exiting...");
        System.exit(0);
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        do_exit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            String result = Props.get_instance().save();
            System.err.println(result == null ? "No need to save properties." : ("Property file written to " + result + "."));
        } catch (Exception e) {
            warning("Problems saving properties: " + e.getMessage());
            return;
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            //JFrame mainFrame = gui_main.getApplication().getMainFrame();
            aboutBox = new AboutPopup(this/*mainFrame*/, false);
            aboutBox.setLocationRelativeTo(/*mainFrame*/this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentMenuItemActionPerformed
        harcutils.browse(Props.get_instance().get_helpfilename());
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = select_file("Select properties save", "xml", "XML Files", true, null).getAbsolutePath();
            Props.get_instance().save(props);
            System.err.println("Property file written to " + props + ".");
        } catch (IOException e) {
            System.err.println(e);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void update_verbosity() {
        UserPrefs.get_instance().set_verbose(verbose);
        gc.set_verbosity(verbose);
        irt.set_verbosity(verbose);
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
    }

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        verbose = verbose_CheckBoxMenuItem.isSelected();
        update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed


    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getText());
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.out.println("asfkad");//do_exit();
    }//GEN-LAST:event_formWindowClosed

    private IrSignal extract_code() throws NumberFormatException, IrpMasterException, RecognitionException {
        String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
        short devno = deviceno_TextField.getText().trim().isEmpty() ? -1 : harcutils.parse_shortnumber(deviceno_TextField.getText());
        short sub_devno = -1;
        Protocol protocol = get_protocol(protocol_name);
        if (protocol.hasParameter("S") && !(protocol.hasParameterDefault("S") && subdevice_TextField.getText().trim().equals("")))
            sub_devno = harcutils.parse_shortnumber(subdevice_TextField.getText());
        if (IrpUtils.parseUpper(commandno_TextField.getText()) != IrpUtils.invalid) {
            System.err.println("Interval in command number not allowed here.");
            return null;
        }
        short cmd_no = harcutils.parse_shortnumber(commandno_TextField.getText());
        String tog = (String) toggle_ComboBox.getModel().getSelectedItem();
        toggletype toggle = toggletype.decode_toggle((String) toggle_ComboBox.getModel().getSelectedItem());
        String add_params = protocol_params_TextField.getText();
        //System.err.println(protocol_name + devno + " " + sub_devno + " " + cmd_no + toggle);
        
        if (protocol == null)
            return null;
        
        HashMap<String, Long> params = //parameters(deviceno, subdevice, cmdno, toggle, extra_params);
                                     new HashMap<String, Long>();
        if (devno != invalid_parameter)
            params.put("D", (long) devno);
        if (sub_devno != invalid_parameter)
            params.put("S", (long) sub_devno);
        if (cmd_no != invalid_parameter)
            params.put("F", (long) cmd_no);
        if (toggle != toggletype.dont_care)
            params.put("T", (long) toggletype.toInt(toggle));
        if (add_params != null && !add_params.trim().isEmpty()) {
            String[] str = add_params.trim().split("[ \t]+");
            for (String s : str) {
                String[] q = s.split("=");
                if (q.length == 2)
                    params.put(q[0], IrpUtils.parseLong(q[1]));
            }
        }
        IrSignal irSignal = protocol.renderIrSignal(params);
        return irSignal;//protocol.encode(protocol_name, devno, sub_devno, cmd_no, toggle, add_params, false);
    }
    
    private void export_ccf() throws NumberFormatException, IrpMasterException, RecognitionException, FileNotFoundException {
 /*       String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
        short devno = deviceno_TextField.getText().trim().isEmpty() ? -1 : harcutils.parse_shortnumber(deviceno_TextField.getText());
        short sub_devno = -1;
        if (protocol.has_subdevice(protocol_name) && !(protocol.subdevice_optional(protocol_name) && subdevice_TextField.getText().trim().equals("")))
            sub_devno = harcutils.parse_shortnumber(subdevice_TextField.getText());
        short cmd_no_upper = (short) IrpUtils.parseUpper(commandno_TextField.getText());
        short cmd_no_lower = (short) IrpUtils.parseLong(commandno_TextField.getText());
        if (cmd_no_upper == (short) IrpUtils.invalid)
            cmd_no_upper = cmd_no_lower;
        toggletype toggle = (toggletype) toggle_ComboBox.getModel().getSelectedItem();
        String add_params = protocol_params_TextField.getText();
        File file = harcutils.create_export_file(Props.get_instance().get_exportdir(),
                protocol_name + "_" + devno + (sub_devno != -1 ? ("_" + sub_devno) : ""),
                "hex");
        PrintStream export_file = new PrintStream(file);
        System.err.println("Exporting to " + file);
        for (short cmd_no = cmd_no_lower; cmd_no <= cmd_no_upper; cmd_no++) {
            export_file.println("Device Code: " + devno + (sub_devno != -1 ? ("." + sub_devno) : "") + ", Function: " + cmd_no + " " + add_params);
            export_file.println(protocol.encode(protocol_name, devno, sub_devno, cmd_no, toggle, add_params, false).ccfString());
        }
        export_file.close();*/
    }

    private void update_protocol_parameters() {
        if (irpMaster == null)
            return;
        try {
            deviceno_TextField.setText("0");
            commandno_TextField.setText("0");
            toggle_ComboBox.setSelectedItem(toggletype.dont_care);
           
            Protocol protocol = get_protocol((String)protocol_ComboBox.getModel().getSelectedItem());
            subdevice_TextField.setEnabled(protocol.hasParameter("S"));
            if (protocol.hasParameter("S") && !protocol.hasParameterDefault("S"))
                subdevice_TextField.setText("0");
            else
                subdevice_TextField.setText(null);
            toggle_ComboBox.setEnabled(protocol.hasParameter("T"));
            IRP_TextField.setText(protocol.getIrp());
        } catch (UnassignedException ex) {
            subdevice_TextField.setEnabled(false);
            toggle_ComboBox.setEnabled(false);
        } catch (RecognitionException ex) {
            subdevice_TextField.setEnabled(false);
            toggle_ComboBox.setEnabled(false);
        }
    }

    private void consoletext_save_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoletext_save_MenuItemActionPerformed
        try {
            String filename = select_file("Save console text as...", "txt", "Text file", true, null).getAbsolutePath();
            PrintStream ps = new PrintStream(new FileOutputStream(filename));
            ps.println(console_TextArea.getText());
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_consoletext_save_MenuItemActionPerformed

    private void verbose_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxActionPerformed
        verbose = this.verbose_CheckBox.isSelected();
	update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxActionPerformed

    private void ccf_export_prontomodel_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ccf_export_prontomodel_ComboBoxActionPerformed
        com.neuron.app.tonto.ProntoModel prontomodel = com.neuron.app.tonto.ProntoModel.getModelByName((String) ccf_export_prontomodel_ComboBox.getModel().getSelectedItem());
	Dimension size = prontomodel.getScreenSize();
	this.ccf_export_screenwidth_TextField.setText(Integer.toString(size.width));
	this.ccf_export_screenheight_TextField.setText(Integer.toString(size.height));
	/*System.err.println(prontomodel + " " + size.height + " "+size.width);*/
    }//GEN-LAST:event_ccf_export_prontomodel_ComboBoxActionPerformed

    private void exportdir_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_browse_ButtonActionPerformed

        try {
	    String dir = select_file("Select export directory", null, null, false, ((new File(Props.get_instance().get_exportdir())).getAbsoluteFile().getParent())).getAbsolutePath();
	    Props.get_instance().set_exportdir(dir);
	    exportdir_TextField.setText(dir);
	} catch (NullPointerException e) {
	}
    }//GEN-LAST:event_exportdir_browse_ButtonActionPerformed

    private void exportdir_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exportdir_TextFieldFocusLost
        Props.get_instance().set_exportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldFocusLost

    private void exportdir_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_TextFieldActionPerformed
        Props.get_instance().set_exportdir(exportdir_TextField.getText());
     }//GEN-LAST:event_exportdir_TextFieldActionPerformed

    private void browser_select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browser_select_ButtonActionPerformed
        String filename = select_file("Select browser program", "exe", "exe-files", false, null).getAbsolutePath();
         if (filename != null) {
             this.browser_TextField.setText(filename);
             Props.get_instance().set_browser(filename);
         }
    }//GEN-LAST:event_browser_select_ButtonActionPerformed

    private void browser_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_browser_TextFieldFocusLost
        Props.get_instance().set_browser(browser_TextField.getText());
     }//GEN-LAST:event_browser_TextFieldFocusLost

    private void browser_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browser_TextFieldActionPerformed
        Props.get_instance().set_browser(browser_TextField.getText());
    }//GEN-LAST:event_browser_TextFieldActionPerformed

    private void makehexIrpDir_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_makehexIrpDir_TextFieldFocusLost

    }//GEN-LAST:event_makehexIrpDir_TextFieldFocusLost

    private void homeconf_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_homeconf_TextFieldFocusLost

        Props.get_instance().set_homefilename(homeconf_TextField.getText());
     }//GEN-LAST:event_homeconf_TextFieldFocusLost

    private void homeconf_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeconf_TextFieldActionPerformed

        Props.get_instance().set_homefilename(homeconf_TextField.getText());
     }//GEN-LAST:event_homeconf_TextFieldActionPerformed

    private void period_selection_enable_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_period_selection_enable_CheckBoxActionPerformed
        boolean mystate = period_selection_enable_CheckBox.isSelected();
         no_periods_TextField.setEditable(mystate); 
        time_TextField.setEditable(!mystate); 
    }//GEN-LAST:event_period_selection_enable_CheckBoxActionPerformed

    private void prontocode_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontocode_TextFieldFocusLost
        update_from_frequencycode();
     }//GEN-LAST:event_prontocode_TextFieldFocusLost

    private void prontocode_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontocode_TextFieldActionPerformed
        update_from_frequencycode();
     }//GEN-LAST:event_prontocode_TextFieldActionPerformed

    private void time_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_time_TextFieldFocusLost
        time_TextFieldActionPerformed(null);
     }//GEN-LAST:event_time_TextFieldFocusLost

    private void time_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_time_TextFieldActionPerformed
        int time = Integer.parseInt(time_TextField.getText());
	int freq = Integer.parseInt(frequency_TextField.getText());
	no_periods_TextField.setText(String.format("%.1f", time * freq / 1000000.0));
     }//GEN-LAST:event_time_TextFieldActionPerformed

    private void frequency_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_frequency_TextFieldFocusLost
        update_from_frequency();
     }//GEN-LAST:event_frequency_TextFieldFocusLost

    private void frequency_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frequency_TextFieldActionPerformed
        update_from_frequency();
     }//GEN-LAST:event_frequency_TextFieldActionPerformed

    private void no_periods_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_TextFieldFocusLost
        no_periods_TextFieldActionPerformed(null);
     }//GEN-LAST:event_no_periods_TextFieldFocusLost

    private void no_periods_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_TextFieldActionPerformed
        double no_periods = Double.parseDouble(no_periods_TextField.getText());
         int freq = Integer.parseInt(frequency_TextField.getText());
         time_TextField.setText(Integer.toString((int) (1000000.0 * no_periods / freq)));
     }//GEN-LAST:event_no_periods_TextFieldActionPerformed

    private void hex_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_hex_TextFieldFocusLost
        hex_TextFieldActionPerformed(null);
     }//GEN-LAST:event_hex_TextFieldFocusLost

    private void hex_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hex_TextFieldActionPerformed
        try {
             int in = Integer.parseInt(hex_TextField.getText(), 16);
             decimal_TextField.setText(Integer.toString(in));
             update_hexcalc(in);
         } catch (NumberFormatException e) {
             decimal_TextField.setText("*");
             hexcalc_silly_number(e);
         }
     }//GEN-LAST:event_hex_TextFieldActionPerformed

    private void decimal_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_decimal_TextFieldFocusLost
        decimal_TextFieldActionPerformed(null);
     }//GEN-LAST:event_decimal_TextFieldFocusLost

    private void decimal_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decimal_TextFieldActionPerformed
        try {
             int in = Integer.parseInt(decimal_TextField.getText());
             hex_TextField.setText(Integer.toHexString(in));
             update_hexcalc(in);
         } catch (NumberFormatException e) {
             hex_TextField.setText("*");
             hexcalc_silly_number(e);
         }
     }//GEN-LAST:event_decimal_TextFieldActionPerformed

    private void LIRC_address_TextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LIRC_address_TextField1ActionPerformed
        
    }//GEN-LAST:event_LIRC_address_TextField1ActionPerformed

    private void LIRCStopIrButtongc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LIRCStopIrButtongc_stop_ir_ActionPerformed
	
	}//GEN-LAST:event_LIRCStopIrButtongc_stop_ir_ActionPerformed

        private void LIRC_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LIRC_address_TextFieldActionPerformed
	
	}//GEN-LAST:event_LIRC_address_TextFieldActionPerformed

    private void irtrans_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_browse_ButtonActionPerformed
        harcutils.browse(irtrans_address_TextField.getText());
     }//GEN-LAST:event_irtrans_browse_ButtonActionPerformed

    private void irtrans_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_address_TextFieldActionPerformed
        irt = new irtrans(irtrans_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
     }//GEN-LAST:event_irtrans_address_TextFieldActionPerformed

    private void gcDiscoveredTypejTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcDiscoveredTypejTextFieldActionPerformed
	
    }//GEN-LAST:event_gcDiscoveredTypejTextFieldActionPerformed

    private void discoverButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discoverButtonActionPerformed
        System.err.println("Now trying to discover a GlobalCache on LAN. This may take up to 60 seconds.");
	amx_beacon.result beacon = globalcache.listen_beacon();
	String gcHostname = beacon.addr.getCanonicalHostName();
	gc_address_TextField.setText(gcHostname);
	gcDiscoveredTypejTextField.setText(beacon.table.get("-Model"));
	gc_address_TextFieldActionPerformed(null);
    }//GEN-LAST:event_discoverButtonActionPerformed

    private void gc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_stop_ir_ActionPerformed
        try {
	    gc.stop_ir(get_gc_module(), get_gc_connector());
	} catch (UnknownHostException ex) {
	    System.err.println(ex.getMessage());
	} catch (IOException ex) {
	    System.err.println(ex.getMessage());
	} catch (InterruptedException ex) {
	    System.err.println(ex.getMessage());
	}
    }//GEN-LAST:event_gc_stop_ir_ActionPerformed

    private void gc_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_browse_ButtonActionPerformed
        harcutils.browse(gc_address_TextField.getText());
    }//GEN-LAST:event_gc_browse_ButtonActionPerformed

    private void gc_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_address_TextFieldActionPerformed
        gc = new globalcache(gc_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
	try {
	    gc_module_ComboBox.setEnabled(false);
	    gc_connector_ComboBox.setEnabled(false);
	    String devs = gc.getdevices();
	    String[] dvs = devs.split("\n");
	    String[] s = new String[dvs.length];
	    for (int i = 0; i < s.length; i++) {
		s[i] = dvs[i].endsWith("IR") ? dvs[i].substring(7, 8) : null;
	    }
	    String[] modules = harcutils.nonnulls(s);
	    gc_modules_dcbm = new DefaultComboBoxModel(modules != null ? modules : new String[]{"-"});
	    gc_module_ComboBox.setModel(gc_modules_dcbm);
	    gc_module_ComboBox.setEnabled(modules != null);
	    gc_connector_ComboBox.setEnabled(modules != null);
	} catch (UnknownHostException e) {
	    gc = null;
	    System.err.println(e.getMessage());
	} catch (IOException e) {
	    gc = null;
	    System.err.println(e.getMessage());
	} catch (InterruptedException e) {
	    gc = null;
	    System.err.println(e.getMessage());
	}
	/*deviceclass_send_Button.setEnabled(gc != null);*/
	protocol_send_Button.setEnabled(gc != null);
    }//GEN-LAST:event_gc_address_TextFieldActionPerformed

    private void protocolExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolExportButtonActionPerformed
        try {
	    export_ccf();
	} catch (NumberFormatException ex) {
	    System.err.println(ex.getMessage());
	} catch (IrpMasterException ex) {
	    System.err.println(ex.getMessage());
	} catch (RecognitionException ex) {
	    System.err.println(ex.getMessage());
	} catch (FileNotFoundException ex) {
	    System.err.println(ex.getMessage());
	}
    }//GEN-LAST:event_protocolExportButtonActionPerformed

    private void icf_import_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_icf_import_ButtonActionPerformed
        File file = this.select_file("Select ict file", "ict", "ict Files", false, null);
	if (file != null) {
	    try {
		if (verbose)
		    System.err.println("Imported " + file.getName());
		IrSignal ip = ICT.parse(file);
		protocol_raw_TextArea.setText(ip.ccfString());
		this.protocol_send_Button.setEnabled(true);
		this.protocol_decode_Button.setEnabled(true);
		this.protocol_clear_Button.setEnabled(true);
                this.protocolCopyButton.setEnabled(true);
                this.protocolAnalyzeButton.setEnabled(true);
	    } catch (IncompatibleArgumentException ex) {
		System.err.println(ex.getMessage());
	    } catch (FileNotFoundException ex) {
		System.err.println(ex);
	    } catch (IOException ex) {
		System.err.println(ex);
	    }
	}
    }//GEN-LAST:event_icf_import_ButtonActionPerformed

    private void protocol_clear_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_clear_ButtonActionPerformed

        protocol_raw_TextArea.setText(null);
	/*/protocol_params_TextField.setText(null);*/
	protocol_clear_Button.setEnabled(false);
        protocolCopyButton.setEnabled(false);
        protocolAnalyzeButton.setEnabled(false);
	protocol_decode_Button.setEnabled(false);
	protocol_send_Button.setEnabled(false);
    }//GEN-LAST:event_protocol_clear_ButtonActionPerformed

    private void protocol_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_stop_ButtonActionPerformed
	
        try {
	    if (the_globalcache_protocol_thread != null)
		this.the_globalcache_protocol_thread.interrupt();
	    gc.stop_ir(this.get_gc_module(), this.get_gc_connector());
	} catch (UnknownHostException e) {
            System.err.println(e);
	} catch (IOException e) {
	    System.err.println(e);
	} catch (InterruptedException e) {
	    System.err.println(e);
	}
    }//GEN-LAST:event_protocol_stop_ButtonActionPerformed

    private void protocol_decode_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_decode_ButtonActionPerformed
        String /*/code = protocol_params_TextField.getText().trim();*/
	       /*/if (code == null || code.equals(""))*/
                 code = protocol_raw_TextArea.getText().trim();
	try {
	    /*/com.hifiremote.decodeir.DecodeIR.DecodedSignal[] result = com.hifiremote.decodeir.DecodeIR.decode(code);*/
             DecodeIR.DecodedSignal[] result = DecodeIR.decodePronto(code);
             if (result == null || result.length == 0) {
                 System.err.println("DecodeIR failed (but was found).");
                 return;
             }
             for (int i = 0; i < result.length; i++) {
                 System.err.println(result[i]);
             }
	} catch (UnsatisfiedLinkError e) {
	    System.err.println("Error: DecodeIR not found.");
	} catch (NumberFormatException e) {
	    System.err.println("Parse error in string; " + e.getMessage());
	}
    }//GEN-LAST:event_protocol_decode_ButtonActionPerformed

    private void protocol_raw_TextAreaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaFocusLost
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_raw_TextAreaFocusLost

    private void protocol_params_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_protocol_params_TextFieldFocusLost
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_params_TextFieldFocusLost

    private void protocol_params_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_params_TextFieldActionPerformed
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_params_TextFieldActionPerformed

    private void protocol_generate_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_generate_ButtonActionPerformed
        try {
	    IrSignal code = extract_code();
	    if (code == null)
		return;
	    protocol_raw_TextArea.setText(code.ccfString());
	    /*/protocol_cooked_TextField.setText(code.cooked_ccf_string());*/
	    protocol_decode_Button.setEnabled(true);
	    protocol_clear_Button.setEnabled(true);
            protocolCopyButton.setEnabled(true);
            protocolAnalyzeButton.setEnabled(true);
	    protocol_send_Button.setEnabled(true);
	} catch (RecognitionException ex) {
	    System.err.println(ex.getMessage());
	} catch (IrpMasterException ex) {
	    System.err.println(ex.getMessage());
	} catch (NumberFormatException e) {
	    System.err.println("Parse error " + e.getMessage());
	}
    }//GEN-LAST:event_protocol_generate_ButtonActionPerformed

    private void protocol_send_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_send_ButtonActionPerformed
        int count = Integer.parseInt((String) no_sends_protocol_ComboBox.getModel().getSelectedItem());
	boolean use_globalcache = protocol_outputhw_ComboBox.getSelectedIndex() == 0;
	try {
	    String ccf = protocol_raw_TextArea.getText();
	    if (ccf == null || ccf.trim().equals("")) {
		/*/ Take code from the upper row, ignoring text areas*/
		IrSignal code = extract_code();
		if (code == null)
		    return;
		if (use_globalcache) {
		    /*/gc.send_ir(code, get_gc_module(), get_gc_connector(), count);*/
		    if (the_globalcache_protocol_thread != null)
			System.err.println("Warning: the_globalcache_protocol_thread != null");
		    the_globalcache_protocol_thread = new globalcache_thread(code, get_gc_module(), get_gc_connector(), count, protocol_send_Button, protocol_stop_Button);
		    the_globalcache_protocol_thread.start();
		} else
		    irt.send_ir(code, get_irtrans_led(), count);
	    } else {
		/*/ Take code from the text area */
		if (use_globalcache)
		    gc.send_ir(ccf, get_gc_module(), get_gc_connector(), count);
		else
		    irt.send_ir(ccf, get_irtrans_led(), count);
	    }
	} catch (IrpMasterException e) {
	    System.err.println(e.getMessage());
	} catch (NumberFormatException e) {
	    System.err.println("Parse error " + e.getMessage());
	} catch (UnknownHostException e) {
	    System.err.println(e.getMessage());
	} catch (IOException e) {
	    System.err.println(e.getMessage());
	} catch (InterruptedException e) {
	    System.err.println(e.getMessage());/*/ FIXME*/
	} catch (RecognitionException e) {
	    System.err.println(e.getMessage());
	}
    }//GEN-LAST:event_protocol_send_ButtonActionPerformed

    private void commandno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextFieldFocusLost
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldFocusLost

    private void commandno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldActionPerformed

    private void deviceno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_deviceno_TextFieldFocusLost
	possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldFocusLost

    private void deviceno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldActionPerformed

    private void protocol_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_ComboBoxActionPerformed
        update_protocol_parameters();
    }//GEN-LAST:event_protocol_ComboBoxActionPerformed

    private void irpProtocolsBrowse(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsBrowse
        
    }//GEN-LAST:event_irpProtocolsBrowse

    private void irpProtocolsSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsSelect
        
    }//GEN-LAST:event_irpProtocolsSelect

    private void makehexIrpDirSelect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makehexIrpDirSelect
        
    }//GEN-LAST:event_makehexIrpDirSelect

    private void rendererComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rendererComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rendererComboBoxActionPerformed

    private void lastFTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastFTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lastFTextFieldActionPerformed

    private void lastFTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lastFTextFieldFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_lastFTextFieldFocusLost

    private void commandno_TextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField1ActionPerformed

    private void commandno_TextField1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextField1FocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField1FocusLost

    private void commandno_TextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField2ActionPerformed

    private void commandno_TextField2FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextField2FocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField2FocusLost

    private void commandno_TextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField3ActionPerformed

    private void commandno_TextField3FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextField3FocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_commandno_TextField3FocusLost

    private void protocolAnalyzeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolAnalyzeButtonActionPerformed
        String code = protocol_raw_TextArea.getText().trim();
	try {
	     IrSignal irSignal = Pronto.ccfSignal(code);
             Analyzer analyzer = new Analyzer(irSignal);
             System.out.println("Analyzer result: " + analyzer.getIrpWithAltLeadout());
	} catch (IrpMasterException e) {
	    System.err.println(e.getMessage());
	} catch (NumberFormatException e) {
	    System.err.println("Parse error in string; " + e.getMessage());
	}
    }//GEN-LAST:event_protocolAnalyzeButtonActionPerformed

    private void protocolCopyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolCopyButtonActionPerformed
        (new copy_clipboard_text()).to_clipboard(this.protocol_raw_TextArea.getText());
    }//GEN-LAST:event_protocolCopyButtonActionPerformed


    private void update_hexcalc(int in) {
        int comp = in > 255 ? 65535 : 255;
        int rev = in > 255 ? ((Integer.reverse(in) >> 16) & 65535) : ((Integer.reverse(in) >> 24) & 255);

        complement_decimal_TextField.setText(Integer.toString(comp - in));
        complement_hex_TextField.setText(Integer.toHexString(comp - in));
        reverse_decimal_TextField.setText(Integer.toString(rev));
        reverse_hex_TextField.setText(Integer.toHexString(rev));
    }

    private void hexcalc_silly_number(NumberFormatException e) {
        System.err.println("Parse error " + e.getMessage());
        complement_decimal_TextField.setText("****");
        complement_hex_TextField.setText("****");
        reverse_decimal_TextField.setText("****");
        reverse_hex_TextField.setText("****");
    }

    private void update_from_frequency() {
        int freq = Integer.parseInt(frequency_TextField.getText());
        prontocode_TextField.setText(Pronto.formatInteger(Pronto.getProntoCode(freq)));//ir_code.ccf_integer(ir_code.get_frequency_code(freq)));
        update_from_frequency(freq);
    }
    
    private void update_from_frequencycode() {
        int freq = (int) Pronto.getFrequency(Integer.parseInt(prontocode_TextField.getText(),16));
        frequency_TextField.setText(Integer.toString(freq));
        update_from_frequency(freq);
    }
        
    private void update_from_frequency(int freq) {
        if (period_selection_enable_CheckBox.isSelected()) {
            double no_periods = Double.parseDouble(no_periods_TextField.getText());
            time_TextField.setText(Integer.toString((int)(1000000.0*no_periods/freq)));
        } else {
            int time = Integer.parseInt(time_TextField.getText());
            no_periods_TextField.setText(String.format("%.1f", (time*freq)/1000000.0));
        }
    }
    
    private void possibly_enable_decode_button() {
        boolean looks_ok = /*!protocol_params_TextField.getText().isEmpty()
                ||*/ !protocol_raw_TextArea.getText().isEmpty();
        protocol_decode_Button.setEnabled(looks_ok);
        protocol_clear_Button.setEnabled(looks_ok);
        protocolCopyButton.setEnabled(looks_ok);
        protocolAnalyzeButton.setEnabled(looks_ok);
        protocol_send_Button.setEnabled(looks_ok);
    }

    private void possibly_enable_encode_send() {
        boolean looks_ok = !commandno_TextField.getText().isEmpty() /*&& !deviceno_TextField.getText().isEmpty()*/;
        protocol_send_Button.setEnabled(looks_ok /*|| !protocol_params_TextField.getText().isEmpty()*/
                || !protocol_raw_TextArea.getText().isEmpty());
        protocol_generate_Button.setEnabled(looks_ok);
        //wav_export_Button.setEnabled(looks_ok);
    }

    private int get_gc_module() {
        return Integer.parseInt((String) gc_modules_dcbm.getSelectedItem());
    }

    private int get_gc_connector() {
        return Integer.parseInt((String) gc_connector_ComboBox.getModel().getSelectedItem());
    }

    private irtrans.led_t get_irtrans_led() {
        return irtrans.led_t.parse((String)irtrans_led_ComboBox.getSelectedItem());
    }

    //public static gui_main getApplication() {
    //  return Application.getInstance(gui_main.class);
    //}
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new GuiMain().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel DecodeIRVersion;
    private javax.swing.JTextField IRP_TextField;
    private javax.swing.JButton LIRCStopIrButton;
    private javax.swing.JTextField LIRC_address_TextField;
    private javax.swing.JTextField LIRC_address_TextField1;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel analyzePanel;
    private javax.swing.JCheckBox automaticFileNamesCheckBox;
    private javax.swing.JTextField browser_TextField;
    private javax.swing.JButton browser_select_Button;
    private javax.swing.JTextField ccf_export_buttonheight_TextField;
    private javax.swing.JTextField ccf_export_buttonwidth_TextField;
    private javax.swing.JButton ccf_export_export_Button;
    private javax.swing.JPanel ccf_export_opts_Panel;
    private javax.swing.JComboBox ccf_export_prontomodel_ComboBox;
    private javax.swing.JCheckBox ccf_export_raw_CheckBox;
    private javax.swing.JTextField ccf_export_screenheight_TextField;
    private javax.swing.JTextField ccf_export_screenwidth_TextField;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JTextField commandno_TextField;
    private javax.swing.JTextField commandno_TextField1;
    private javax.swing.JTextField commandno_TextField2;
    private javax.swing.JTextField commandno_TextField3;
    private javax.swing.JTextField complement_decimal_TextField;
    private javax.swing.JTextField complement_hex_TextField;
    private javax.swing.JTextArea console_TextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JPanel debug_Panel;
    private javax.swing.JTextField debug_TextField;
    private javax.swing.JTextField decimal_TextField;
    private javax.swing.JTextField deviceno_TextField;
    private javax.swing.JButton discoverButton;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JPanel exportPanel;
    private javax.swing.JCheckBox exportTogglesGenerateCheckBox;
    private javax.swing.JTextField exportdir_TextField;
    private javax.swing.JButton exportdir_browse_Button;
    private javax.swing.JTabbedPane exportopts_TabbedPane;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTextField frequency_TextField;
    private javax.swing.JTextField gcDiscoveredTypejTextField;
    private javax.swing.JTextField gc_address_TextField;
    private javax.swing.JButton gc_browse_Button;
    private javax.swing.JComboBox gc_connector_ComboBox;
    private javax.swing.JComboBox gc_module_ComboBox;
    private javax.swing.JPanel general_Panel;
    private javax.swing.JPanel general_export_opts_Panel;
    private javax.swing.JPanel globalcache_Panel;
    private javax.swing.JPanel globalcache_Panel1;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextField hex_TextField;
    private javax.swing.JPanel hexcalcPanel;
    private javax.swing.JButton home_browse_Button;
    private javax.swing.JButton home_load_Button;
    private javax.swing.JButton home_select_Button;
    private javax.swing.JTextField homeconf_TextField;
    private javax.swing.JButton icf_import_Button;
    private javax.swing.JPanel irtrans_Panel;
    private javax.swing.JTextField irtrans_address_TextField;
    private javax.swing.JButton irtrans_browse_Button;
    private javax.swing.JComboBox irtrans_led_ComboBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField lastFTextField;
    private javax.swing.JButton macro_browse_Button;
    private javax.swing.JButton macro_select_Button;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JTextField makehexIrpDir_TextField;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextField no_periods_TextField;
    private javax.swing.JComboBox no_sends_protocol_ComboBox;
    private javax.swing.JTabbedPane optsTabbedPane;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JCheckBox period_selection_enable_CheckBox;
    private javax.swing.JTextField prontocode_TextField;
    private javax.swing.JButton protocolAnalyzeButton;
    private javax.swing.JButton protocolCopyButton;
    private javax.swing.JButton protocolExportButton;
    private javax.swing.JComboBox protocol_ComboBox;
    private javax.swing.JButton protocol_clear_Button;
    private javax.swing.JButton protocol_decode_Button;
    private javax.swing.JButton protocol_generate_Button;
    private javax.swing.JComboBox protocol_outputhw_ComboBox;
    private javax.swing.JComboBox protocol_outputhw_ComboBox1;
    private javax.swing.JTextField protocol_params_TextField;
    private javax.swing.JTextArea protocol_raw_TextArea;
    private javax.swing.JButton protocol_send_Button;
    private javax.swing.JButton protocol_stop_Button;
    private javax.swing.JPanel protocolsPanel;
    private javax.swing.JTabbedPane protocolsSubPane;
    private javax.swing.JComboBox rendererComboBox;
    private javax.swing.JTextField reverse_decimal_TextField;
    private javax.swing.JTextField reverse_hex_TextField;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTextField subdevice_TextField;
    private javax.swing.JTextField time_TextField;
    private javax.swing.JComboBox toggle_ComboBox;
    private javax.swing.JCheckBox verbose_CheckBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JPanel warDialerPanel;
    // End of variables declaration//GEN-END:variables
    private AboutPopup aboutBox;
}
