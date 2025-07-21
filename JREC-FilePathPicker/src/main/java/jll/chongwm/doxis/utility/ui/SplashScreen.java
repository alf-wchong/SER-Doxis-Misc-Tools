package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.Main;
import jll.chongwm.doxis.utility.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Splash screen shown at application startup.
 * Displays the application name, Windows username, and current date/time.
 */
public class SplashScreen extends JWindow {
    
    private static final int WIDTH = 500;
    private static final int HEIGHT = 300;
    private static final String USERNAME = System.getProperty("user.name");
    
    public SplashScreen() {
        initComponents();
        positionOnScreen();
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                // Use Graphics2D for better rendering
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Draw gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(59, 89, 152), // JLL corporate blue
                    0, HEIGHT, new Color(41, 62, 107)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
                
                // Draw title
                Font titleFont = new Font("Arial", Font.BOLD, 32);
                g2d.setFont(titleFont);
                g2d.setColor(Color.WHITE);
                
                String title = Main.APP_NAME;
                FontMetrics fm = g2d.getFontMetrics();
                int titleWidth = fm.stringWidth(title);
                g2d.drawString(title, (WIDTH - titleWidth) / 2, HEIGHT / 3);
                
                // Draw version
                Font versionFont = new Font("Arial", Font.PLAIN, 16);
                g2d.setFont(versionFont);
                g2d.setColor(new Color(220, 220, 220));
                
                String version = "v" + Main.APP_VERSION;
                fm = g2d.getFontMetrics();
                int versionWidth = fm.stringWidth(version);
                g2d.drawString(version, (WIDTH - versionWidth) / 2, HEIGHT / 3 + 30);
                
                // Draw username
                Font userFont = new Font("Arial", Font.PLAIN, 14);
                g2d.setFont(userFont);
                
                String userText = "User: " + USERNAME;
                fm = g2d.getFontMetrics();
                int userWidth = fm.stringWidth(userText);
                g2d.drawString(userText, (WIDTH - userWidth) / 2, HEIGHT / 2 + 20);
                
                // Draw date and time
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String dateTimeText = "Date: " + now.format(formatter);
                
                fm = g2d.getFontMetrics();
                int dateWidth = fm.stringWidth(dateTimeText);
                g2d.drawString(dateTimeText, (WIDTH - dateWidth) / 2, HEIGHT / 2 + 50);
                
                // Draw loading text
                Font loadingFont = new Font("Arial", Font.ITALIC, 14);
                g2d.setFont(loadingFont);
                g2d.setColor(new Color(200, 200, 200));
                
                String loadingText = "Loading, please wait...";
                fm = g2d.getFontMetrics();
                int loadingWidth = fm.stringWidth(loadingText);
                g2d.drawString(loadingText, (WIDTH - loadingWidth) / 2, HEIGHT - 40);
                
                g2d.dispose();
            }
        };
        
        mainPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        
        // Add border
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(41, 62, 107), 2));
        
        getContentPane().add(mainPanel);
        pack();
    }
    
    private void positionOnScreen() {
        // Center on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - getWidth()) / 2;
        int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
    }
}