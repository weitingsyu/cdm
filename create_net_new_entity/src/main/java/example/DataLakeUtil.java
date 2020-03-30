package example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;

public class DataLakeUtil {
    private static String storage_acccount_name_url = "https://wsdcdmstorageprd.blob.core.windows.net/"; // <your-storage-account-url>
    private static String sas_token = "?sv=2019-02-02&ss=bfqt&srt=sco&sp=rwdlacup&se=2021-03-30T09:37:19Z&st=2020-03-30T01:37:19Z&spr=https,http&sig=WfhLmw6%2BWtZkQkqGAhtbY0uHxLOT7w39oLJ7AZjEwTg%3D"; // <your-sasToken>

    public static void saveToDataLake(String filePath, String rootPath)
            throws InterruptedException, ExecutionException, IOException {
        DataLakeServiceClient dataLakeServiceClient = new DataLakeServiceClientBuilder()
                .endpoint(storage_acccount_name_url).sasToken(sas_token).buildClient();

        DataLakeFileSystemClient dataLakeFileSystemClient = dataLakeServiceClient.getFileSystemClient("powerbi");
        DataLakeDirectoryClient directoryClient = dataLakeFileSystemClient.getDirectoryClient(rootPath);
        File file = new File(filePath);
        saveFileToDataLake(file, directoryClient);

    }

    private static void saveFileToDataLake(File file, DataLakeDirectoryClient directoryClient) throws IOException {
        for (File subFile : file.listFiles()) {
            System.out.println(subFile.getAbsolutePath());
            if (subFile.isFile()) {

                DataLakeFileClient fileClient = directoryClient.createFile(subFile.getName());
                InputStream targetStream = new FileInputStream(subFile);
                long fileSize = subFile.length();
                fileClient.append(targetStream, 0, fileSize);
                fileClient.flush(fileSize);
                targetStream.close();
            } else {
                directoryClient.createSubdirectory(subFile.getName());
                DataLakeDirectoryClient directoryClient1 = directoryClient.getSubdirectoryClient(subFile.getName());
                saveFileToDataLake(subFile, directoryClient1);
            }

        }

    }
}