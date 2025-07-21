package jll.chongwm.doxis.utility.ui;

import jll.chongwm.doxis.utility.model.FileRecord;
import jll.chongwm.doxis.utility.model.FileSystemModel;
import jll.chongwm.doxis.utility.service.DynamoDBService;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel displaying a table of files in the current directory. Shows only files (not directories) with checkboxes for selection.
 */
public class FileTablePanel extends JPanel
{
	private static final Logger logger = LoggerFactory.getLogger(FileTablePanel.class);

	private final FileSystemModel fileSystemModel;
	private final DynamoDBService dynamoDBService;
	private final FileTableModel tableModel;
	private final JTable fileTable;
	private final FileSystemView fileSystemView;
	private JButton selectAllButton;
	private JButton deselectAllButton;

	/**
	 * Creates a new FileTablePanel.
	 */
	public FileTablePanel()
	{
		this.fileSystemModel = FileSystemModel.getInstance();
		this.dynamoDBService = DynamoDBService.getInstance();
		this.fileSystemView = FileSystemView.getFileSystemView();

		setLayout(new BorderLayout());

		// Create table model
		tableModel = new FileTableModel();

		// Create table with custom cell renderers
		fileTable = new JTable(tableModel);
		fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fileTable.setAutoCreateRowSorter(true);
		fileTable.setRowHeight(24); // For better checkbox/icon visibility

		// Set up checkbox column
		TableColumn checkboxColumn = fileTable.getColumnModel().getColumn(0);
		checkboxColumn.setMaxWidth(50);
		checkboxColumn.setCellRenderer(new CheckBoxRenderer());
		checkboxColumn.setCellEditor(new CheckBoxEditor());

		// Set up file name column with icons
		TableColumn nameColumn = fileTable.getColumnModel().getColumn(1);
		nameColumn.setCellRenderer(new FileNameRenderer());

		// Set up path column
		TableColumn pathColumn = fileTable.getColumnModel().getColumn(2);
		pathColumn.setPreferredWidth(400);

		// Add scrollbars
		JScrollPane scrollPane = new JScrollPane(fileTable);
		add(scrollPane, BorderLayout.CENTER);

		// Create button panel for select/deselect controls
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		selectAllButton = new JButton("Select All");
		deselectAllButton = new JButton("Deselect All");
		
		// Add action listeners
		selectAllButton.addActionListener(e -> selectAllFiles());
		deselectAllButton.addActionListener(e -> deselectAllFiles());

		// Add buttons to panel
		buttonPanel.add(selectAllButton);
		buttonPanel.add(deselectAllButton);
		// Add to the top of the panel (above the file table)
		add(buttonPanel, BorderLayout.NORTH);

		// Update with current directory files
		updateFileTable(fileSystemModel.getFiles());

		// Add observers
		fileSystemModel.addFileListChangeObserver(this::updateFileTable);
		fileSystemModel.addFileRecordChangeObserver(this::updateFileRecord);

		logger.debug("FileTablePanel initialized");
	}

	/**
	 * Updates the file table with the given list of files.
	 * 
	 * @param files List of files to display
	 */
	private void updateFileTable(List<File> files)
	{
		SwingUtilities.invokeLater(() ->
		{
			tableModel.setFiles(files);
			logger.debug("Updated file table with {} files", files.size());
		});
	}

	/**
	 * Updates a file record in the table.
	 * 
	 * @param record The file record to update
	 */
	private void updateFileRecord(FileRecord record)
	{
		SwingUtilities.invokeLater(() -> { tableModel.updateFileRecord(record); });
	}

	/**
	 * Table model for displaying files with checkboxes.
	 */
	private class FileTableModel extends AbstractTableModel
	{
		private final String[] columnNames =
		{ "", "File Name", "Path" };
		private final Vector<File> files = new Vector<>();

		/**
		 * Sets the files to display in the table.
		 * 
		 * @param newFiles List of files to display
		 */
		public void setFiles(List<File> newFiles)
		{
			files.clear();
			files.addAll(newFiles);
			fireTableDataChanged();
		}

