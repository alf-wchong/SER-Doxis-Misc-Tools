package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.service.DynamoDBService;
import jll.chongwm.doxis.utility.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main window for the JREC-FilePathPicker application. Contains the main panel and provides access to selected files.
 */
public class MainWindow extends JFrame
{
	private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

	private final MainPanel mainPanel;
	private boolean isCancelled = false;
	private JLabel userInfoLabel;

	/**
	 * Creates a new MainWindow with default title.
	 */
	public MainWindow()
	{
		this(Main.APP_NAME + " v" + Main.APP_VERSION);
	}

	/**
	 * Creates a new MainWindow with the specified title.
	 * 
	 * @param title The window title
	 */
	public MainWindow(String title)
	{
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Create main panel
		mainPanel = new MainPanel();
		getContentPane().add(mainPanel, BorderLayout.CENTER);

		// Create button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		
		JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		userInfoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		// Get current Windows username
		String username = System.getProperty("user.name");
		userInfoLabel = new JLabel("Current username used for tagging: " + username);
		userInfoLabel.setFont(userInfoLabel.getFont().deriveFont(Font.BOLD));
		userInfoPanel.add(userInfoLabel);
		getContentPane().add(userInfoPanel, BorderLayout.NORTH);

		okButton.addActionListener(e ->
		{
			isCancelled = false;
			dispose();
		});

		cancelButton.addActionListener(e ->
		{
			isCancelled = true;
			dispose();
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		// Set size and position
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = Math.min(1200, (int) (screenSize.width * 0.8));
		int height = Math.min(800, (int) (screenSize.height * 0.8));
		setSize(width, height);
		setLocationRelativeTo(null); // Center on screen

		// Add window listener for cleanup
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				// Set as cancelled if closed via window controls
				isCancelled = true;

				// Clean shutdown of services
				DynamoDBService.getInstance().shutdown();
				logger.info("Application shutting down");
			}
		});

		logger.info("MainWindow initialized");
	}

	/**
	 * Gets the list of selected file paths.
	 * 
	 * @return List of selected file paths
	 */
	public List<String> getSelectedFilePaths()
	{
		return mainPanel.getSelectedFilePaths();
	}

	/**
	 * Gets the number of selected files.
	 * 
	 * @return Number of selected files
	 */
	public int getSelectedCount()
	{
		return mainPanel.getSelectedCount();
	}

	/**
	 * Checks if the selection was cancelled.
	 * 
	 * @return true if cancelled, false otherwise
	 */
	public boolean isCancelled()
	{
		return isCancelled;
	}

	/**
	 * Sets the window title with the application name and version.
	 * 
	 * @param subtitle Additional subtitle to append
	 */
	public void setApplicationTitle(String subtitle)
	{
		if (subtitle != null && !subtitle.isEmpty())
		{
			setTitle(Main.APP_NAME + " v" + Main.APP_VERSION + " - " + subtitle);
		} else
		{
			setTitle(Main.APP_NAME + " v" + Main.APP_VERSION);
		}
	}
}