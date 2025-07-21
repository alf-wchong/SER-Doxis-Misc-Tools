package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel displaying a tree of directories.
 * Shows only immediate subdirectories of the current root directory.
 */
public class DirectoryTreePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryTreePanel.class);
    
    private final FileSystemModel fileSystemModel;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JTree directoryTree;
    private final FileSystemView fileSystemView;
    
    /**
     * Creates a new DirectoryTreePanel.
     */
    public DirectoryTreePanel() {
        this.fileSystemModel = FileSystemModel.getInstance();
        this.fileSystemView = FileSystemView.getFileSystemView();
        
        setLayout(new BorderLayout());
        
        // Create tree model with a root node
        rootNode = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(rootNode);
        
        // Create tree with custom cell renderer
        directoryTree = new JTree(treeModel);
        directoryTree.setCellRenderer(new DirectoryTreeCellRenderer());
        directoryTree.setRootVisible(true);
        directoryTree.setShowsRootHandles(true);
        directoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        // Add scrollbars
        JScrollPane scrollPane = new JScrollPane(directoryTree);
        add(scrollPane, BorderLayout.CENTER);
        
        // Set initial directory
        updateDirectoryTree(fileSystemModel.getCurrentDirectory());
        
        // Add selection listener
        directoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) 
                    directoryTree.getLastSelectedPathComponent();
            
            if (selectedNode != null && selectedNode.getUserObject() instanceof File) {
                File selectedDir = (File) selectedNode.getUserObject();
                
                if (selectedDir.isDirectory()) {
                    UIUtils.runWithLoadingIndicator(this, 
                            () -> fileSystemModel.setCurrentDirectory(selectedDir),
                            "Loading directory...");
                }
            }
        });
        
        // Add observer to update when directory changes
        fileSystemModel.addDirectoryChangeObserver(this::updateDirectoryTree);
        
        logger.debug("DirectoryTreePanel initialized");
    }
    
    /**
     * Updates the directory tree to show subdirectories of the given directory.
     * 
     * @param currentDirectory The current directory
     */
    private void updateDirectoryTree(File currentDirectory) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Update root node to represent current directory
                rootNode.setUserObject(currentDirectory);
                rootNode.removeAllChildren();
                
                // Add subdirectories as children
                List<File> subdirs = fileSystemModel.getSubdirectories();
                for (File subdir : subdirs) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(subdir);
                    rootNode.add(childNode);
                }
                
                // Notify tree model of changes
                treeModel.nodeStructureChanged(rootNode);
                
                // Expand root node
                directoryTree.expandPath(new TreePath(rootNode.getPath()));
                
                logger.debug("Updated directory tree: {} ({} subdirectories)", 
                        currentDirectory.getAbsolutePath(), subdirs.size());
            } catch (Exception e) {
                logger.error("Error updating directory tree", e);
            }
        });
    }
    
    /**
     * Custom cell renderer for the directory tree.
     */
    private class DirectoryTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof File) {
                File file = (File) userObject;
                String name = fileSystemView.getSystemDisplayName(file);
                if (name == null || name.isEmpty()) {
                    name = file.getName();
                    if (name.isEmpty()) {
                        name = file.getPath();
                    }
                }
                setText(name);
                setIcon(fileSystemView.getSystemIcon(file));
                setToolTipText(file.getAbsolutePath());
            }
            
            return this;
        }
    }
}