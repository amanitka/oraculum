package com.oraculum.load.service;

public interface ParquetFileLoadService {
    /**
     * Merge records from a Parquet file natively into a target database table.
     *
     * @param parquetFilePath The absolute file path to the Parquet data on the shared volume.
     */
    void merge(String parquetFilePath);
}
