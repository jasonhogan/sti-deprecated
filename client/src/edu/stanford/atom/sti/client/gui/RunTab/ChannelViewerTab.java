/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * NewJPanel.java
 *
 * Created on Oct 6, 2010, 8:24:15 AM
 */

package edu.stanford.atom.sti.client.gui.RunTab;

import java.util.*;
import edu.stanford.atom.sti.client.comm.bl.device.Device;
import edu.stanford.atom.sti.client.comm.bl.device.DeviceCollectionListener;
import edu.stanford.atom.sti.client.comm.bl.device.DeviceEvent;
import edu.stanford.atom.sti.client.comm.bl.device.DeviceManager;
import edu.stanford.atom.sti.client.comm.bl.TChannelDecode;
import edu.stanford.atom.sti.corba.Types.TValMixed;
import edu.stanford.atom.sti.corba.Types.*;
import  javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import edu.stanford.atom.sti.client.gui.checktree.*;
import javax.swing.table.*;
import edu.stanford.atom.sti.client.gui.oscilloscope.*;
import java.util.Hashtable;

/**
 *
 * @author Susannah Dickerson
 */
public class ChannelViewerTab extends javax.swing.JPanel implements DeviceCollectionListener {

//    private SortedMap deviceMap = new TreeMap();
    private Hashtable<String, Device> deviceMap = new Hashtable<String, Device>();
    private CheckTreeManager checkTreeManager;
    private ArrayList<ArrayList> selectedDeviceChannels;
//    private OscilloscopePanel oscilloscopePanel;

//    private SortedMap deviceMap = Collections.synchronizedSortedMap(new TreeMap());

//    private Vector<Device> deviceVector = new Vector<Device>();

    /** Creates new form NewJPanel */
    public ChannelViewerTab() {

 //       oscScrollPane = new javax.swing.JScrollPane();
        initComponents();
        checkTreeManager = new CheckTreeManager(selectedDeviceTree);
        addChannelDialog.pack();
//        
//        oscilloscopePanel = new OscilloscopePanel();
//
//        oscScrollPane.setViewportView(oscilloscopePanel);
//        oscScrollPane.setBounds(0, 0, 700, 400);
//        jLayeredPane1.add(oscScrollPane, 0);


 //       jLayeredPane1.add(oscilloscopePanel, jLayeredPane1.DEFAULT_LAYER);

  //      jSplitPane1.setTopComponent(jLayeredPane1);
    }

    public void addDevice(Device device) {
//        if( !deviceVector.contains(device) ) {
//
//            deviceVector.add(device);
//
//            DefaultListModel model = (DefaultListModel) dialogDeviceList.getModel();
//            model.addElement(device.name());
//            dialogDeviceList.setModel(model);
//        }
        if (!deviceMap.containsKey(device.name()))
        {
            deviceMap.put(device.name(), device);

            dialogDeviceList.setListData(deviceMap.keySet().toArray());
        }
    }

