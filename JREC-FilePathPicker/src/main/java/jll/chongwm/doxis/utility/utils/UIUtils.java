package jll.chongwm.doxis.utility.utils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for UI-related helper methods.
 */
public class UIUtils {
    private static final Logger logger = LoggerFactory.getLogger(UIUtils.class);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Creates a standardized button with the given text and action.
     * 
     * @param text Button text
     * @param action Action to perform on click
     * @return Configured JButton
     */
    public static JButton createButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }
    
    /**
     * Creates a standardized toolbar button with the given text and action.
     * 
     * @param text Button text
     * @param action Action to perform on click
     * @return Configured JButton with toolbar styling
     */
    public static JButton createToolbarButton(String text, ActionListener action) {
        JButton button = createButton(text, action);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2, 8, 2, 8));
        return button;
    }
    
    /**
     * Creates a transparent glass pane with loading spinner and message.
     * 
     * @param message Loading message to display
     * @return Configured JPanel to use as glass pane
     */
    public static JPanel createLoadingGlassPane(String message) {
        JPanel glassPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        glassPane.setOpaque(false);
        glassPane.setLayout(new GridBagLayout());
        
        JPanel loadingPanel = new JPanel();
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
        loadingPanel.setOpaque(false);
        
        JLabel spinner = new JLabel(createTextSpinner());
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font(messageLabel.getFont().getName(), Font.BOLD, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        loadingPanel.add(spinner);
        loadingPanel.add(Box.createVerticalStrut(10));
        loadingPanel.add(messageLabel);
        
        glassPane.add(loadingPanel);
        
        return glassPane;
    }
    
    /**
     * Create a text-based spinner animation for loading indicator.
     * 
     * @return ImageIcon with spinner animation
     */
    private static ImageIcon createTextSpinner() {
        // This would normally create or return a spinner GIF
        // Since we can't include external resources, using a placeholder
        JLabel spinner = new JLabel("Loading...");
        spinner.setFont(new Font(spinner.getFont().getName(), Font.BOLD, 16));
        spinner.setForeground(Color.BLUE);
        return new ImageIcon(); // Placeholder - would be a real spinner in production
    }
    
    /**
     * Executes a task in a background thread while displaying a loading indicator.
     * 
     * @param component Parent component
     * @param task Runnable task to execute
     * @param loadingMessage Message to display during loading
     */
    public static void runWithLoadingIndicator(JComponent component, Runnable task, String loadingMessage) {
        JRootPane rootPane = SwingUtilities.getRootPane(component);
        if (rootPane == null) {
            logger.warn("Cannot show loading indicator - component not attached to root pane");
            executor.submit(task);
            return;
        }
        
        JPanel glassPane = createLoadingGlassPane(loadingMessage);
        Component oldGlassPane = rootPane.getGlassPane();
        rootPane.setGlassPane(glassPane);
        glassPane.setVisible(true);
        
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Background task failed", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(component, 
                            "Operation failed: " + e.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    rootPane.setGlassPane(oldGlassPane);
                    oldGlassPane.setVisible(false);
                });
            }
        });
    }
    
    /**
     * Gets a file icon for display in the file table.
     * 
     * @param file File to get icon for
     * @return ImageIcon representing the file type
     */
    public static Icon getFileIcon(File file) {
        return FileSystemView.getFileSystemView().getSystemIcon(file);
    }
    
    /**
     * Shows an error dialog with the given message.
     * 
     * @param component Parent component
     * @param title Dialog title
     * @param message Error message
     */
    public static void showError(Component component, String title, String message) {
        JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows a confirmation dialog with the given message.
     * 
     * @param component Parent component
     * @param title Dialog title
     * @param message Confirmation message
     * @return true if confirmed, false otherwise
     */
    public static boolean showConfirmation(Component component, String title, String message) {
        return JOptionPane.showConfirmDialog(component, message, title, 
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private UIUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}