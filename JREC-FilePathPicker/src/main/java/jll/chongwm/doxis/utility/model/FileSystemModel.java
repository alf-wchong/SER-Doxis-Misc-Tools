package jll.chongwm.doxis.utility.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for handling file system operations and maintaining file state.
 */
public class FileSystemModel
{
	private static final Logger logger = LoggerFactory.getLogger(FileSystemModel.class);

	// Singleton instance
	private static FileSystemModel instance;

	// Current directory
	private File currentDirectory;

	// Map to track file selection states (canonical path -> FileRecord)
	private final ConcurrentMap<String, FileRecord> fileRecordMap = new ConcurrentHashMap<>();

	// Observers for model changes
	private final List<Consumer<File>> directoryChangeObservers = Collections.synchronizedList(new ArrayList<>());
	private final List<Consumer<List<File>>> fileListChangeObservers = Collections.synchronizedList(new ArrayList<>());
	private final List<Consumer<FileRecord>> fileRecordChangeObservers = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Private constructor for singleton
	 */
	private FileSystemModel()
	{
	    // Check for custom start directory from system property
	    String customStartDir = System.getProperty("jrec.startDirectory");
	    if (customStartDir != null && !customStartDir.isEmpty()) 
	    {
	        File startDir = new File(customStartDir);
	        if (startDir.exists() && startDir.isDirectory()) 
	        {
	            this.currentDirectory = startDir;
	            logger.info("FileSystemModel initialized with custom directory: {}", currentDirectory.getAbsolutePath());
	            return;
	        } else 
	        {
	            logger.warn("Custom start directory is invalid: {}", customStartDir);
	        }
	    }
	    
	    // Fall back to user's home directory by default
	    this.currentDirectory = new File(System.getProperty("user.home"));
	    logger.info("FileSystemModel initialized with home directory: {}", currentDirectory.getAbsolutePath());
	}		
		

	/**
	 * Gets the singleton instance of the model.
	 * 
	 * @return The FileSystemModel instance
	 */
	public static synchronized FileSystemModel getInstance()
	{
		if (instance == null)
		{
			instance = new FileSystemModel();
		}
		return instance;
	}

	/**
	 * Gets the current directory.
	 * 
	 * @return The current directory
	 */
	public File getCurrentDirectory()
	{
		return currentDirectory;
	}

	/**
	 * Sets the current directory and notifies observers.
	 * 
	 * @param directory The new current directory
	 * @return true if directory was changed successfully
	 */
	public boolean setCurrentDirectory(File directory)
	{
		if (directory == null || !directory.exists() || !directory.isDirectory())
		{
			logger.warn("Invalid directory: {}", directory);
			return false;
		}

		try
		{
			File canonicalDir = directory.getCanonicalFile();
			this.currentDirectory = canonicalDir;
			notifyDirectoryChanged();

			List<File> files = getFiles(); // This will also notify observers

			return true;
		} catch (IOException e)
		{
			logger.error("Error getting canonical path for directory: {}", directory, e);
			return false;
		}
	}

	/**
	 * Gets the parent directory of the current directory.
	 * 
	 * @return The parent directory or null if at root
	 */
	public File getParentDirectory()
	{
		return currentDirectory.getParentFile();
	}

	/**
	 * Navigates to the parent directory.
	 * 
	 * @return true if navigation was successful
	 */
	public boolean navigateToParent()
	{
		File parent = getParentDirectory();
		if (parent != null)
		{
			return setCurrentDirectory(parent);
		}
		return false;
	}

