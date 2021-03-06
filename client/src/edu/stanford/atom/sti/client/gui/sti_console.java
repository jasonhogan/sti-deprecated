/** @file sti_console.java
 *  @author Jason Michael Hogan
 *  @brief Source-file for the class sti_console
 *  @section license License
 *
 *  Copyright (C) 2008 Jason Hogan <hogan@stanford.edu>\n
 *  This file is part of the Stanford Timing Interface (STI).
 *
 *  The STI is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The STI is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the STI.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.stanford.atom.sti.client.gui;

import edu.stanford.atom.sti.client.comm.bl.DataManager;
import edu.stanford.atom.sti.client.comm.bl.SequenceManager;
import edu.stanford.atom.sti.client.comm.bl.TChannelDecode;
import edu.stanford.atom.sti.client.comm.bl.device.*;
import edu.stanford.atom.sti.client.comm.io.STIServerConnection;
import edu.stanford.atom.sti.client.comm.io.STIServerEventHandler;
import edu.stanford.atom.sti.client.gui.EventsTab.ChannelStateTab;
import edu.stanford.atom.sti.client.gui.state.*;
import edu.stanford.atom.sti.client.gui.state.STIStateMachine.State;
import edu.stanford.atom.sti.corba.Types.TChannel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class sti_console extends javax.swing.JFrame implements STIStateListener {

    private String playButtonDisabledToolTipReminderDirectMode = "(A files cannot be played in Direct Mode.)";
    private String playButtonDisabledToolTipReminder = "(A file must be parsed before it can be played.)";
    private String playToolTip = "Play";    
    private boolean clientHasControl = false;

    private ChannelStateTab channelStateTab1 = new ChannelStateTab();
    
    private STIServerEventHandler eventHandler = new STIServerEventHandler();
    private STIStateMachine stateMachine = new STIStateMachine();
    private STIServerConnection serverConnection = new STIServerConnection(stateMachine, eventHandler);
    
    private DeviceManager deviceManager = new DeviceManager();
    private DataManager dataManager = new DataManager();
    private SequenceManager sequenceManager = new SequenceManager();

    private Thread connectionThread = null;
    private Thread parseThread = null;
    private Thread playThread = null;

    private ApplicationManager applicationManager = null;

    private Version version = new Version(1.0);     //Version 0.5
    
    public sti_console() {
       
        initComponents();    
        
        plugInTab8.add(channelStateTab1);

        eventHandler.addEventListener(serverConnection);
        eventHandler.addEventListener(stateMachine);
        eventHandler.addEventListener(tabbedEditor1);
        eventHandler.addEventListener(dataManager);
        eventHandler.addEventListener(deviceManager);
        eventHandler.addEventListener(sequenceManager);

        //Setup version info
        System.out.println("STI Build Number = " + version.getBuildNumber() + ": " + version.getBuildDate());
        versionLabel.setText("Version " + version.getVersionNumber() + ", Build " + version.getBuildNumber()+ ".");
        buildDateLabel.setText("Build Date: " + version.getBuildDate());
        buildTimeLabel.setText("Build Time: " + version.getBuildTime());

        serverAddressTextField.setText(serverConnection.getServerAddress());

        tabbedEditor1.setMainFileComboBoxModel(mainFileComboBox.getModel());

        runTab1.setSequenceManager(sequenceManager);
        
        stateMachine.addStateListener(this);
        stateMachine.addStateListener(tabbedEditor1);
        stateMachine.addStateListener(sequenceManager);

        dataManager.addDataListener(eventsTab1);
        dataManager.addDataListener(variableTab1);
        dataManager.addDataListener(timingDiagramTab1);
        dataManager.addDataListener(runTab1);
        dataManager.addDataListener(channelStateTab1);
        dataManager.addDataListener(tabbedEditor1);
        
        plugInTab7.addVisibleTabListener(timingDiagramTab1);

        applicationManager = new ApplicationManager(plugInManager);

        
        //Define various DeviceCollection rules
        
        //For the RegisteredDevicesTab: all devices
        DeviceCollection genericCollection = new DeviceCollection() {
            public boolean isAllowedMember(Device device) {
                return true;
            }
        };
        
        //For the ChannelViewerTab ('scope'): all devices with at least one input
        DeviceCollection inputCollection = new DeviceCollection() {
            public boolean isAllowedMember(Device device) {
                boolean isAllowed = false;
                TChannel[] channels = device.getChannels();

                for (TChannel channel : channels) {
                    TChannelDecode channelDecode = new TChannelDecode(channel);
                    if (channelDecode.IOType().equals("Input"))
                    {
                        isAllowed |= true;
                    }
                }
                return isAllowed;
          }
        };
        
        //For STI_Applications: application devices
        DeviceCollection applicationCollection = new DeviceCollection() {
            public boolean isAllowedMember(Device device) {
                return applicationManager.isApplication(device);
            }
        };

        genericCollection.addDeviceCollectionListener(registeredDevicesTab1);
        genericCollection.addDeviceCollectionListener(eventsTab1);
        applicationCollection.addDeviceCollectionListener(applicationManager);
        inputCollection.addDeviceCollectionListener(channelViewerTab1);
        
        //Device collections
        deviceManager.addDeviceCollection(genericCollection);
        deviceManager.addDeviceCollection(applicationCollection);
        deviceManager.addDeviceCollection(inputCollection);

        timingDiagramTab1.installDeviceCollection(genericCollection);

        sequenceManager.addSequenceListener(runTab1);
        
        serverConnection.addServerConnectionListener(dataManager);
        serverConnection.addServerConnectionListener(deviceManager);
        serverConnection.addServerConnectionListener(sequenceManager);
        serverConnection.addServerConnectionListener(documentationTab1);
        serverConnection.addServerConnectionListener(variableTab1);

        registeredDevicesTab1.registerDeviceManager(deviceManager);

        stateMachine.changeMode(STIStateMachine.Mode.Documented);

    }

    public void updateMode(STIStateEvent event) {
        
        switch( event.mode() ) {
            case Direct:
                modeComboBox.setSelectedItem(STIStateMachine.Mode.Direct);
                directModeMenuItem.setSelected(true);
                runRadioButtonPanel.setEnabled(false);
                clientHasControl = true;
                break;
            case Documented:
                modeComboBox.setSelectedItem(STIStateMachine.Mode.Documented);
                documentedModeMenuItem.setSelected(true);
                runRadioButtonPanel.setEnabled(true);
                clientHasControl = true;
                break;
            case Testing:
                modeComboBox.setSelectedItem(STIStateMachine.Mode.Testing);
                testingModeMenuItem.setSelected(true);
                runRadioButtonPanel.setEnabled(true);
                clientHasControl = true;
                break;
            case Monitor:
                modeComboBox.setSelectedItem(STIStateMachine.Mode.Monitor);
                monitorModeMenuItem.setSelected(true);
                runRadioButtonPanel.setEnabled(false);
                clientHasControl = false;
                break;
            default:
                break;
        }
        updateState(event);
    }
    
    public void updateState(STIStateEvent event) {

        switch( event.runType() ) {
            case Sequence:
                //State is unparsed if there are no unchecked experiements in the sequence table
                if( event.state() == STIStateMachine.State.IdleParsed
                        && !sequenceManager.experimentsRemaining() )
                    updateState(STIStateMachine.State.IdleUnparsed);
                else
                    updateState(event.state());
                break;
            case Single:
            default:
                updateState(event.state());
                break;
        }
    }

    //Client state machine widget control
    public void updateState(STIStateMachine.State state) {
        switch( state ) {
            case Disconnected:
                connectButton.setText("Connect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, false);
            //    jProgressBar1.setIndeterminate(false);
                jProgressBar1.setValue(0);
                statusTextField.setText("Not connected to server");
                serverAddressTextField.setEditable(true);
                playButton.setEnabled(false);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                continuousToggleButton.setSelected(false);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip + " " + playButtonDisabledToolTipReminder );
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(false);
                directModeMenuItem.setEnabled(false);
                documentedModeMenuItem.setEnabled(false);
                testingModeMenuItem.setEnabled(false);
                monitorModeMenuItem.setEnabled(false);
                modeComboBox.setEnabled(false);
                sequenceRunRadioButton.setEnabled(false);
                singleRunRadioButton.setEnabled(false);
                mainFileComboBox.setEnabled(tabbedEditor1.mainFileIsValid());
//                serverAddressTextField.setText(serverAddress);
//                connectionThread.interrupt();
                serverConnection.disconnectFromServer();
                break;
            case Connecting:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, true);
                statusTextField.setText("Connecting...");
                serverAddressTextField.setEditable(false);
                serverConnection.setServerAddress(serverAddressTextField.getText());
                playButton.setEnabled(false);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(true);
                continuousToggleButton.setSelected(false);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip + " " + playButtonDisabledToolTipReminder );
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(true);
                directModeMenuItem.setEnabled(false);
                documentedModeMenuItem.setEnabled(false);
                testingModeMenuItem.setEnabled(false);
                monitorModeMenuItem.setEnabled(false);
                modeComboBox.setEnabled(false);
                sequenceRunRadioButton.setEnabled(false);
                singleRunRadioButton.setEnabled(false);
                mainFileComboBox.setEnabled(false);
                connectionThread = new Thread(serverConnection);
                connectionThread.start();
                break;
            case IdleUnparsed:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(tabbedEditor1.mainFileIsValid() && clientHasControl);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, false);
                jProgressBar1.setValue(0);
                statusTextField.setText("Ready");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(false);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                continuousToggleButton.setSelected(false);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip + " " + playButtonDisabledToolTipReminder );
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(false);
                directModeMenuItem.setEnabled(true);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(true);
                singleRunRadioButton.setEnabled(true);
                mainFileComboBox.setEnabled(tabbedEditor1.mainFileIsValid());
                attachServants();
                break;
            case Parsing:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, true);
                statusTextField.setText("Parsing...");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(false);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(clientHasControl);
                continuousToggleButton.setSelected(false);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip + " " + playButtonDisabledToolTipReminder );
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(clientHasControl);
                directModeMenuItem.setEnabled(false);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(true);
                singleRunRadioButton.setEnabled(true);
                mainFileComboBox.setEnabled(false);
                break;
            case IdleParsed:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(tabbedEditor1.mainFileIsValid() && clientHasControl);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, false);
                jProgressBar1.setValue(0);
                statusTextField.setText("Ready");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(clientHasControl);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                continuousToggleButton.setEnabled(clientHasControl && singleRunRadioButton.isSelected());
                continuousMenuItem.setEnabled(continuousToggleButton.isEnabled());
                playButton.setToolTipText( playToolTip );
                playMenuItem.setEnabled(clientHasControl);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(false);
                directModeMenuItem.setEnabled(true);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(true);
                singleRunRadioButton.setEnabled(true);
                mainFileComboBox.setEnabled(tabbedEditor1.mainFileIsValid());
                break;
            case Running:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, true);
                jProgressBar1.setValue(0);
                statusTextField.setText("Running...");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(false);
                pauseButton.setEnabled(clientHasControl);
                stopButton.setEnabled(clientHasControl);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip );
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(clientHasControl);
                stopMenuItem.setEnabled(clientHasControl);
                directModeMenuItem.setEnabled(false);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(false);
                singleRunRadioButton.setEnabled(false);
                mainFileComboBox.setEnabled(false);
                break;
            case Paused:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, false);
                jProgressBar1.setValue(0);
                statusTextField.setText("Paused");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(clientHasControl);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(clientHasControl);
                playButton.setToolTipText( playToolTip );
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playMenuItem.setEnabled(clientHasControl);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(clientHasControl);
                directModeMenuItem.setEnabled(false);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(false);
                singleRunRadioButton.setEnabled(false);
                mainFileComboBox.setEnabled(false);
                break;
            case RunningDirect:
                connectButton.setText("Disconnect");
                parseButton.setEnabled(false);
                parseMenuItem.setEnabled(parseButton.isEnabled());
                setIndeterminateLater(jProgressBar1, true);
                statusTextField.setText("Running Direct...");
                serverAddressTextField.setEditable(false);
                serverAddressTextField.setText(serverConnection.getServerAddress());
                playButton.setEnabled(false);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(clientHasControl);
                continuousToggleButton.setEnabled(false);
                continuousMenuItem.setEnabled(false);
                playButton.setToolTipText( playToolTip + " " + playButtonDisabledToolTipReminderDirectMode);
                playMenuItem.setEnabled(false);
                pauseMenuItem.setEnabled(false);
                stopMenuItem.setEnabled(clientHasControl);
                directModeMenuItem.setEnabled(true);
                documentedModeMenuItem.setEnabled(true);
                testingModeMenuItem.setEnabled(true);
                monitorModeMenuItem.setEnabled(true);
                modeComboBox.setEnabled(true);
                sequenceRunRadioButton.setEnabled(false);
                singleRunRadioButton.setEnabled(false);
                mainFileComboBox.setEnabled(true);
                break;
            default:
                break;
        }
    }
    
    public void updateRunType(STIStateEvent event) {
        switch(event.runType()) {
            case Single:
                sequenceRunRadioButton.setSelected(false);
                singleRunRadioButton.setSelected(true);
                singleRunRadioButtonMenuItem.setSelected(true);
                sequenceRunRadioButtonMenuItem.setSelected(false);
                playToolTip = "Play";
                playMenuItem.setText("Play Single");
                break;
            case Sequence:
                sequenceRunRadioButton.setSelected(true);
                singleRunRadioButton.setSelected(false);
                singleRunRadioButtonMenuItem.setSelected(false);
                sequenceRunRadioButtonMenuItem.setSelected(true);
                playToolTip = "Play Sequence";
                playMenuItem.setText("Play Sequence");
                break;
            default:
                break;
        }

        updateState(event);
    }

    private void setIndeterminateLater(final javax.swing.JProgressBar progressBar, final boolean status) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(status);
            }
        });
    }

    private void attachServants() {
        tabbedEditor1.setParser(serverConnection.getParser());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        aboutDialog = new javax.swing.JDialog();
        jLabel4 = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JSeparator();
        jLabel6 = new javax.swing.JLabel();
        buildDateLabel = new javax.swing.JLabel();
        buildTimeLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        jSplitPane3 = new javax.swing.JSplitPane();
        jSplitPane5 = new javax.swing.JSplitPane();
        runRadioButtonPanel = new javax.swing.JPanel();
        singleRunRadioButton = new javax.swing.JRadioButton();
        sequenceRunRadioButton = new javax.swing.JRadioButton();
        jSeparator7 = new javax.swing.JSeparator();
        jPanel5 = new javax.swing.JPanel();
        playButton = new javax.swing.JButton();
        pauseButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        continuousToggleButton = new javax.swing.JToggleButton();
        jPanel7 = new javax.swing.JPanel();
        parseButton = new javax.swing.JButton();
        mainFileComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jSplitPane4 = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        modeComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        serverAddressTextField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        plugInManager = new edu.stanford.atom.sti.client.gui.PlugInManager();
        plugInTab3 = new edu.stanford.atom.sti.client.gui.PlugInTab("Editor");
        tabbedEditor1 = new edu.stanford.atom.sti.client.gui.FileEditorTab.TabbedEditor();
        plugInTab4 = new edu.stanford.atom.sti.client.gui.PlugInTab("Variables");
        variableTab1 = new edu.stanford.atom.sti.client.gui.VariablesTab.VariableTab();
        plugInTab5 = new edu.stanford.atom.sti.client.gui.PlugInTab("Events");
        eventsTab1 = new edu.stanford.atom.sti.client.gui.EventsTab.EventsTab();
        plugInTab1 = new edu.stanford.atom.sti.client.gui.PlugInTab("Devices", "Devices");
        registeredDevicesTab1 = new edu.stanford.atom.sti.client.gui.DevicesTab.RegisteredDevicesTab();
        plugInTab2 = new edu.stanford.atom.sti.client.gui.PlugInTab("Run", "Run");
        runTab1 = new edu.stanford.atom.sti.client.gui.RunTab.RunTab();
        plugInTab6 = new edu.stanford.atom.sti.client.gui.PlugInTab("Documentation","Documentation");
        documentationTab1 = new edu.stanford.atom.sti.client.gui.RunTab.DocumentationTab();
        plugInTab8 = new edu.stanford.atom.sti.client.gui.PlugInTab();
        plugInTab7 = new edu.stanford.atom.sti.client.gui.PlugInTab();
        timingDiagramTab1 = new edu.stanford.atom.sti.client.gui.EventsTab.TimingDiagramTab();
        plugInTab9 = new edu.stanford.atom.sti.client.gui.PlugInTab("Channel Viewer","Channel Viewer");
        channelViewerTab1 = new edu.stanford.atom.sti.client.gui.RunTab.ChannelViewerTab();
        jPanel2 = new javax.swing.JPanel();
        statusTextField = new javax.swing.JTextField();
        jProgressBar1 = new javax.swing.JProgressBar();
        jMenuBar2 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        openMenuItem = new javax.swing.JMenuItem();
        openLocalMenuItem = new javax.swing.JMenuItem();
        closeMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        saveAsLocalMenuItem = new javax.swing.JMenuItem();
        saveAllMenuItem1 = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        refreshAllMenuItem = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        modeMenu = new javax.swing.JMenu();
        directModeMenuItem = new javax.swing.JRadioButtonMenuItem();
        documentedModeMenuItem = new javax.swing.JRadioButtonMenuItem();
        testingModeMenuItem = new javax.swing.JRadioButtonMenuItem();
        monitorModeMenuItem = new javax.swing.JRadioButtonMenuItem();
        runMenu = new javax.swing.JMenu();
        playMenuItem = new javax.swing.JMenuItem();
        pauseMenuItem = new javax.swing.JMenuItem();
        stopMenuItem = new javax.swing.JMenuItem();
        continuousMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        singleRunRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        sequenceRunRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        parseMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuItem2 = new javax.swing.JMenuItem();

        aboutDialog.setTitle("About");
        aboutDialog.setMinimumSize(new java.awt.Dimension(305, 220));
        aboutDialog.setResizable(false);

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()+10));
        jLabel4.setText("STI Console");

        versionLabel.setFont(versionLabel.getFont().deriveFont(versionLabel.getFont().getSize()+3f));
        versionLabel.setText("Version ");

        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getSize()+3f));
        jLabel6.setText("Client for the Stanford Timing Interface (STI).");

        buildDateLabel.setFont(buildDateLabel.getFont().deriveFont(buildDateLabel.getFont().getSize()+3f));
        buildDateLabel.setText("Build Date: ");

        buildTimeLabel.setFont(buildTimeLabel.getFont().deriveFont(buildTimeLabel.getFont().getSize()+3f));
        buildTimeLabel.setText("Build Time: ");

        javax.swing.GroupLayout aboutDialogLayout = new javax.swing.GroupLayout(aboutDialog.getContentPane());
        aboutDialog.getContentPane().setLayout(aboutDialogLayout);
        aboutDialogLayout.setHorizontalGroup(
            aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aboutDialogLayout.createSequentialGroup()
                .addGroup(aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(aboutDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addGroup(aboutDialogLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(versionLabel)
                                    .addComponent(buildDateLabel)
                                    .addComponent(buildTimeLabel)))))
                    .addGroup(aboutDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel6))
                    .addGroup(aboutDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(aboutDialogLayout.createSequentialGroup()
                        .addGap(99, 99, 99)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        aboutDialogLayout.setVerticalGroup(
            aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aboutDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionLabel)
                .addGap(4, 4, 4)
                .addComponent(buildDateLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buildTimeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("STI Console");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(new java.awt.Dimension(800, 100));

        jPanel3.setPreferredSize(new java.awt.Dimension(750, 200));

        jSplitPane1.setDividerLocation(50);
        jSplitPane1.setDividerSize(3);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setMinimumSize(new java.awt.Dimension(750, 0));
        jSplitPane1.setPreferredSize(new java.awt.Dimension(750, 750));

        jPanel1.setMaximumSize(new java.awt.Dimension(750, 32767));
        jPanel1.setMinimumSize(new java.awt.Dimension(750, 50));
        jPanel1.setPreferredSize(new java.awt.Dimension(750, 50));

        jToolBar1.setRollover(true);
        jToolBar1.setMaximumSize(new java.awt.Dimension(780, 50));
        jToolBar1.setMinimumSize(new java.awt.Dimension(780, 50));
        jToolBar1.setPreferredSize(new java.awt.Dimension(780, 50));

        jSplitPane3.setBorder(null);
        jSplitPane3.setDividerSize(0);

        jSplitPane5.setBorder(null);
        jSplitPane5.setDividerSize(0);

        runRadioButtonPanel.setMaximumSize(new java.awt.Dimension(107, 48));
        runRadioButtonPanel.setMinimumSize(new java.awt.Dimension(107, 48));
        runRadioButtonPanel.setPreferredSize(new java.awt.Dimension(107, 48));

        buttonGroup1.add(singleRunRadioButton);
        singleRunRadioButton.setSelected(true);
        singleRunRadioButton.setText("Single Run");
        singleRunRadioButton.setFocusable(false);
        singleRunRadioButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        singleRunRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        singleRunRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                singleRunRadioButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(sequenceRunRadioButton);
        sequenceRunRadioButton.setText("Sequence Run");
        sequenceRunRadioButton.setFocusable(false);
        sequenceRunRadioButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sequenceRunRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        sequenceRunRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequenceRunRadioButtonActionPerformed(evt);
            }
        });

        jSeparator7.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator7.setMaximumSize(new java.awt.Dimension(10, 48));
        jSeparator7.setMinimumSize(new java.awt.Dimension(10, 48));
        jSeparator7.setPreferredSize(new java.awt.Dimension(10, 48));

        javax.swing.GroupLayout runRadioButtonPanelLayout = new javax.swing.GroupLayout(runRadioButtonPanel);
        runRadioButtonPanel.setLayout(runRadioButtonPanelLayout);
        runRadioButtonPanelLayout.setHorizontalGroup(
            runRadioButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runRadioButtonPanelLayout.createSequentialGroup()
                .addGroup(runRadioButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sequenceRunRadioButton)
                    .addComponent(singleRunRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        runRadioButtonPanelLayout.setVerticalGroup(
            runRadioButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runRadioButtonPanelLayout.createSequentialGroup()
                .addComponent(singleRunRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sequenceRunRadioButton)
                .addGap(2, 2, 2))
            .addGroup(runRadioButtonPanelLayout.createSequentialGroup()
                .addComponent(jSeparator7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane5.setRightComponent(runRadioButtonPanel);

        playButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/atom/sti/client/resources/Play16.gif"))); // NOI18N
        playButton.setToolTipText("Play");
        playButton.setFocusable(false);
        playButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        playButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        playButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        pauseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/atom/sti/client/resources/Pause16.gif"))); // NOI18N
        pauseButton.setToolTipText("Pause");
        pauseButton.setFocusable(false);
        pauseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pauseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/atom/sti/client/resources/Stop16.gif"))); // NOI18N
        stopButton.setToolTipText("Stop");
        stopButton.setFocusable(false);
        stopButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stopButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        continuousToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/atom/sti/client/resources/Refresh24.gif"))); // NOI18N
        continuousToggleButton.setToolTipText("Run Continous");
        continuousToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                continuousToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(playButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(continuousToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(continuousToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(playButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jSplitPane5.setLeftComponent(jPanel5);

        jSplitPane3.setLeftComponent(jSplitPane5);

        jPanel7.setMaximumSize(new java.awt.Dimension(180, 48));
        jPanel7.setMinimumSize(new java.awt.Dimension(180, 48));
        jPanel7.setPreferredSize(new java.awt.Dimension(180, 48));

        parseButton.setText("Parse");
        parseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parseButtonActionPerformed(evt);
            }
        });

        mainFileComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mainFileComboBoxActionPerformed(evt);
            }
        });

        jLabel3.setText("Main File:");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(parseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(mainFileComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addComponent(mainFileComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane3.setRightComponent(jPanel7);

        jToolBar1.add(jSplitPane3);

        jSplitPane4.setBorder(null);
        jSplitPane4.setDividerSize(0);

        modeComboBox.setModel(new DefaultComboBoxModel(
            new STIStateMachine.Mode[] {
                STIStateMachine.Mode.Direct,
                STIStateMachine.Mode.Documented,
                STIStateMachine.Mode.Testing,
                STIStateMachine.Mode.Monitor}
        ));
        modeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modeComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Mode:");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.DEFAULT_SIZE, 1, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(modeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jSeparator1)
            .addComponent(jSeparator3)
        );

        jSplitPane4.setLeftComponent(jPanel4);

        jLabel2.setText("Server Address: ");

        serverAddressTextField.setText("localhost:2809");
        serverAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverAddressTextFieldActionPerformed(evt);
            }
        });

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(connectButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serverAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(24, 24, 24))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(serverAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(connectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jSplitPane4.setRightComponent(jPanel6);

        jToolBar1.add(jSplitPane4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jSplitPane1.setTopComponent(jPanel1);

        jPanel8.setPreferredSize(new java.awt.Dimension(750, 400));

        plugInManager.setAutoscrolls(true);
        plugInManager.setMaximumSize(new java.awt.Dimension(770, 750));
        plugInManager.setMinimumSize(new java.awt.Dimension(750, 300));
        plugInManager.setPreferredSize(new java.awt.Dimension(770, 400));

        plugInTab3.setRollover(true);
        plugInTab3.setMinimumSize(new java.awt.Dimension(639, 500));

        tabbedEditor1.setMinimumSize(new java.awt.Dimension(626, 500));
        tabbedEditor1.setPreferredSize(new java.awt.Dimension(750, 650));
        plugInTab3.add(tabbedEditor1);

        plugInManager.addTab("Editor", plugInTab3);

        plugInTab4.setRollover(true);
        plugInTab4.add(variableTab1);

        plugInManager.addTab("Variables", plugInTab4);

        plugInTab5.setRollover(true);
        plugInTab5.add(eventsTab1);

        plugInManager.addTab("Events", plugInTab5);

        plugInTab1.setRollover(true);
        plugInTab1.setMaximumSize(new java.awt.Dimension(32769, 32769));
        plugInTab1.setMinimumSize(new java.awt.Dimension(500, 570));
        plugInTab1.add(registeredDevicesTab1);

        plugInManager.addTab("Devices", plugInTab1);

        plugInTab2.setRollover(true);
        plugInTab2.add(runTab1);

        plugInManager.addTab("Run", plugInTab2);

        plugInTab6.setRollover(true);
        plugInTab6.add(documentationTab1);

        plugInManager.addTab("Documentation", plugInTab6);

        plugInTab8.setRollover(true);
        plugInManager.addTab("State", plugInTab8);

        plugInTab7.setRollover(true);
        plugInTab7.add(timingDiagramTab1);

        plugInManager.addTab("Timing Diagram", plugInTab7);

        plugInTab9.setRollover(true);
        plugInTab9.add(channelViewerTab1);

        plugInManager.addTab("Channel Viewer", plugInTab9);

        plugInManager.setSelectedIndex(0);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 822, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(plugInManager, javax.swing.GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 566, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(plugInManager, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel8);

        jPanel2.setMaximumSize(new java.awt.Dimension(100, 20));
        jPanel2.setMinimumSize(new java.awt.Dimension(100, 20));
        jPanel2.setPreferredSize(new java.awt.Dimension(100, 20));
        jPanel2.setRequestFocusEnabled(false);

        statusTextField.setEditable(false);
        statusTextField.setText("Not connected to server");
        statusTextField.setBorder(null);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 310, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 240, Short.MAX_VALUE)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(statusTextField, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 824, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 824, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenuBar2.setMinimumSize(new java.awt.Dimension(600, 2));
        jMenuBar2.setPreferredSize(new java.awt.Dimension(600, 21));

        fileMenu.setText("File");

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setText("New File");
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);
        fileMenu.add(jSeparator4);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText("Open File...");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        openLocalMenuItem.setText("Open Local file...");
        openLocalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLocalMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openLocalMenuItem);

        closeMenuItem.setText("Close File");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(closeMenuItem);
        fileMenu.add(jSeparator5);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText("Save");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);

        saveAsLocalMenuItem.setText("Save As Local...");
        saveAsLocalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsLocalMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsLocalMenuItem);

        saveAllMenuItem1.setText("Save All");
        fileMenu.add(saveAllMenuItem1);
        fileMenu.add(jSeparator6);

        refreshAllMenuItem.setText("Refresh All Files");
        refreshAllMenuItem.setToolTipText("Checks the file server for modifications of all open files.");
        refreshAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshAllMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(refreshAllMenuItem);
        fileMenu.add(jSeparator11);

        exitMenuItem.setText("Exit");
        fileMenu.add(exitMenuItem);

        jMenuBar2.add(fileMenu);

        editMenu.setText("Edit");
        jMenuBar2.add(editMenu);

        modeMenu.setText("Mode");

        buttonGroup2.add(directModeMenuItem);
        directModeMenuItem.setText("Direct");
        directModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directModeMenuItemActionPerformed(evt);
            }
        });
        modeMenu.add(directModeMenuItem);

        buttonGroup2.add(documentedModeMenuItem);
        documentedModeMenuItem.setText("Documented");
        documentedModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                documentedModeMenuItemActionPerformed(evt);
            }
        });
        modeMenu.add(documentedModeMenuItem);

        buttonGroup2.add(testingModeMenuItem);
        testingModeMenuItem.setText("Testing");
        testingModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testingModeMenuItemActionPerformed(evt);
            }
        });
        modeMenu.add(testingModeMenuItem);

        buttonGroup2.add(monitorModeMenuItem);
        monitorModeMenuItem.setText("Monitor");
        monitorModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monitorModeMenuItemActionPerformed(evt);
            }
        });
        modeMenu.add(monitorModeMenuItem);

        jMenuBar2.add(modeMenu);

        runMenu.setText("Run");

        playMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        playMenuItem.setText("Play Single");
        playMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(playMenuItem);

        pauseMenuItem.setText("Pause");
        pauseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(pauseMenuItem);

        stopMenuItem.setText("Stop");
        stopMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(stopMenuItem);

        continuousMenuItem.setText("Run Continuous");
        continuousMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                continuousMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(continuousMenuItem);
        runMenu.add(jSeparator9);

        buttonGroup3.add(singleRunRadioButtonMenuItem);
        singleRunRadioButtonMenuItem.setSelected(true);
        singleRunRadioButtonMenuItem.setText("Single Run Mode");
        singleRunRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                singleRunRadioButtonMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(singleRunRadioButtonMenuItem);

        buttonGroup3.add(sequenceRunRadioButtonMenuItem);
        sequenceRunRadioButtonMenuItem.setText("Sequence Run Mode");
        sequenceRunRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequenceRunRadioButtonMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(sequenceRunRadioButtonMenuItem);
        runMenu.add(jSeparator10);

        parseMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        parseMenuItem.setText("Parse Main File");
        parseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parseMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(parseMenuItem);

        jMenuBar2.add(runMenu);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);
        helpMenu.add(jSeparator2);

        jMenuItem2.setText("License");
        helpMenu.add(jMenuItem2);

        jMenuBar2.add(helpMenu);

        setJMenuBar(jMenuBar2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 824, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 646, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        playActionPerformed();
}//GEN-LAST:event_playButtonActionPerformed

    private void playActionPerformed() {
                
        if(stateMachine.getState().equals(STIStateMachine.State.Paused)) {
            resume();
        }
        else {
            play();
        }
    }

    private void pause() {
        Thread pauseThread = new Thread(new Runnable() {
            public void run() {
                serverConnection.getServerTimingSeqControl().pause();
            }
        });
        pauseThread.start();

    }

    private void resume() {
        Thread resumeThread = new Thread(new Runnable() {

            public void run() {
                serverConnection.getServerTimingSeqControl().resume();
            }
        });
        resumeThread.start();

    }

    private void play() {
        if (stateMachine.getRunType().equals(STIStateMachine.RunType.Single)) {
            runSingle();
        } else {
            runSequence();
        }
    }
    private void runSingle() {

        playThread = new Thread(new Runnable() {

            public void run() {
                if(continuousToggleButton.isSelected()) {
                    serverConnection.getServerTimingSeqControl().runSingleContinuous();
                } else {
                    serverConnection.getServerTimingSeqControl().runSingle(
                        stateMachine.getMode().equals(STIStateMachine.Mode.Documented));
                }
            }
        });
        playThread.start();
    }

    private void runSequence() {
        //prompt for sequence description
        if (stateMachine.getMode().equals(STIStateMachine.Mode.Documented)
                && documentationTab1.promptForSeqDescription()) {
            if (!documentationTab1.showDialogForSeqDescriptionBeforePlay()) {
                return;
            }
        }

        playThread = new Thread(new Runnable() {
            public void run() {
                sequenceManager.runSequence(
                        stateMachine.getMode().equals(STIStateMachine.Mode.Documented),
                        documentationTab1.getTExpSequenceInfo());
            }
        });
        playThread.start();
    }
    private void singleRunRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_singleRunRadioButtonActionPerformed
        stateMachine.changeRunType(STIStateMachine.RunType.Single);
}//GEN-LAST:event_singleRunRadioButtonActionPerformed

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        
        if(stateMachine.getState() == State.Disconnected) {
            stateMachine.connect();
        } 
        else {
            //show disconnection warning dialog
            java.lang.Object[] options = {"Disconnect", "Cancel"};
            int fileOpenDialogResult = JOptionPane.showOptionDialog(this,
                    "Are you sure you want to disconnect from\n " +
                    "the STI server at " + serverConnection.getServerAddress() + " ?",
                    "Disconnect from server",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);
            switch (fileOpenDialogResult) {
                case JOptionPane.OK_OPTION:
                    //"Yes" -- Disconnect
                    stateMachine.disconnect();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    //"Cancel"
                    break;
                }
        }

}//GEN-LAST:event_connectButtonActionPerformed

    private void serverAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverAddressTextFieldActionPerformed
        connectButtonActionPerformed(evt);
}//GEN-LAST:event_serverAddressTextFieldActionPerformed

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        tabbedEditor1.createNewFile();
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        tabbedEditor1.saveAsNetworkActiveTab();
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void saveAsLocalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsLocalMenuItemActionPerformed
        tabbedEditor1.saveAsLocalActiveTab();
    }//GEN-LAST:event_saveAsLocalMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        tabbedEditor1.saveActiveTab();
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        tabbedEditor1.openNetworkFile();
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void openLocalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openLocalMenuItemActionPerformed
        tabbedEditor1.openLocalFile();
    }//GEN-LAST:event_openLocalMenuItemActionPerformed

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        tabbedEditor1.closeFileInActiveTab();
    }//GEN-LAST:event_closeMenuItemActionPerformed

    private void modeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modeComboBoxActionPerformed
        stateMachine.changeMode(((STIStateMachine.Mode) modeComboBox.getSelectedItem()));
}//GEN-LAST:event_modeComboBoxActionPerformed

    private void parseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parseButtonActionPerformed

        tabbedEditor1.selectMainFile();
        if (tabbedEditor1.saveMainFile()) {

            parseThread = new Thread(new Runnable() {

                public void run() {
                    tabbedEditor1.parseFile(serverConnection);
                }
            });
            parseThread.start();
        }
}//GEN-LAST:event_parseButtonActionPerformed

    private void mainFileComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mainFileComboBoxActionPerformed
        tabbedEditor1.mainFileComboBoxActionPerformed(evt);
        stateMachine.changeMainFile();
}//GEN-LAST:event_mainFileComboBoxActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        serverConnection.getServerTimingSeqControl().stop();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        aboutDialog.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        aboutDialog.setVisible(false);
    }//GEN-LAST:event_jButton1ActionPerformed

private void sequenceRunRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceRunRadioButtonActionPerformed
    stateMachine.changeRunType(STIStateMachine.RunType.Sequence);
}//GEN-LAST:event_sequenceRunRadioButtonActionPerformed

private void directModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directModeMenuItemActionPerformed

    if (stateMachine.getState() == STIStateMachine.State.IdleParsed) {
        //all parsed results will be erased -- show warning dialog
        java.lang.Object[] options = {"Yes, switch to Direct Mode", "Cancel"};
        int fileOpenDialogResult = JOptionPane.showOptionDialog(this,
                "Are you sure you want to switch to Direct Mode? \n" +
                "All results from parsing the last timing file will be lost.",
                "Direct Mode Confirmation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
        switch (fileOpenDialogResult) {
            case JOptionPane.OK_OPTION:
                //"Yes" -- Direct Mode
                stateMachine.clearParsedData();
                stateMachine.changeMode(STIStateMachine.Mode.Direct);
                break;
            case JOptionPane.CANCEL_OPTION:
                //"Cancel"
                break;
        }
    }
    else {
        stateMachine.changeMode( STIStateMachine.Mode.Direct );
    }
}//GEN-LAST:event_directModeMenuItemActionPerformed

private void documentedModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_documentedModeMenuItemActionPerformed
    stateMachine.changeMode( STIStateMachine.Mode.Documented );
}//GEN-LAST:event_documentedModeMenuItemActionPerformed

private void testingModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testingModeMenuItemActionPerformed
    stateMachine.changeMode( STIStateMachine.Mode.Testing );
}//GEN-LAST:event_testingModeMenuItemActionPerformed

private void monitorModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monitorModeMenuItemActionPerformed
    stateMachine.changeMode( STIStateMachine.Mode.Monitor );
}//GEN-LAST:event_monitorModeMenuItemActionPerformed

private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
    pause();
}//GEN-LAST:event_pauseButtonActionPerformed

private void singleRunRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_singleRunRadioButtonMenuItemActionPerformed
    stateMachine.changeRunType(STIStateMachine.RunType.Single);
}//GEN-LAST:event_singleRunRadioButtonMenuItemActionPerformed

private void sequenceRunRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceRunRadioButtonMenuItemActionPerformed
    stateMachine.changeRunType(STIStateMachine.RunType.Sequence);
}//GEN-LAST:event_sequenceRunRadioButtonMenuItemActionPerformed

private void playMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playMenuItemActionPerformed
    playButton.doClick();
}//GEN-LAST:event_playMenuItemActionPerformed

private void pauseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseMenuItemActionPerformed
    pauseButton.doClick();
}//GEN-LAST:event_pauseMenuItemActionPerformed

private void stopMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopMenuItemActionPerformed
    stopButton.doClick();
}//GEN-LAST:event_stopMenuItemActionPerformed

private void continuousToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousToggleButtonActionPerformed
    playButton.doClick();
}//GEN-LAST:event_continuousToggleButtonActionPerformed

private void continuousMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousMenuItemActionPerformed
    if (continuousToggleButton.isEnabled()) {
        if (continuousToggleButton.isSelected()) {
            playButton.doClick();
        } else {
            continuousToggleButton.doClick();
        }
    }
}//GEN-LAST:event_continuousMenuItemActionPerformed

private void parseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parseMenuItemActionPerformed
    parseButton.doClick();
}//GEN-LAST:event_parseMenuItemActionPerformed

private void refreshAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshAllMenuItemActionPerformed
    tabbedEditor1.refreshAllOpenFiles();
}//GEN-LAST:event_refreshAllMenuItemActionPerformed
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception unused) {
            //unused.printStackTrace(); // Ignore exception because we can't do anything.  Will use default.
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new sti_console().setVisible(true);
            }
        });
    }
    
   
    /* properties files
     * 
     *     
     private Properties prop = new Properties();
     private FileInputStream fis = null;
            try {
//            InputStream is = this.getClass().getClassLoader().getResourceAsStream("edu.stanford.atom.sti.client.resources.stiServer.properties");
//            fis = new FileInputStream("edu.stanford.atom.sti.client.resources.stiServer.properties");
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("edu/stanford/atom/sti/client/resources/stiServer.properties");
            prop.load(is);
            prop.setProperty("STISERVERADDRESS", serverAddressTextField.getText()+"J");
            serverAddressTextField.setText(prop.getProperty("STISERVERADDRESS").toString());
        //    FileOutputStream fos = new FileOutputStream( new File( ( this.getClass().getClassLoader().getResource("edu/stanford/atom/sti/client/resources/stiServer.properties") ).getFile() ));
        //    prop.store(fos, "");
        } catch(Exception e) {
            e.printStackTrace();
        }

    */


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog aboutDialog;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JLabel buildDateLabel;
    private javax.swing.JLabel buildTimeLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private edu.stanford.atom.sti.client.gui.RunTab.ChannelViewerTab channelViewerTab1;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JButton connectButton;
    private javax.swing.JMenuItem continuousMenuItem;
    private javax.swing.JToggleButton continuousToggleButton;
    private javax.swing.JRadioButtonMenuItem directModeMenuItem;
    private edu.stanford.atom.sti.client.gui.RunTab.DocumentationTab documentationTab1;
    private javax.swing.JRadioButtonMenuItem documentedModeMenuItem;
    private javax.swing.JMenu editMenu;
    private edu.stanford.atom.sti.client.gui.EventsTab.EventsTab eventsTab1;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JSplitPane jSplitPane4;
    private javax.swing.JSplitPane jSplitPane5;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JComboBox mainFileComboBox;
    private javax.swing.JComboBox modeComboBox;
    private javax.swing.JMenu modeMenu;
    private javax.swing.JRadioButtonMenuItem monitorModeMenuItem;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openLocalMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JButton parseButton;
    private javax.swing.JMenuItem parseMenuItem;
    private javax.swing.JButton pauseButton;
    private javax.swing.JMenuItem pauseMenuItem;
    private javax.swing.JButton playButton;
    private javax.swing.JMenuItem playMenuItem;
    private edu.stanford.atom.sti.client.gui.PlugInManager plugInManager;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab1;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab2;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab3;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab4;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab5;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab6;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab7;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab8;
    private edu.stanford.atom.sti.client.gui.PlugInTab plugInTab9;
    private javax.swing.JMenuItem refreshAllMenuItem;
    private edu.stanford.atom.sti.client.gui.DevicesTab.RegisteredDevicesTab registeredDevicesTab1;
    private javax.swing.JMenu runMenu;
    private javax.swing.JPanel runRadioButtonPanel;
    private edu.stanford.atom.sti.client.gui.RunTab.RunTab runTab1;
    private javax.swing.JMenuItem saveAllMenuItem1;
    private javax.swing.JMenuItem saveAsLocalMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JRadioButton sequenceRunRadioButton;
    private javax.swing.JRadioButtonMenuItem sequenceRunRadioButtonMenuItem;
    private javax.swing.JTextField serverAddressTextField;
    private javax.swing.JRadioButton singleRunRadioButton;
    private javax.swing.JRadioButtonMenuItem singleRunRadioButtonMenuItem;
    private javax.swing.JTextField statusTextField;
    private javax.swing.JButton stopButton;
    private javax.swing.JMenuItem stopMenuItem;
    private edu.stanford.atom.sti.client.gui.FileEditorTab.TabbedEditor tabbedEditor1;
    private javax.swing.JRadioButtonMenuItem testingModeMenuItem;
    private edu.stanford.atom.sti.client.gui.EventsTab.TimingDiagramTab timingDiagramTab1;
    private edu.stanford.atom.sti.client.gui.VariablesTab.VariableTab variableTab1;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
    
}