		/**
		 * Updates a file record in the table.
		 * 
		 * @param record The file record to update
		 */
		public void updateFileRecord(FileRecord record)
		{
			for (int i = 0; i < files.size(); i++)
			{
				File file = files.get(i);
				try
				{
					if (file.getCanonicalPath().equals(record.getFilePath()))
					{
						// Found the file, update checkbox state
						fireTableCellUpdated(i, 0);
						break;
					}
				} catch (IOException e)
				{
					logger.error("Error getting canonical path", e);
				}
			}
		}

		@Override
		public int getRowCount()
		{
			return files.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			switch (column)
			{
			case 0:
				return Boolean.class;
			default:
				return String.class;
			}
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column == 0; // Only checkbox column is editable
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			if (row >= files.size())
			{
				return null;
			}

			File file = files.get(row);

			switch (column)
			{
			case 0: // Checkbox
				return fileSystemModel.isFileSelected(file);
			case 1: // File name
				return file;
			case 2: // File path
				try
				{
					return file.getCanonicalPath();
				} catch (IOException e)
				{
					logger.error("Error getting canonical path", e);
					return file.getAbsolutePath();
				}
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (column == 0 && value instanceof Boolean)
			{
				File file = files.get(row);
				boolean selected = (Boolean) value;

				// Update file selection state in model
				fileSystemModel.updateFileSelection(file, selected);

				// Add record to pending list for sync
				try
				{
					String canonicalPath = file.getCanonicalPath();
					long timestamp = file.lastModified();
					String username = System.getProperty("user.name");

					FileRecord record = new FileRecord(canonicalPath, timestamp, username, selected);
					dynamoDBService.addRecord(record);

					logger.debug("File selection changed: {} ({})", file.getName(), selected);
				} catch (IOException e)
				{
					logger.error("Error updating file selection", e);
				}

				fireTableCellUpdated(row, column);
			}
		}

		/**
		 * Gets the file at the given row.
		 * 
		 * @param row Table row
		 * @return File at the row
		 */
		public File getFileAt(int row)
		{
			if (row >= 0 && row < files.size())
			{
				return files.get(row);
			}
			return null;
		}
	}

	/**
	 * Renderer for checkbox column.
	 */
	private class CheckBoxRenderer extends DefaultTableCellRenderer
	{
		private final JCheckBox checkbox = new JCheckBox();

		public CheckBoxRenderer()
		{
			checkbox.setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{

			checkbox.setSelected(value != null && (Boolean) value);
			checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			checkbox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

			return checkbox;
		}
	}

	/**
	 * Editor for checkbox column.
	 */
	private class CheckBoxEditor extends DefaultCellEditor
	{
		public CheckBoxEditor()
		{
			super(new JCheckBox());
			JCheckBox checkbox = (JCheckBox) getComponent();
			checkbox.setHorizontalAlignment(SwingConstants.CENTER);
		}
	}

	/**
	 * Renderer for file name column with icons.
	 */
	private class FileNameRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof File)
			{
				File file = (File) value;
				setText(file.getName());
				setIcon(fileSystemView.getSystemIcon(file));
				setToolTipText(file.getAbsolutePath());
			}

			return this;
		}
	}
	
	/**
	 * Selects all files in the table.
	 */
	private void selectAllFiles() {
	    for (int i = 0; i < tableModel.getRowCount(); i++) {
	        File file = tableModel.getFileAt(i);
	        fileSystemModel.updateFileSelection(file, true);
	        tableModel.setValueAt(true, i, 0);
	    }
	    logger.debug("Selected all files");
	}

	/**
	 * Deselects all files in the table.
	 */
	private void deselectAllFiles() {
	    for (int i = 0; i < tableModel.getRowCount(); i++) {
	        File file = tableModel.getFileAt(i);
	        fileSystemModel.updateFileSelection(file, false);
	        tableModel.setValueAt(false, i, 0);
	    }
	    logger.debug("Deselected all files");
	}	
	
	
	
	
}