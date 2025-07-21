package jll.chongwm.doxis.utility.service;

import jll.chongwm.doxis.utility.model.FileRecord;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for interacting with Amazon DynamoDB for file record synchronization.
 */
public class DynamoDBService {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBService.class);
    
    private static final String TABLE_NAME = "JRECFilePathPickerRecords";
    private static final String KEY_FILEPATH = "filePath";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_SELECTED = "selected";
    
    private static final int SYNC_INTERVAL_MINUTES = 5;
    
    // Singleton instance
    private static DynamoDBService instance;
    
    private final boolean isTestMode;
    private DynamoDbClient dynamoDbClient;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock syncLock = new ReentrantLock();
    private final List<FileRecord> pendingRecords = new ArrayList<>();
    
    /**
     * Private constructor for singleton
     */
    private DynamoDBService() {
        String testModeProperty = System.getProperty("local.test.mode", "true");
        isTestMode = Boolean.parseBoolean(testModeProperty);
        scheduler = Executors.newScheduledThreadPool(1);
        logger.debug("POM local.test.mode is "+testModeProperty);
        logger.info("DynamoDBService initialized in {} mode", isTestMode ? "TEST" : "PRODUCTION");
    }
    
    /**
     * Gets the singleton instance of the service.
     * 
     * @return The DynamoDBService instance
     */
    public static synchronized DynamoDBService getInstance() {
        if (instance == null) {
            instance = new DynamoDBService();
        }
        return instance;
    }
    
    /**
     * Initializes the DynamoDB client and schedules synchronization.
     */
    public void initialize() {
        if (!isTestMode) {
            try {
                // Initialize the DynamoDB client with default AWS profile and region
                dynamoDbClient = DynamoDbClient.builder()
                        .region(Region.US_EAST_1)  // Change to your preferred region
                        .credentialsProvider(ProfileCredentialsProvider.create())
                        .build();
                
                // Check if table exists, create if not
                ensureTableExists();
                
                logger.info("DynamoDB client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize DynamoDB client", e);
                throw new RuntimeException("Failed to initialize DynamoDB service", e);
            }
        } else {
            logger.info("Running in test mode, DynamoDB operations will be simulated");
        }
        
        // Schedule periodic synchronization
        scheduler.scheduleAtFixedRate(
                this::synchronizeRecords, 
                SYNC_INTERVAL_MINUTES, 
                SYNC_INTERVAL_MINUTES, 
                TimeUnit.MINUTES);
        
        logger.info("Scheduled synchronization every {} minutes", SYNC_INTERVAL_MINUTES);
    }
    
    /**
     * Ensures that the DynamoDB table exists, creates it if not.
     */
    private void ensureTableExists() {
        if (isTestMode) return;
        
        try {
            // Check if table exists
            dynamoDbClient.describeTable(req -> req.tableName(TABLE_NAME));
            logger.info("DynamoDB table '{}' already exists", TABLE_NAME);
        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, create it
            logger.info("DynamoDB table '{}' not found, creating...", TABLE_NAME);
            
            CreateTableRequest request = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName(KEY_FILEPATH)
                        .keyType(KeyType.HASH)
                        .build()
                )
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName(KEY_FILEPATH)
                        .attributeType(ScalarAttributeType.S)
                        .build()
                )
                .provisionedThroughput(
                    ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build()
                )
                .build();
            
            dynamoDbClient.createTable(request);
            
            // Wait for table to be created
            boolean tableCreated = false;
            int attempts = 0;
            while (!tableCreated && attempts < 10) {
                try {
                    Thread.sleep(1000);
                    TableDescription tableDesc = dynamoDbClient.describeTable(
                            req -> req.tableName(TABLE_NAME)).table();
                    tableCreated = "ACTIVE".equals(tableDesc.tableStatus());
                    attempts++;
                } catch (Exception waitEx) {
                    logger.warn("Error while waiting for table creation: {}", waitEx.getMessage());
                    attempts++;
                }
            }
            
            if (tableCreated) {
                logger.info("DynamoDB table '{}' created successfully", TABLE_NAME);
            } else {
                logger.warn("DynamoDB table '{}' creation may not have completed", TABLE_NAME);
            }
        }
    }
    
    /**
     * Adds a file record to the pending list for synchronization.
     * 
     * @param record The file record to add
     */
    public void addRecord(FileRecord record) {
        syncLock.lock();
        try {
            // Remove any existing record with the same path
            pendingRecords.removeIf(r -> r.getFilePath().equals(record.getFilePath()));
            pendingRecords.add(record);
            logger.debug("Added record to pending list: {}", record.getFilePath());
        } finally {
            syncLock.unlock();
        }
    }
    
    /**
     * Synchronizes all pending records with DynamoDB.
     */
    public void synchronizeRecords() {
        if (pendingRecords.isEmpty()) {
            logger.debug("No records to synchronize");
            return;
        }
        
        syncLock.lock();
        try {
            logger.info("Synchronizing {} records with DynamoDB", pendingRecords.size());
            
            if (!isTestMode) {
                List<FileRecord> recordsToSync = new ArrayList<>(pendingRecords);
                
                for (FileRecord record : recordsToSync) {
                    try {
                        // Check if record exists in DynamoDB
                        FileRecord existingRecord = getRecordFromDynamoDB(record.getFilePath());
                        
                        // Only update if the record doesn't exist or has a newer timestamp
                        if (existingRecord == null || record.getTimestamp() > existingRecord.getTimestamp()) {
                            putRecordToDynamoDB(record);
                            logger.debug("Updated record in DynamoDB: {}", record.getFilePath());
                        } else {
                            logger.debug("Skipped record update (older timestamp): {}", record.getFilePath());
                        }
                    } catch (Exception e) {
                        logger.error("Error syncing record: {}", record.getFilePath(), e);
                    }
                }
            } else {
                logger.info("TEST MODE: Simulated synchronization of {} records", pendingRecords.size());
            }
            
            pendingRecords.clear();
        } finally {
            syncLock.unlock();
        }
    }
    
    /**
     * Gets a file record from DynamoDB.
     * 
     * @param filePath The canonical file path
     * @return The file record or null if not found
     */
    private FileRecord getRecordFromDynamoDB(String filePath) {
        if (isTestMode) return null;
        
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(KEY_FILEPATH, AttributeValue.builder().s(filePath).build());
            
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .build());
            
            Map<String, AttributeValue> item = response.item();
            if (item == null || item.isEmpty()) {
                return null;
            }
            
            long timestamp = Long.parseLong(item.get(ATTR_TIMESTAMP).n());
            String username = item.get(ATTR_USERNAME).s();
            boolean selected = Boolean.parseBoolean(item.get(ATTR_SELECTED).bool().toString());
            
            return new FileRecord(filePath, timestamp, username, selected);
        } catch (Exception e) {
            logger.error("Error getting record from DynamoDB: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * Puts a file record into DynamoDB.
     * 
     * @param record The file record to put
     */
    private void putRecordToDynamoDB(FileRecord record) {
        if (isTestMode) return;
        
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(KEY_FILEPATH, AttributeValue.builder().s(record.getFilePath()).build());
            item.put(ATTR_TIMESTAMP, AttributeValue.builder().n(String.valueOf(record.getTimestamp())).build());
            item.put(ATTR_USERNAME, AttributeValue.builder().s(record.getUsername()).build());
            item.put(ATTR_SELECTED, AttributeValue.builder().bool(record.isSelected()).build());
            
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build());
        } catch (Exception e) {
            logger.error("Error putting record to DynamoDB: {}", record.getFilePath(), e);
        }
    }
    
    /**
     * Loads all file records from DynamoDB.
     * 
     * @return List of all file records
     */
    public List<FileRecord> loadAllRecords() {
        if (isTestMode) {
            logger.info("TEST MODE: Simulated loading of records from DynamoDB");
            return new ArrayList<>(pendingRecords);
        }
        
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();
            
            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<FileRecord> records = new ArrayList<>();
            
            for (Map<String, AttributeValue> item : response.items()) {
                String filePath = item.get(KEY_FILEPATH).s();
                long timestamp = Long.parseLong(item.get(ATTR_TIMESTAMP).n());
                String username = item.get(ATTR_USERNAME).s();
                boolean selected = item.get(ATTR_SELECTED).bool();
                
                records.add(new FileRecord(filePath, timestamp, username, selected));
            }
            
            logger.info("Loaded {} records from DynamoDB", records.size());
            return records;
        } catch (Exception e) {
            logger.error("Error loading records from DynamoDB", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Forces immediate synchronization of all pending records.
     */
    public void forceSynchronize() {
        synchronizeRecords();
    }
    
    /**
     * Shutdown hook to clean up resources and perform final synchronization.
     */
    public void shutdown() {
        logger.info("Shutting down DynamoDBService");
        
        // Force final synchronization
        synchronizeRecords();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        
        // Close DynamoDB client
        if (dynamoDbClient != null && !isTestMode) {
            dynamoDbClient.close();
        }
        
        logger.info("DynamoDBService shutdown complete");
    }
}