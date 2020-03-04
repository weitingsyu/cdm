package com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder.types;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The local entity declaration for CDM folders format.
 */
public class LocalEntityDeclaration extends EntityDeclaration {
    private String entityPath;
    private List<DataPartition> dataPartitions;
    private List<DataPartitionPattern> dataPartitionPatterns;
    private OffsetDateTime lastFileStatusCheckTime;
    private OffsetDateTime lastFileModifiedTime;
    private OffsetDateTime lastChildFileModifiedTime;

    public String getEntityPath() {
        return entityPath;
    }

    public void setEntityPath(final String entityPath) {
        this.entityPath = entityPath;
    }

    public void setEntitySchema(final String entitySchema) {
        this.entityPath = entitySchema;
    }

    public List<DataPartition> getDataPartitions() {
        return dataPartitions;
    }

    public void setDataPartitions(final List<DataPartition> dataPartitions) {
        this.dataPartitions = dataPartitions;
    }

    public List<DataPartitionPattern> getDataPartitionPatterns() {
        return dataPartitionPatterns;
    }

    public void setDataPartitionPatterns(final List<DataPartitionPattern> dataPartitionPatterns) {
        this.dataPartitionPatterns = dataPartitionPatterns;
    }

    public OffsetDateTime getLastFileStatusCheckTime() {
        return lastFileStatusCheckTime;
    }

    public void setLastFileStatusCheckTime(final OffsetDateTime lastFileStatusCheckTime) {
        this.lastFileStatusCheckTime = lastFileStatusCheckTime;
    }

    public OffsetDateTime getLastFileModifiedTime() {
        return lastFileModifiedTime;
    }

    public void setLastFileModifiedTime(final OffsetDateTime lastFileModifiedTime) {
        this.lastFileModifiedTime = lastFileModifiedTime;
    }

    public OffsetDateTime getLastChildFileModifiedTime() {
        return lastChildFileModifiedTime;
    }

    public void setLastChildFileModifiedTime(final OffsetDateTime lastChildFileModifiedTime) {
        this.lastChildFileModifiedTime = lastChildFileModifiedTime;
    }
}