	/**
	 * Gets all subdirectories in the current directory.
	 * 
	 * @return List of subdirectories
	 */
	public List<File> getSubdirectories()
	{
		try
		{
			File[] files = currentDirectory.listFiles(File::isDirectory);
			if (files == null)
			{
				logger.warn("Could not list subdirectories in {}", currentDirectory);
				return Collections.emptyList();
			}
			return Arrays.asList(files);
		} catch (SecurityException e)
		{
			logger.error("Security exception listing subdirectories", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Gets all files (non-directories) in the current directory.
	 * 
	 * @return List of files
	 */
	public List<File> getFiles()
	{
		try
		{
			File[] files = currentDirectory.listFiles(file -> !file.isDirectory());
			if (files == null)
			{
				logger.warn("Could not list files in {}", currentDirectory);
				return Collections.emptyList();
			}
			List<File> fileList = Arrays.asList(files);
			notifyFileListChanged(fileList);
			return fileList;
		} catch (SecurityException e)
		{
			logger.error("Security exception listing files", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Updates the selection state for a file.
	 * 
	 * @param file     The file to update
	 * @param selected Whether the file is selected
	 * @return true if the state was successfully updated
	 */
	public boolean updateFileSelection(File file, boolean selected)
	{
		try
		{
			String canonicalPath = file.getCanonicalPath();
			long lastModified = file.lastModified();
			String username = System.getProperty("user.name");

			BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			long timestamp = attrs.lastModifiedTime().toMillis();

			FileRecord record = new FileRecord(canonicalPath, timestamp, username, selected);
			fileRecordMap.put(canonicalPath, record);
			notifyFileRecordChanged(record);

			return true;
		} catch (IOException e)
		{
			logger.error("Error updating file selection for {}", file, e);
			return false;
		}
	}

	/**
	 * Gets the selection state for a file.
	 * 
	 * @param file The file to check
	 * @return true if the file is selected, false otherwise
	 */
	public boolean isFileSelected(File file)
	{
		try
		{
			String canonicalPath = file.getCanonicalPath();
			FileRecord record = fileRecordMap.get(canonicalPath);
			return record != null && record.isSelected();
		} catch (IOException e)
		{
			logger.error("Error checking file selection for {}", file, e);
			return false;
		}
	}

	/**
	 * Gets all file records.
	 * 
	 * @return List of all file records
	 */
	public List<FileRecord> getAllFileRecords()
	{
		return new ArrayList<>(fileRecordMap.values());
	}

	/**
	 * Updates file records from external source (e.g., DynamoDB). Only updates records if the timestamp is newer.
	 * 
	 * @param records Records to update
	 */
	public void updateFileRecords(List<FileRecord> records)
	{
		for (FileRecord newRecord : records)
		{
			fileRecordMap.compute(newRecord.getFilePath(), (key, existingRecord) ->
			{
				if (existingRecord == null || newRecord.getTimestamp() > existingRecord.getTimestamp())
				{
					return newRecord;
				}
				return existingRecord;
			});
		}

		// Notify observers for visible files
		try
		{
			List<File> currentFiles = Arrays.asList(currentDirectory.listFiles(file -> !file.isDirectory()));
			for (File file : currentFiles)
			{
				String canonicalPath = file.getCanonicalPath();
				FileRecord record = fileRecordMap.get(canonicalPath);
				if (record != null)
				{
					notifyFileRecordChanged(record);
				}
			}
		} catch (Exception e)
		{
			logger.error("Error notifying observers after record update", e);
		}
	}

	/**
	 * Clears all file records.
	 */
	public void clearFileRecords()
	{
		fileRecordMap.clear();
	}

	/**
	 * Adds a directory change observer.
	 * 
	 * @param observer The observer to add
	 */
	public void addDirectoryChangeObserver(Consumer<File> observer)
	{
		directoryChangeObservers.add(observer);
	}

	/**
	 * Adds a file list change observer.
	 * 
	 * @param observer The observer to add
	 */
	public void addFileListChangeObserver(Consumer<List<File>> observer)
	{
		fileListChangeObservers.add(observer);
	}

	/**
	 * Adds a file record change observer.
	 * 
	 * @param observer The observer to add
	 */
	public void addFileRecordChangeObserver(Consumer<FileRecord> observer)
	{
		fileRecordChangeObservers.add(observer);
	}

	/**
	 * Notifies all directory change observers.
	 */
	private void notifyDirectoryChanged()
	{
		for (Consumer<File> observer : directoryChangeObservers)
		{
			observer.accept(currentDirectory);
		}
	}

	/**
	 * Notifies all file list change observers.
	 * 
	 * @param files List of files
	 */
	private void notifyFileListChanged(List<File> files)
	{
		for (Consumer<List<File>> observer : fileListChangeObservers)
		{
			observer.accept(files);
		}
	}

	/**
	 * Notifies all file record change observers.
	 * 
	 * @param record The file record that changed
	 */
	private void notifyFileRecordChanged(FileRecord record)
	{
		for (Consumer<FileRecord> observer : fileRecordChangeObservers)
		{
			observer.accept(record);
		}
	}
}