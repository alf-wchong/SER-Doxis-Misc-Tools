package jll.chongwm.doxis.utility;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.concurrent.CountDownLatch;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jll.chongwm.doxis.utility.service.DynamoDBService;
import jll.chongwm.doxis.utility.ui.MainWindow;
import jll.chongwm.doxis.utility.ui.SplashScreen;

/**
 * Main entry point for the JREC-FilePathPicker application. This class initializes the application and shows the splash
 * screen before loading the main window.
 */
public class Main
{
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	// Application constants
	public static final String APP_NAME = "JREC-FilePathPicker";
	public static final String APP_VERSION = "1.0.0";

	public static void main(String[] args)
	{
		logger.info("Starting {} v{}", APP_NAME, APP_VERSION);

		// Set system look and feel
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e)
		{
			logger.warn("Failed to set system look and feel", e);
		}

		// Display splash screen
		final SplashScreen splash = new SplashScreen();
		SwingUtilities.invokeLater(() -> { splash.setVisible(true);	});

		// Initialize services in background
		final CountDownLatch initLatch = new CountDownLatch(1);
		Thread initThread = new Thread(() ->
		{
			try
			{
				logger.debug("Initializing DynamoDB service...");
				DynamoDBService.getInstance().initialize();
				logger.debug("DynamoDB service initialized successfully");

				// Simulate loading time to show splash screen (remove in production)
				Thread.sleep(1500);

			} catch (Exception e)
			{
				logger.error("Error during application initialization", e);
				SwingUtilities.invokeLater(() -> {JOptionPane.showMessageDialog(null, "Error initializing application: " + e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);});
			} 
			finally
			{
				initLatch.countDown();
			}
		});
		initThread.start();

		// Wait for initialization to complete
		try
		{
			initLatch.await();
		} catch (InterruptedException e)
		{
			logger.error("Initialization interrupted", e);
			Thread.currentThread().interrupt();
		}

		// Close splash screen and show main window
		SwingUtilities.invokeLater(() ->
		{
			splash.dispose();

			MainWindow mainWindow = new MainWindow();
			mainWindow.setVisible(true);

			// Position in the center of the screen
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (screenSize.width - mainWindow.getWidth()) / 2;
			int y = (screenSize.height - mainWindow.getHeight()) / 2;
			mainWindow.setLocation(x, y);

			logger.info("Application GUI initialized and displayed");
		});
	}
}