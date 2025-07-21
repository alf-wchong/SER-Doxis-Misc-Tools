package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.model.FileRecord;
import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.service.DynamoDBService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel displaying status information and synchronization controls. Shows item count, selection count, and sync status.
 */
public class StatusBarPanel extends JPanel
{
	private static final Logger logger = LoggerFactory.getLogger(StatusBarPanel.class);

	private final FileSystemModel fileSystemModel;
	private final DynamoDBService dynamoDBService;

	private final JLabel itemCountLabel;
	private final JLabel selectedCountLabel;
	private final JLabel lastSyncLabel;
	private final JButton syncButton;

	private final AtomicInteger selectedCount = new AtomicInteger(0);
	private long lastSyncTime = 0;

	private ScheduledExecutorService scheduler;

	/**
	 * Creates a new StatusBarPanel.
	 */
	public StatusBarPanel()
	{
		this.fileSystemModel = FileSystemModel.getInstance();
		this.dynamoDBService = DynamoDBService.getInstance();

		setLayout(new BorderLayout(5, 0));
		setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), new EmptyBorder(5, 5, 5, 5)));

		// Left side - item counts
		JPanel countsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

		itemCountLabel = new JLabel("Items: 0");
		countsPanel.add(itemCountLabel);

		selectedCountLabel = new JLabel("Selected: 0");
		countsPanel.add(selectedCountLabel);

		add(countsPanel, BorderLayout.WEST);

		// Right side - sync status and button
		JPanel syncPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

		lastSyncLabel = new JLabel("Last sync: Never");
		syncPanel.add(lastSyncLabel);

		syncButton = new JButton("Sync Now");
		syncButton.addActionListener(e ->
		{
			syncButton.setEnabled(false);
			syncButton.setText("Syncing...");

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
			{
				@Override
				protected Void doInBackground()
				{
					dynamoDBService.forceSynchronize();
					return null;
				}

				@Override
				protected void done()
				{
					updateLastSyncTime();
					syncButton.setEnabled(true);
					syncButton.setText("Sync Now");
				}
			};
			worker.execute();
		});
		syncPanel.add(syncButton);

		add(syncPanel, BorderLayout.EAST);

		// Add observers
		fileSystemModel.addFileListChangeObserver(this::updateItemCount);
		fileSystemModel.addFileRecordChangeObserver(this::updateSelectedCount);

		// Schedule periodic updates for sync time
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(this::updateLastSyncTimeDisplay, 1, 60, TimeUnit.SECONDS);

		logger.debug("StatusBarPanel initialized");
	}

	public void shutdown()
	{
		if (scheduler != null)
		{
			scheduler.shutdown();
			try
			{
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
				{
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
		}
		logger.debug("StatusBarPanel shutdown complete");
	}

	/**
	 * Updates the item count display.
	 * 
	 * @param files List of files
	 */
	private void updateItemCount(List<File> files)
	{
		SwingUtilities.invokeLater(() ->
		{
			itemCountLabel.setText("Items: " + files.size());
			updateSelectedCount();
		});
	}

	/**
	 * Updates the selected count when a file record changes.
	 * 
	 * @param record The file record that changed
	 */
	private void updateSelectedCount(FileRecord record)
	{
		updateSelectedCount();
	}

	/**
	 * Updates the selected count display.
	 */
	private void updateSelectedCount()
	{
		SwingUtilities.invokeLater(() ->
		{
			List<FileRecord> records = fileSystemModel.getAllFileRecords();
			int count = (int) records.stream().filter(FileRecord::isSelected).count();

			selectedCount.set(count);
			selectedCountLabel.setText("Selected: " + count);
		});
	}

	/**
	 * Updates the last sync time.
	 */
	private void updateLastSyncTime()
	{
		lastSyncTime = System.currentTimeMillis();
		updateLastSyncTimeDisplay();
	}

	/**
	 * Updates the last sync time display.
	 */
	private void updateLastSyncTimeDisplay()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (lastSyncTime == 0)
			{
				lastSyncLabel.setText("Last sync: Never");
			} else
			{
				String timeStr = formatTimeAgo(lastSyncTime);
				lastSyncLabel.setText("Last sync: " + timeStr);
			}
		});
	}

	/**
	 * Formats a time as a human-readable "time ago" string.
	 * 
	 * @param time The time in milliseconds
	 * @return Human-readable time ago string
	 */
	private String formatTimeAgo(long time)
	{
		long now = System.currentTimeMillis();
		long diff = now - time;

		if (diff < 60_000)
		{ // Less than a minute
			return "Just now";
		} else if (diff < 3600_000)
		{ // Less than an hour
			int minutes = (int) (diff / 60_000);
			return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
		} else if (diff < 86400_000)
		{ // Less than a day
			int hours = (int) (diff / 3600_000);
			return hours + (hours == 1 ? " hour ago" : " hours ago");
		} else
		{ // More than a day
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			return format.format(new Date(time));
		}
	}

	/**
	 * Gets the number of selected files.
	 * 
	 * @return Number of selected files
	 */
	public int getSelectedCount()
	{
		return selectedCount.get();
	}

	/**
	 * Gets a list of selected file paths.
	 * 
	 * @return List of selected file paths
	 */
	public List<String> getSelectedFilePaths()
	{
		return fileSystemModel.getAllFileRecords().stream().filter(FileRecord::isSelected).map(FileRecord::getFilePath).collect(Collectors.toList());
	}
}