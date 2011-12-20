/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.corecomponents;

import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.Lookup;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataContent;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataContentViewer;
import org.sleuthkit.autopsy.datamodel.ContentUtils;
import org.sleuthkit.autopsy.datamodel.DataConversion;
import org.sleuthkit.datamodel.Content;

/**
 * Top component that organizes all of the data content viewers.  Doing a lookup on this class will
 * always return the default instance (which is created at startup). 
 */
// Registered as a service provider in layer.xml
public final class DataContentTopComponent extends TopComponent implements DataContent, ChangeListener {

    // reference to the "default" TC that always stays open
    private static DataContentTopComponent defaultInstance;
    private Node currentNode;
    // set to true if this is the TC that always stays open and is the default place to display content
    private boolean isDefault;
    // Different DataContentViewers
    private List<UpdateWrapper> viewers = new ArrayList<UpdateWrapper>();
    // contains a list of the undocked TCs
    private static ArrayList<DataContentTopComponent> newWindowList = new ArrayList<DataContentTopComponent>();
    private static final String PREFERRED_ID = "DataContentTopComponent";
    private static final String DEFAULT_NAME = NbBundle.getMessage(DataContentTopComponent.class, "CTL_DataContentTopComponent");
    private static final String TOOLTIP_TEXT = NbBundle.getMessage(DataContentTopComponent.class, "HINT_DataContentTopComponent");
    
    private DataContentTopComponent(boolean isDefault, String name) {
        initComponents();
        setName(name);
        setToolTipText(TOOLTIP_TEXT);

        this.isDefault = isDefault;
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.valueOf(isDefault)); // prevent option to close compoment in GUI

