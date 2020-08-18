import com.google.common.io.Files;

import java.io.*;
import java.net.*;


public class DataUploader {
    private String endpointUrl = "/monitoring/profiles/batch/";

    private String composeUrl(String base, long modelVersionId) throws java.net.URISyntaxException {
        return new URI(base).resolve(this.endpointUrl + modelVersionId).toString();
    }

    public int upload(String baseUrl, String filePath, long modelVersionId) throws Exception {
        String composedUrl = this.composeUrl(baseUrl, modelVersionId);
        HttpURLConnection connection = (HttpURLConnection) new URL(composedUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(4096);

        OutputStream output = connection.getOutputStream();
        Files.copy(new File(filePath), output);
        output.flush();

        return connection.getResponseCode();
    }

    public static void main(String[] args) throws Exception {
        DataUploader dataUploader = new DataUploader();
        int responseCode = dataUploader.upload(
            "http://<hydrosphere>/", "/path/to/data.csv", 1);
        System.out.println(responseCode);
    }
}
