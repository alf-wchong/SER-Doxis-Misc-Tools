package jll.chongwm.doxis.utility.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jll.chongwm.doxis.utility.model.FileRecord;
import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.service.DynamoDBService;

/**
 * Main panel containing all components of the file picker. Organizes the directory tree, file table, path bar, and status bar.
 */
public class MainPanel extends JPanel
{
	private static final Logger logger = LoggerFactory.getLogger(MainPanel.class);

	private final FileSystemModel fileSystemModel;
	private final DynamoDBService dynamoDBService;

	private final DirectoryTreePanel directoryTreePanel;
	private final FileTablePanel fileTablePanel;
	private final PathBarPanel pathBarPanel;
	private final StatusBarPanel statusBarPanel;
	
	// Reference to track active background workers
	private SwingWorker<?, ?> activeWorker;

	/**
	 * Creates a new MainPanel.
	 */
	public MainPanel()
	{
		// Initialize models and services
		fileSystemModel = FileSystemModel.getInstance();
		dynamoDBService = DynamoDBService.getInstance();
		dynamoDBService.initialize();

		// Set layout
		setLayout(new BorderLayout());

		// Create path bar (top)
		pathBarPanel = new PathBarPanel();
		add(pathBarPanel, BorderLayout.NORTH);

		// Create split pane for directory tree and file table
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.3); // 30% left, 70% right

		// Create directory tree (left)
		directoryTreePanel = new DirectoryTreePanel();
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBorder(BorderFactory.createTitledBorder("Directories"));
		leftPanel.add(directoryTreePanel, BorderLayout.CENTER);
		splitPane.setLeftComponent(leftPanel);

		// Create file table (right)
		fileTablePanel = new FileTablePanel();
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBorder(BorderFactory.createTitledBorder("Files"));
		rightPanel.add(fileTablePanel, BorderLayout.CENTER);
		splitPane.setRightComponent(rightPanel);

		add(splitPane, BorderLayout.CENTER);

		// Create status bar (bottom)
		statusBarPanel = new StatusBarPanel();
		add(statusBarPanel, BorderLayout.SOUTH);

		// Load file records from DynamoDB
		loadFileRecords();

		logger.info("MainPanel initialized");
	}

	/**
	 * Gets the list of selected file paths.
	 * 
	 * @return List of selected file paths
	 */
	public List<String> getSelectedFilePaths()
	{
		return statusBarPanel.getSelectedFilePaths();
	}

	/**
	 * Gets the number of selected files.
	 * 
	 * @return Number of selected files
	 */
	public int getSelectedCount()
	{
		return statusBarPanel.getSelectedCount();
	}

	/**
	 * Loads file records from DynamoDB.
	 */
	private void loadFileRecords()
	{
		SwingWorker<List<FileRecord>, Void> worker = new SwingWorker<List<FileRecord>, Void>()
		{
			@Override
			protected List<FileRecord> doInBackground()
			{
				return dynamoDBService.loadAllRecords();
			}

			@Override
			protected void done()
			{
				try
				{
					List<FileRecord> records = get();
					if (records != null)
					{
						fileSystemModel.updateFileRecords(records);
						logger.info("Loaded {} file records from DynamoDB", records.size());
					}
				} catch (Exception e)
				{
					logger.error("Error loading file records", e);
				}
				activeWorker = null;
			}
		};
		activeWorker = worker;
		worker.execute();
	}
	
	/**
	 * Cleanly shuts down all services and background processes.
	 */
	public void shutdown()
	{
		logger.info("Starting application shutdown sequence");
		
		// Cancel any active background workers
		if (activeWorker != null && !activeWorker.isDone()) {
			activeWorker.cancel(true);
			logger.debug("Canceled active background worker");
		}
		
		// Shutdown status bar panel (which has the ScheduledExecutorService)
		if (statusBarPanel != null) {
			statusBarPanel.shutdown();
			logger.debug("Status bar panel shutdown complete");
		}
		
		// Shutdown directory tree panel resources if needed
		if (directoryTreePanel != null) {
			// If DirectoryTreePanel has a shutdown method, call it here
			// directoryTreePanel.shutdown();
			logger.debug("Directory tree panel shutdown complete");
		}
		
		// Shutdown file table panel resources if needed
		if (fileTablePanel != null) {
			// If FileTablePanel has a shutdown method, call it here
			// fileTablePanel.shutdown();
			logger.debug("File table panel shutdown complete");
		}
		
		// Shutdown path bar panel resources if needed
		if (pathBarPanel != null) {
			// If PathBarPanel has a shutdown method, call it here
			// pathBarPanel.shutdown();
			logger.debug("Path bar panel shutdown complete");
		}
		
		// Finally, shutdown DynamoDB service
		if (dynamoDBService != null) {
			dynamoDBService.shutdown();
			logger.debug("DynamoDB service shutdown complete");
		}
		
		logger.info("Application shutdown sequence complete");
	}

	/**
	 * Gets the status bar panel.
	 * 
	 * @return The status bar panel
	 */
	public StatusBarPanel getStatusBarPanel() 
	{
	    return statusBarPanel;
	}	

	/**
	 * Creates a frame containing the MainPanel.
	 * 
	 * @return The created frame
	 */
	public static JFrame createFrame()
	{
		JFrame frame = new JFrame("JLL File Path Picker");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		MainPanel mainPanel = new MainPanel();
		frame.getContentPane().add(mainPanel);

		// Set size and position
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = Math.min(1200, (int) (screenSize.width * 0.8));
		int height = Math.min(800, (int) (screenSize.height * 0.8));
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null); // Center on screen

		// Add window listener for cleanup
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				// Use the centralized shutdown method
				mainPanel.shutdown();
				logger.info("Application shutting down");
			}
		});

		return frame;
	}

	/**
	 * Main method for testing.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		// Set system property for test mode if needed
		// System.setProperty("local.test.mode", "true");

		// Set look and feel
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e)
		{
			logger.warn("Could not set system look and feel", e);
		}

		// Create and show UI
		SwingUtilities.invokeLater(() ->
		{
			JFrame frame = createFrame();
			frame.setVisible(true);
		});
	}
}