/*
Copyright (C) 2012 Bengt Martensson.

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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.harctoolbox.IrpMaster.*;
import ptolemy.plot.Plot;
import ptolemy.plot.PlotFrame;
import ptolemy.util.RunnableExceptionCatcher;

/**
 * This class generates a plot of an IrSignal, using the PTPlot library.
 */
public class Plotter extends PlotFrame {
    private final static int timeScale = 1;

    class MyCommandListener implements KeyListener {

        private boolean control;
        private boolean shift;

        @Override
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();

            switch (keycode) {
                case KeyEvent.VK_CONTROL:
                    control = true;
                    break;

                case KeyEvent.VK_SHIFT:
                    shift = true;
                    break;

                case KeyEvent.VK_Q:
                case KeyEvent.VK_ESCAPE:
                    _close();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int keycode = e.getKeyCode();

            switch (keycode) {
                case KeyEvent.VK_CONTROL:
                    control = false;
                    break;

                case KeyEvent.VK_SHIFT:
                    shift = false;
                    break;

                default:
                    break;
                }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
    
    /**
     * Displays a help message.
     */
    @Override
    protected void _help() {
        JOptionPane.showMessageDialog(this,
                "The plot shows the IR signal withouth modulation.\n"
                + " * Red: Intro sequeuence.\n"
                + " * Blue: Repetition sequence, here shown exactly once.\n"
                + " * Green: Ending sequence.\n\n"
                + "Any of these can be empty.\n"
                + "Use left mouse button (hold down,drag, release) for zooming.",
                "Help for IrMaster Plot", JOptionPane.INFORMATION_MESSAGE);
    }
    /**
     * Generates a plot of its IrSignal using PTPlot.
     * @param irSignal Signal to be plotted
     * @param exitOnClose If true, calls System.exit on close. For use as a standalone application.
     * @param title String as Window title.
     * @param legend String to use as legend for the plot. 
     */

    public Plotter(final IrSignal irSignal, boolean exitOnClose, final String title, final String legend) {
        super("IrMaster Plotter");
        final Plot thePlot = (Plot) plot;
        setTitle(title);
        
        // Remove some menu entries that do not fit here
        _specialMenu.remove(5); // Sample plot
        _specialMenu.remove(2); // clear
        _specialMenu.setText("Help"); // Rename specialMenu into Help
        _specialMenu.setMnemonic('H');
        _specialMenu.getItem(1).setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        _editMenu.add(_specialMenu.getItem(3)); // Reset Axis moved to edit menu
        _editMenu.add(_specialMenu.getItem(2)); // Fill moved to edit menu
        _fileMenu.remove(2); // saveAs
        _fileMenu.remove(1); // save
        _fileMenu.remove(0); // open

        // Default keylistener KILLS the application, including invoking application,
        // with System.exit. We do not want that brain damage here.
        KeyListener[] kl = thePlot.getKeyListeners();
        thePlot.removeKeyListener(kl[0]);
        thePlot.addKeyListener(new MyCommandListener());
        
        thePlot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getButton() != MouseEvent.BUTTON1 && evt.getButton() != MouseEvent.NOBUTTON)
                    _close();
            }
        });

        thePlot.setToolTipText("Press left mouse button and drag to zoom. Press any other mouse button to close.");
        thePlot.setTitle(legend);
        

        if (exitOnClose)
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Strangely, calling _close() here sends javac into
                    // an infinite loop (in jdk 1.1.4).
                //              _close();
                System.exit(IrpUtils.exitSuccess);
            }
        });

        Runnable sample = new RunnableExceptionCatcher(new Runnable() {

            @Override
            public void run() {
                
                thePlot.setYRange(0, 1);
                thePlot.addYTick("Off", 0);
                thePlot.addYTick("On", 1);
                thePlot.setButtons(true);

                int nBeg = irSignal.getIntroLength();
                int nRep = irSignal.getRepeatLength();
                int nEnd = irSignal.getEndingLength();
                double sum = 0;
                for (int i = 0; i < nBeg; i++) {
                    sum += Math.abs(irSignal.getIntroDouble(i));
                }
                for (int i = 0; i < nRep; i++) {
                    sum += Math.abs(irSignal.getRepeatDouble(i));
                }
                for (int i = 0; i < nEnd; i++) {
                    sum += Math.abs(irSignal.getEndingDouble(i));
                }
                //synchronized (Plot.this) {
                
                //}
                thePlot.setXRange(0, sum * timeScale);
                thePlot.setXLabel("time in micro-seconds");

                //plot.setMarksStyle("none");
                thePlot.setImpulses(true);
                thePlot.setBars(true);
                double time = 0;
                if (nBeg > 1) {
                    thePlot.addPoint(0, time, 0, true);
                    thePlot.addPoint(0, time, 1, true);
                }
                for (int i = 0; i < nBeg; i++) {
                    double val = irSignal.getIntroDouble(i);
                    time += Math.abs(val);
                    if (i % 2 == 1) {
                        // Off period
                        thePlot.addPoint(0, time * timeScale, 0, true);
                        if (i < nBeg - 1)
                            thePlot.addPoint(0, time * timeScale, 1, true);
                    } else {
                        // On period
                        thePlot.addPoint(0, time * timeScale, 1, true);
                        thePlot.addPoint(0, time * timeScale, 0, true);
                    }
                }
                if (nRep > 1) {
                    thePlot.addPoint(1, time * timeScale, 0, true);
                    thePlot.addPoint(1, time * timeScale, 1, true);
                }
                for (int i = 0; i < nRep; i++) {
                    double val = irSignal.getRepeatDouble(i);
                    time += Math.abs(val);
                    if (i % 2 == 1) {
                        thePlot.addPoint(1, time * timeScale, 0, true);
                        if (i < nRep - 1)
                            thePlot.addPoint(1, time * timeScale, 1, true);
                    } else {
                        thePlot.addPoint(1, time * timeScale, 1, true);
                        thePlot.addPoint(1, time * timeScale, 0, true);
                    }
                }
                if (nEnd > 1) {
                    thePlot.addPoint(2, time * timeScale, 0, true);
                    thePlot.addPoint(2, time * timeScale, 1, true);
                }
                for (int i = 0; i < nEnd; i++) {
                    double val = irSignal.getEndingDouble(i);
                    time += Math.abs(val);
                    if (i % 2 == 1) {
                        thePlot.addPoint(2, time * timeScale, 0, true);
                        if (i < nEnd - 1)
                            thePlot.addPoint(2, time * timeScale, 1, true);
                    } else {
                        thePlot.addPoint(2, time * timeScale, 1, true);
                        thePlot.addPoint(2, time * timeScale, 0, true);
                    }
                }

                repaint();
            } // run
        });
        plot.deferIfNecessary(sample);
        setVisible(true);
    }

    private static void usage(int exitcode) {
        System.err.println("Usage:");
        System.err.println("    Plotter [-c <protocol_ini_path>] <protocol> <deviceno> [<subdevice_no>] commandno [<toggle>]");
        System.err.println("or");
        System.err.println("    Plotter <Pronto code starting with 0000>");
        System.exit(exitcode);
    }

    /**
     * Allows for plotting of either raw CCF signals or protocol/parameters represented IR signals.
     * @param args
     */
    public static void main(final String[] args) {
        int debug = 0; // presently not used.
        String irprotocolsIniFilename = "data/IrpProtocols.ini";
        IrSignal irTmp = null;
        int noOptionArgs;
        String tmpLegend = null;

        int arg_i = 0;
        while (arg_i < args.length && (args[arg_i].length() > 0)
                && args[arg_i].charAt(0) == '-') {

            if (args[arg_i].equals("-c")) {
                arg_i++;
                irprotocolsIniFilename = args[arg_i++];
            } else if (args[arg_i].equals("-d")) {
                arg_i++;
                debug++;
            } else if (args[arg_i].equals("-h") || args[arg_i].equals("-?") ||args[arg_i].equals("--help")) {
                usage(IrpUtils.exitSuccess);
             } else
                usage(IrpUtils.exitUsageError);
        }
        noOptionArgs = arg_i;

        if (args.length == arg_i)
            usage(IrpUtils.exitUsageError);

        if (args[arg_i].equals("0000")) {
            // Pronto form
            int length = args.length - arg_i;
            int[] ccf = new int[length];
            for (int i = 0; i < length; i++) {
                ccf[i] = Integer.parseInt(args[i + arg_i], 16);
            }
            String str = IrpUtils.join(noOptionArgs, args, " ");
            tmpLegend = str.length() > 40 ? str.substring(0, 40) + "..." : str;
            try {
                irTmp = Pronto.ccfSignal(ccf);
            } catch (IrpMasterException ex) {
                System.err.println(ex.getMessage());
                System.exit(IrpUtils.exitUsageError);
            }
        } else {
            // protocol, parameters
            String protocolname = args[arg_i++];
            long D = IrpUtils.invalid;
            long S = IrpUtils.invalid;
            long F = IrpUtils.invalid;
            long T = IrpUtils.invalid;

            switch (args.length - arg_i) {
                case 1:
                    F = Long.parseLong(args[arg_i++]);
                    break;
                case 2:
                    D = Long.parseLong(args[arg_i++]);
                    F = Long.parseLong(args[arg_i++]);
                    break;
                case 3:
                    D = Long.parseLong(args[arg_i++]);
                    S = Long.parseLong(args[arg_i++]);
                    F = Long.parseLong(args[arg_i++]);
                    break;
                case 4:
                    D = Long.parseLong(args[arg_i++]);
                    S = Long.parseLong(args[arg_i++]);
                    F = Long.parseLong(args[arg_i++]);
                    T = Long.parseLong(args[arg_i++]);
                    break;
                default:
                    usage(IrpUtils.exitUsageError);
            }

            try {
                IrpMaster irpMaster = new IrpMaster(irprotocolsIniFilename);
                Protocol protocol = irpMaster.newProtocol(protocolname);
                irTmp = protocol.renderIrSignal(D, S, F, T);
                tmpLegend = protocolname + ": " + protocol.notationString("=", " ");
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                System.exit(IrpUtils.exitFatalProgramFailure);
            }
        }
        final IrSignal irSignal = irTmp;
        final String legend = tmpLegend;

        try {
            // Run this in the Swing Event Thread.
            Runnable doActions = new Runnable() {

                @Override
                public void run() {
                    try {
                        new Plotter(irSignal, true, "IrMaster Standalone Plotter", legend);
                    } catch (Exception ex) {
                        System.err.println(ex.toString());
                        ex.printStackTrace();
                    }
                }
            };

            SwingUtilities.invokeAndWait(doActions);
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();
        }
    }
}
