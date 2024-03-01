package io.github.laffeymyth;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(new Gson(), new OkHttpClient(), TimeUnit.SECONDS, 10);

        Document document = new Document();
        document.setDocId("test-doc-id");
        document.setOwnerInn("test-owner-inn");
        document.setParticipantInn("test-participant-inn");
        document.setProducerInn("test-producer-inn");
        document.setProductionDate("2022-01-01");
        document.setRegDate("2022-01-01");
        document.setRegNumber("test-reg-number");

        Product product = new Product();
        product.setOwnerInn("test-owner-inn");
        product.setProducerInn("test-producer-inn");
        product.setTnvedCode("test-tnved-code");
        product.setUitCode("test-uit-code");
        product.setUituCode("test-uitu-code");
        document.setProducts(List.of(product));

        String signature = "test-signature";

        Document response = api.createDocument(document, signature);

        System.out.println("Response: " + response);
    }

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore requestLimitSemaphore;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public CrptApi(Gson gson, OkHttpClient httpClient, TimeUnit timeUnit, int requestLimit) throws InterruptedException {
        this.requestLimitSemaphore = new Semaphore(requestLimit, true);
        this.requestLimitSemaphore.tryAcquire(requestLimit, timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public Document createDocument(Document document, String signature) throws IOException, InterruptedException {
        RequestBody requestBody = RequestBody.create(gson.toJson(document), okhttp3.MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .header("X-Signature", signature)
                .build();

        requestLimitSemaphore.acquire();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new IOException("response body is null " + response);
            }

            return gson.fromJson(response.body().string(), Document.class);
        } finally {
            requestLimitSemaphore.release();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private Description description;
        @SerializedName("doc_id")
        private String docId;
        @SerializedName("doc_status")
        private String docStatus;
        @SerializedName("doc_type")
        private String doc_type = "LP_INTRODUCE_GOODS";
        private boolean importRequest;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("participant_inn")
        private String participantInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private String productionDate;
        @SerializedName("production_type")
        private String productionType;
        private List<Product> products;
        @SerializedName("reg_date")
        private String regDate;
        @SerializedName("reg_number")
        private String regNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        @SerializedName("certificate_document")
        private String certificateDocument;
        @SerializedName("certificate_document_date")
        private String certificateDocumentDate;
        @SerializedName("certificate_document_number")
        private String certificateDocumentNumber;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private String productionDate;
        @SerializedName("tnved_code")
        private String tnvedCode;
        @SerializedName("uit_code")
        private String uitCode;
        @SerializedName("uitu_code")
        private String uituCode;
    }
}