    public void removeDevice(Device device) {
//        if (deviceVector.contains(device))
//        {
//            DefaultListModel model = (DefaultListModel) dialogDeviceList.getModel();
//            model.remove(deviceVector.indexOf(device));
//            dialogDeviceList.setModel(model);
//
//            deviceMap.remove(device);
//        }

        if (deviceMap.containsKey(device.name()))
        {
            deviceMap.remove(device.name());

            dialogDeviceList.setListData(deviceMap.keySet().toArray());
        }

    }
    public void handleDeviceEvent(DeviceEvent evt) {
        deviceMap.get(evt.getDevice());
    }
    public void setDeviceManagerStatus(DeviceManager.DeviceManagerStatus status) {
        switch(status) {
            case Refreshing:
                //This updates the GUI and so it must use invokeLater to execute
                //on the event dispatch thread after all repainting is finished.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                    }
                });
                break;
            case Idle:
            default:
                //This updates the GUI and so it must use invokeLater to execute
                //on the event dispatch thread after all repainting is finished.
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                    }
                });
                break;
        }

    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addChannelDialog = new javax.swing.JFrame();
        jScrollPane4 = new javax.swing.JScrollPane();
        dialogDeviceList = new javax.swing.JList();
        dialogaddDeviceButton = new javax.swing.JButton();
        dialogRemoveDeviceButton = new javax.swing.JButton();
        dialogOKButton = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        selectedDeviceTree = new javax.swing.JTree();
        jSeparator2 = new javax.swing.JSeparator();
        addChannelsButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        oscilloscopePanel = new edu.stanford.atom.sti.client.gui.oscilloscope.OscilloscopePanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        channelTable = new javax.swing.JTable();

        addChannelDialog.setTitle("Add/Remove Channels");

        dialogDeviceList.setModel(new DefaultListModel());
        jScrollPane4.setViewportView(dialogDeviceList);

        dialogaddDeviceButton.setText("Add >>");
        dialogaddDeviceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dialogaddDeviceButtonActionPerformed(evt);
            }
        });

        dialogRemoveDeviceButton.setText("<< Remove");
        dialogRemoveDeviceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dialogRemoveDeviceButtonActionPerformed(evt);
            }
        });

        dialogOKButton.setText("OK");
        dialogOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dialogOKButtonActionPerformed(evt);
            }
        });

        selectedDeviceTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Devices")));
        jScrollPane5.setViewportView(selectedDeviceTree);

        jScrollPane6.setViewportView(jScrollPane5);

        javax.swing.GroupLayout addChannelDialogLayout = new javax.swing.GroupLayout(addChannelDialog.getContentPane());
        addChannelDialog.getContentPane().setLayout(addChannelDialogLayout);
        addChannelDialogLayout.setHorizontalGroup(
            addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addChannelDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                    .addGroup(addChannelDialogLayout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(dialogaddDeviceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(dialogRemoveDeviceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(10, 10, 10)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE))
                    .addComponent(dialogOKButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        addChannelDialogLayout.setVerticalGroup(
            addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addChannelDialogLayout.createSequentialGroup()
                .addGroup(addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(addChannelDialogLayout.createSequentialGroup()
                        .addGap(100, 100, 100)
                        .addComponent(dialogaddDeviceButton)
                        .addGap(32, 32, 32)
                        .addComponent(dialogRemoveDeviceButton))
                    .addGroup(addChannelDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(addChannelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane6, 0, 0, Short.MAX_VALUE)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE))))
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dialogOKButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        addChannelsButton.setText("Add/Remove Channels...");
        addChannelsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addChannelsButtonActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(1.0);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(600, 417));
        jScrollPane1.setViewportView(oscilloscopePanel);

        jSplitPane1.setTopComponent(jScrollPane1);

        channelTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Device", "Channel", "Name", "Type", "Value", "Include"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Short.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(channelTable);

        jSplitPane1.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addChannelsButton)
                        .addContainerGap(614, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jSeparator1)
                        .addGap(18, 18, 18))))
            .addComponent(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(addChannelsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addChannelsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addChannelsButtonActionPerformed
        // TODO add your handling code here:

        addChannelDialog.setVisible(true);
    }//GEN-LAST:event_addChannelsButtonActionPerformed

    private void dialogaddDeviceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dialogaddDeviceButtonActionPerformed
        // TODO add your handling code here:
        DefaultMutableTreeNode newDeviceNode = null;
        Device newSelectedDevice = null;
        String newSelectedDeviceName;
        int[] selectedDeviceIndices = dialogDeviceList.getSelectedIndices();

        DefaultTreeModel treeModel = (DefaultTreeModel) selectedDeviceTree.getModel();

        List<String> keyList = new ArrayList<String>(deviceMap.keySet());

        for (int item : selectedDeviceIndices)
        {
            newSelectedDeviceName = keyList.get(item);
            newSelectedDevice = (Device) deviceMap.get(newSelectedDeviceName);

            TChannel[] channels = newSelectedDevice.getChannels();

            newDeviceNode = new DefaultMutableTreeNode(newSelectedDeviceName);
            DefaultMutableTreeNode newChild = null;

            for (TChannel channel : channels)
            {
                TChannelDecode decoded = new TChannelDecode(channel);
                // load only channels that are inputs.
                if (decoded.IOType().equals("Input"))
                {
                    newChild = new DefaultMutableTreeNode(channel.channel);
                    newDeviceNode.add(newChild);
                }
            }

            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();

            if (!isNodeInChildrenByString(rootNode, newDeviceNode))
            {
                
                rootNode.add(newDeviceNode);
            }
        }

        treeModel.reload();

    }//GEN-LAST:event_dialogaddDeviceButtonActionPerformed

    private boolean isNodeInChildrenByString(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode newNode)
    {
        DefaultMutableTreeNode node = null;
        boolean result = false;
        Enumeration e = parentNode.children();

        //This shouldn't happen, since all nodes should have objects...
        if (newNode.toString() == null)
        {
            return result;
        }

        while (e.hasMoreElements())
        {
            node = (DefaultMutableTreeNode) e.nextElement();
            if (newNode.toString().equals(node.toString()))
            {
                result = true;
            }
        }

        return result;
    }

    private void dialogRemoveDeviceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dialogRemoveDeviceButtonActionPerformed
        // TODO add your handling code here:
        TreePath[] treePaths = selectedDeviceTree.getSelectionPaths();
        DefaultTreeModel treeModel = (DefaultTreeModel) selectedDeviceTree.getModel();

        if (treePaths != null) {
            for (TreePath treePath : treePaths){
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
                             (treePath.getLastPathComponent());
                if (currentNode != null)
                    treeModel.removeNodeFromParent(currentNode);
            }
        }

        // Either there was no selection, or the root was selected.
        //toolkit.beep();

        treeModel.reload();

    }//GEN-LAST:event_dialogRemoveDeviceButtonActionPerformed

    private void dialogOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dialogOKButtonActionPerformed
        // TODO add your handling code here:
        TreePath[] selectedPaths = checkTreeManager.getSelectionModel().getSelectionPaths();

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) selectedDeviceTree.getModel().getRoot();

        ArrayList<ArrayList> deviceChannelIDs = new ArrayList<ArrayList>();

        if (rootNode != null && selectedPaths != null)
        {
            
            Device device;
            String deviceName;
            //Check to see what rows are selected or deselected.
            for(TreePath path : selectedPaths)
            {
                Object[] pathComponents = path.getPath();
                switch (pathComponents.length)
                {
                    case 1:
                        //root node only; add all channels
                        addAllChannelsToArrayList(deviceChannelIDs,rootNode);
                        break;
                    case 2:
                        //device node; add all device's channels
                        deviceName = pathComponents[1].toString();
                        addDeviceChannelsToArrayList(deviceChannelIDs,deviceName);
                        break;
                    case 3:
                        //channel node; add only this channel
                        deviceName = pathComponents[1].toString();
                        final short channelID;
                        channelID = (Short) ((DefaultMutableTreeNode) pathComponents[2]).getUserObject();

                        addChannelToArrayList(deviceChannelIDs,deviceName,channelID);
                        break;
                    default:
                        //either empty path or weird path
                        break;
                }

            }

            
        }

        setChannelTable(deviceChannelIDs);
        
        addChannelDialog.setVisible(false);

        loadTracesToOscilloscope(channelTable);

    }//GEN-LAST:event_dialogOKButtonActionPerformed

    private void loadTracesToOscilloscope(javax.swing.JTable cTable) {
        int rowCount = cTable.getRowCount();

        oscilloscopePanel.oscilloscope.removeAllTraces();
        oscilloscopePanel.oscilloscope.resetTimer();
        TValMixed valueIn = new TValMixed();
        valueIn.stringVal("");

        for (int i = 0; i < rowCount; i++) {
            oscilloscopePanel.oscilloscope.addChannel(
                    (Device) deviceMap.get((String) cTable.getValueAt(i,0)),
                    (Short) cTable.getValueAt(i,1), valueIn);
        }
    }
    private void addChannelToArrayList(ArrayList<ArrayList> list, final String deviceName, final short channelID) {
        ArrayList tempArrayList = new ArrayList(){{
                                add(deviceName);
                                add(channelID);
                            }};
        if (!list.contains(tempArrayList))
            list.add(tempArrayList);
    }

    private void addDeviceChannelsToArrayList(ArrayList<ArrayList> list, final String deviceName) {

        Device device = (Device) deviceMap.get(deviceName);
        TChannel[] channels = device.getChannels();
        for (final TChannel channel : channels)
        {
            TChannelDecode decoded = new TChannelDecode(channel);
            if (decoded.IOType().equals("Input"))
                addChannelToArrayList(list,deviceName,channel.channel);
        }
    }

    private void addAllChannelsToArrayList(ArrayList<ArrayList> list, DefaultMutableTreeNode rootNode) {

        String deviceName;
        Enumeration children = rootNode.children();
        
        while (children.hasMoreElements())
        {
            deviceName = (String)((DefaultMutableTreeNode) children.nextElement()).getUserObject();
            addDeviceChannelsToArrayList(list, deviceName);
        }
    }


    private Object[] makeTableRow(String deviceName, short channelID) {
        Device device = (Device) deviceMap.get(deviceName);

        TChannelDecode channelDecode = new TChannelDecode(device.getChannel(channelID));

        Object[] newRow = new Object[]{deviceName, channelID,"",channelDecode.IOType(),"", true};

        return newRow;
    }

    private Object[] makeTableRow(ArrayList deviceChannelID) {
        return makeTableRow((String) deviceChannelID.get(0), (Short) deviceChannelID.get(1));
    }

    private void setChannelTable(ArrayList<ArrayList> deviceChannelIDs) {

        DefaultTableModel tableModel = (DefaultTableModel) channelTable.getModel();
        
        int rowCount = tableModel.getRowCount();
        List<Integer> rowsToDelete = new ArrayList<Integer>();

        //reverse order so that rowsToDelete goes from high to low so that the
        //rows are deleted from the end.
        for (int i = rowCount-1; i >= 0; i--)
        {
            Iterator<ArrayList> it = deviceChannelIDs.iterator();
            int dCIDsCount = deviceChannelIDs.size();

            while (it.hasNext()) {
                ArrayList next = it.next();
                // if deviceChannelIDs contains the row, delete it from
                // deviceChannelIDs so as not to overwrite the row
                if (tableModel.getValueAt(i, 0).equals(next.get(0)) &&
                    tableModel.getValueAt(i, 1).equals(next.get(1))) {
                    it.remove();
                }
            }

            // if the row was not found in deviceChannelIDs, it should be
            // deleted from the table
            if (deviceChannelIDs.size() == dCIDsCount)
                rowsToDelete.add(i);
        }

        //Remove rows
        for (Integer item : rowsToDelete)
            tableModel.removeRow(item);

        //Add rows
        for (ArrayList item : deviceChannelIDs)
            tableModel.addRow(makeTableRow(item));
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFrame addChannelDialog;
    private javax.swing.JButton addChannelsButton;
    private javax.swing.JTable channelTable;
    private javax.swing.JList dialogDeviceList;
    private javax.swing.JButton dialogOKButton;
    private javax.swing.JButton dialogRemoveDeviceButton;
    private javax.swing.JButton dialogaddDeviceButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSplitPane jSplitPane1;
    private edu.stanford.atom.sti.client.gui.oscilloscope.OscilloscopePanel oscilloscopePanel;
    private javax.swing.JTree selectedDeviceTree;
    // End of variables declaration//GEN-END:variables
    private javax.swing.JScrollPane oscScrollPane;

}
