package jll.chongwm.doxis.utility.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a file record with path, timestamp, user, and selection state.
 */
public class FileRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String filePath;
    private final long timestamp;
    private final String username;
    private final boolean selected;
    
    /**
     * Creates a new FileRecord.
     * 
     * @param filePath The canonical path of the file
     * @param timestamp The last modified timestamp
     * @param username The username who last modified the record
     * @param selected The selection state
     */
    public FileRecord(String filePath, long timestamp, String username, boolean selected) {
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.username = username;
        this.selected = selected;
    }
    
    /**
     * Gets the file path.
     * 
     * @return The canonical file path
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Gets the timestamp.
     * 
     * @return The last modified timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the username.
     * 
     * @return The username who last modified the record
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Checks if the file is selected.
     * 
     * @return true if the file is selected, false otherwise
     */
    public boolean isSelected() {
        return selected;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileRecord that = (FileRecord) o;
        return timestamp == that.timestamp &&
               selected == that.selected &&
               Objects.equals(filePath, that.filePath) &&
               Objects.equals(username, that.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath, timestamp, username, selected);
    }
    
    @Override
    public String toString() {
        return "FileRecord{" +
               "filePath='" + filePath + '\'' +
               ", timestamp=" + timestamp +
               ", username='" + username + '\'' +
               ", selected=" + selected +
               '}';
    }
    
    /**
     * Creates a new FileRecord with the same path and user but a different selection state.
     * 
     * @param selected The new selection state
     * @return A new FileRecord with updated selection
     */
    public FileRecord withSelected(boolean selected) {
        return new FileRecord(this.filePath, System.currentTimeMillis(), this.username, selected);
    }
}