        // set the tab to listen to any tab change (see the "stateChange" method)
        this.dataContentTabbedPane.addChangeListener(this);
    }

    private static class UpdateWrapper {

        private DataContentViewer wrapped;
        private boolean outdated;

        UpdateWrapper(DataContentViewer wrapped) {
            this.wrapped = wrapped;
            this.outdated = true;
        }

        void setNode(Node selectedNode) {
            this.wrapped.setNode(selectedNode);
            this.outdated = false;
        }

        void resetComponent() {
            this.wrapped.resetComponent();
            this.outdated = true;
        }

        boolean isOutdated() {
            return this.outdated;
        }

        boolean isSupported(Node node) {
            return this.wrapped.isSupported(node);
        }
        
        boolean isPreferred(ContentNode node, boolean isSupported) {
            return this.wrapped.isPreferred(node, isSupported);
        }
    }

    /**
     * This createInstance method is used to create an undocked instance
     * for the "View in New Window" feature.
     * @param filePath path of given file node
     * @param givenNode node to view content of
     * @return newly undocked instance
     */
    public static DataContentTopComponent createUndocked(String filePath, Node givenNode) {

        DataContentTopComponent dctc = new DataContentTopComponent(false, filePath);
        dctc.componentOpened();
        dctc.setNode(givenNode);

        newWindowList.add(dctc);

        return dctc;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dataContentTabbedPane = new javax.swing.JTabbedPane();

        dataContentTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        dataContentTabbedPane.setPreferredSize(new java.awt.Dimension(700, 5));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(dataContentTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(dataContentTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane dataContentTabbedPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized defaultInstance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized DataContentTopComponent getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new DataContentTopComponent(true, DEFAULT_NAME);
        }
        return defaultInstance;
    }

    /**
     * Obtain the default DataContentTopComponent defaultInstance. Never call {@link #getDefault} directly!
     */
    public static synchronized DataContentTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(DataContentTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof DataContentTopComponent) {
            return (DataContentTopComponent) win;
        }
        Logger.getLogger(DataContentTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public void componentOpened() {
        // Add all the DataContentViewer to the tabbed pannel.
        // (Only when the it's opened at the first time: tabCount = 0)
        int totalTabs = dataContentTabbedPane.getTabCount();
        if (totalTabs == 0) {
            // find all dataContentViewer and add them to the tabbed pane
            for (DataContentViewer factory : Lookup.getDefault().lookupAll(DataContentViewer.class)) {
                DataContentViewer dcv = factory.getInstance();
                this.viewers.add(new UpdateWrapper(dcv));
                dataContentTabbedPane.addTab(dcv.getTitle(), null,
                        dcv.getComponent(), dcv.getToolTip());
            }
        }

        resetTabs(currentNode);
    }

    @Override
    public void componentClosed() {
        
        // clear all set nodes
        for(UpdateWrapper dcv : viewers) {
            dcv.setNode(null);
        }
        
        if (!this.isDefault) {
            newWindowList.remove(this);
        }
    }

    @Override
    protected String preferredID() {
        if (this.isDefault) {
            return PREFERRED_ID;
        } else {
            return this.getName();
        }
    }

    @Override
    public void setNode(Node selectedNode) {
        // change the cursor to "waiting cursor" for this operation
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            
            
            String defaultName = NbBundle.getMessage(DataContentTopComponent.class, "CTL_DataContentTopComponent");
            // set the file path
            if (selectedNode == null) {
                setName(defaultName);
            } else {
                Content content = selectedNode.getLookup().lookup(Content.class);
                if (content != null) {
                    String path = DataConversion.getformattedPath(ContentUtils.getDisplayPath(selectedNode.getLookup().lookup(Content.class)), 0);
                    setName(path);
                } else {
                    setName(defaultName);
                }
            }

            currentNode = selectedNode;

            resetTabs(selectedNode);

            // set the display on the current active tab
            int currentActiveTab = dataContentTabbedPane.getSelectedIndex();
            if (currentActiveTab != -1) {
                UpdateWrapper dcv = viewers.get(currentActiveTab);
                dcv.setNode(selectedNode);
            }
        } finally {
            this.setCursor(null);
        }
    }

    @Override
    public boolean canClose() {
        return (!this.isDefault) || !Case.existsCurrentCase() || Case.getCurrentCase().getRootObjectsCount() == 0; // only allow this window to be closed when there's no case opened or no image in this case
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        JTabbedPane pane = (JTabbedPane) evt.getSource();

        // Get and set current selected tab
        int currentTab = pane.getSelectedIndex();
        if (currentTab != -1) {
            UpdateWrapper dcv = viewers.get(currentTab);
            if (dcv.isOutdated()) {
                // change the cursor to "waiting cursor" for this operation
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    dcv.setNode(currentNode);
                } finally {
                    this.setCursor(null);
                }
            }
        }
    }

    /**
     * Resets the tabs based on the selected Node. If the selected node is null
     * or not supported, disable that tab as well.
     *
     * @param selectedNode  the selected content Node
     */
    public void resetTabs(Node selectedNode) {

        int totalTabs = dataContentTabbedPane.getTabCount();

        if (totalTabs > 0) { // make sure there are tabs to reset
            int tempIndex = dataContentTabbedPane.getSelectedIndex();
            for (int i = 0; i < totalTabs; i++) {
                UpdateWrapper dcv = viewers.get(i);
                dcv.resetComponent();             

                // disable an unsupported tab (ex: picture viewer)
                boolean dcvSupported = dcv.isSupported(selectedNode);
                if (! dcvSupported) {
                    dataContentTabbedPane.setEnabledAt(i, false);

                    // change the tab selection if it's the current selection
                    if (tempIndex == i) {
                        if (i > 0) {
                            dataContentTabbedPane.setSelectedIndex(0);
                        } else {
                            dataContentTabbedPane.setSelectedIndex(1);
                        }
                    }
                } else {
                    dataContentTabbedPane.setEnabledAt(i, true);
                    if (dcv.isPreferred(selectedNode, dcvSupported))
                        dataContentTabbedPane.setSelectedIndex(i);
                    
                }
            }
            int newIndex = dataContentTabbedPane.getSelectedIndex();
            // set the display of the tab
            viewers.get(newIndex).setNode(selectedNode);
        }
    }

    /**
     * Get the tab pane
     * @return tab pane with individual {@link DataContentViewer}s
     */
    public JTabbedPane getTabPanels() {
        return this.dataContentTabbedPane;
    }

    @Override
    public TopComponent getTopComponent() {
        return this;
    }

    /**
     * Returns a list of the non-default (main) TopComponents
     * @return
     */
    public static List<DataContentTopComponent> getNewWindowList() {
        return newWindowList;
    }
}
