package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel displaying the current directory path with navigation buttons.
 * Includes address bar and navigation controls.
 */
public class PathBarPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PathBarPanel.class);
    
    private final FileSystemModel fileSystemModel;
    private final JTextField pathField;
    private final JButton upButton;
    private final JButton homeButton;
    private final JButton refreshButton;
    private final JComboBox<String> driveComboBox;
    private final FileSystemView fileSystemView;
    
    private boolean updatingUI = false;
    
    /**
     * Creates a new PathBarPanel.
     */
    public PathBarPanel() {
        this.fileSystemModel = FileSystemModel.getInstance();
        this.fileSystemView = FileSystemView.getFileSystemView();
        
        setLayout(new BorderLayout(5, 0));
        setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        // Up button
        upButton = UIUtils.createToolbarButton("↑", e -> {
            File parent = fileSystemModel.getParentDirectory();
            if (parent != null) {
                UIUtils.runWithLoadingIndicator(this, 
                        () -> fileSystemModel.setCurrentDirectory(parent),
                        "Loading parent directory...");
            }
        });
        upButton.setToolTipText("Go to parent directory");
        controlPanel.add(upButton);
        
        // Home button
        homeButton = UIUtils.createToolbarButton("🏠", e -> {
            File homeDir = new File(System.getProperty("user.home"));
            UIUtils.runWithLoadingIndicator(this, 
                    () -> fileSystemModel.setCurrentDirectory(homeDir),
                    "Loading home directory...");
        });
        homeButton.setToolTipText("Go to home directory");
        controlPanel.add(homeButton);
        
        // Refresh button
        refreshButton = UIUtils.createToolbarButton("↻", e -> {
            File currentDir = fileSystemModel.getCurrentDirectory();
            UIUtils.runWithLoadingIndicator(this, 
                    () -> fileSystemModel.setCurrentDirectory(currentDir),
                    "Refreshing directory...");
        });
        refreshButton.setToolTipText("Refresh current directory");
        controlPanel.add(refreshButton);
        
        // Drive selection combo box
        driveComboBox = new JComboBox<>();
        populateDriveComboBox();
        driveComboBox.setPreferredSize(new Dimension(75, 25));
        driveComboBox.addActionListener(e -> {
            if (updatingUI) return;
            
            String selectedDrive = (String) driveComboBox.getSelectedItem();
            if (selectedDrive != null && !selectedDrive.isEmpty()) {
                File driveRoot = new File(selectedDrive);
                UIUtils.runWithLoadingIndicator(this, 
                        () -> fileSystemModel.setCurrentDirectory(driveRoot),
                        "Loading drive...");
            }
        });
        controlPanel.add(driveComboBox);
        
        add(controlPanel, BorderLayout.WEST);
        
        // Path text field
        pathField = new JTextField();
        pathField.addActionListener(e -> {
            if (updatingUI) return;
            
            String path = pathField.getText().trim();
            if (!path.isEmpty()) {
                File dir = new File(path);
                UIUtils.runWithLoadingIndicator(this, 
                        () -> {
                            if (dir.exists() && dir.isDirectory()) {
                                fileSystemModel.setCurrentDirectory(dir);
                            } else {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this, 
                                            "Invalid directory: " + path,
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                                    updatePathField(fileSystemModel.getCurrentDirectory());
                                });
                            }
                        },
                        "Loading directory...");
            }
        });
        add(pathField, BorderLayout.CENTER);
        
        // Set initial path
        updatePathField(fileSystemModel.getCurrentDirectory());
        
        // Add observer to update when directory changes
        fileSystemModel.addDirectoryChangeObserver(this::updatePathField);
        
        logger.debug("PathBarPanel initialized");
    }
    
    /**
     * Updates the path field with the current directory.
     * 
     * @param currentDirectory The current directory
     */
    private void updatePathField(File currentDirectory) {
        SwingUtilities.invokeLater(() -> {
            try {
                updatingUI = true;
                
                // Update path field
                pathField.setText(currentDirectory.getCanonicalPath());
                pathField.setCaretPosition(pathField.getText().length());
                
                // Update drive selection
                String rootPath = getRootPath(currentDirectory);
                for (int i = 0; i < driveComboBox.getItemCount(); i++) {
                    String item = driveComboBox.getItemAt(i);
                    if (item.equals(rootPath)) {
                        driveComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                
                // Enable/disable up button
                upButton.setEnabled(currentDirectory.getParentFile() != null);
                
                logger.debug("Updated path bar: {}", currentDirectory.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error updating path bar", e);
            } finally {
                updatingUI = false;
            }
        });
    }
    
    /**
     * Gets the root path for a file.
     * 
     * @param file The file to get root for
     * @return The root path
     */
    private String getRootPath(File file) {
        File root = file;
        while (root.getParentFile() != null) {
            root = root.getParentFile();
        }
        return root.getAbsolutePath();
    }
    
    /**
     * Populates the drive combo box with available drives.
     */
    private void populateDriveComboBox() {
        File[] roots = File.listRoots();
        if (roots != null) {
            updatingUI = true;
            driveComboBox.removeAllItems();
            
            for (File root : roots) {
                String displayName = fileSystemView.getSystemDisplayName(root);
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = root.getAbsolutePath();
                } else {
                    // Keep the drive letter if display name is too long
                    if (displayName.length() > 20) {
                        displayName = root.getAbsolutePath();
                    }
                }
                driveComboBox.addItem(root.getAbsolutePath());
            }
            
            updatingUI = false;
        }
    }
}