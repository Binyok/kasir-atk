package giriatk.service;

import giriatk.dao.PengaturanDao;
import giriatk.model.TransaksiItem;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class MidtransService {

    private final PengaturanDao pengaturanDao = new PengaturanDao();
    private final HttpClient client;

    public static final class SnapResponse {
        private final String token;
        private final String redirectUrl;

        public SnapResponse(String token, String redirectUrl) {
            this.token = token;
            this.redirectUrl = redirectUrl;
        }

        public String getToken() {
            return token;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }
    }

    public static final class StatusResponse {
        private final String transactionStatus;
        private final String paymentType;
        private final String statusCode;
        private final String statusMessage;

        public StatusResponse(String transactionStatus, String paymentType, String statusCode, String statusMessage) {
            this.transactionStatus = transactionStatus == null ? "" : transactionStatus;
            this.paymentType = paymentType == null ? "" : paymentType;
            this.statusCode = statusCode == null ? "" : statusCode;
            this.statusMessage = statusMessage == null ? "" : statusMessage;
        }

        public String getTransactionStatus() {
            return transactionStatus;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public boolean isSuccess() {
            return "settlement".equalsIgnoreCase(transactionStatus)
                    || "capture".equalsIgnoreCase(transactionStatus);
        }

        public boolean isPending() {
            return "pending".equalsIgnoreCase(transactionStatus);
        }

        public boolean isFailed() {
            return "deny".equalsIgnoreCase(transactionStatus)
                    || "cancel".equalsIgnoreCase(transactionStatus)
                    || "expire".equalsIgnoreCase(transactionStatus);
        }
    }

    public MidtransService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String getConfig(String key, String defaultValue) {
        try {
            return pengaturanDao.get(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getServerKey() {
        return getConfig("midtrans_server_key", "SB-Mid-server-YOUR_SERVER_KEY").trim();
    }

    public String getClientKey() {
        return getConfig("midtrans_client_key", "SB-Mid-client-YOUR_CLIENT_KEY").trim();
    }

    public boolean isProduction() {
        return "1".equals(getConfig("midtrans_is_production", "0"));
    }

    public boolean isEnabled() {
        return "1".equals(getConfig("midtrans_enabled", "1"));
    }

    public boolean isConfigured() {
        String key = getServerKey();
        return !key.isBlank() && !key.contains("YOUR_SERVER_KEY");
    }

    private String getSnapApiUrl() {
        return isProduction()
                ? "https://app.midtrans.com/snap/v1/transactions"
                : "https://app.sandbox.midtrans.com/snap/v1/transactions";
    }

    private String getCoreApiUrl() {
        return isProduction()
                ? "https://api.midtrans.com/v2"
                : "https://api.sandbox.midtrans.com/v2";
    }

    private String authHeader() {
        String key = getServerKey() + ":";
        return "Basic " + Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    public SnapResponse createSnapTransaction(String orderId, BigDecimal amount, String customerName, List<TransaksiItem> items) throws Exception {
        String url = getSnapApiUrl();
        long grossAmount = amount.longValue();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"transaction_details\":{");
        json.append("\"order_id\":\"").append(escapeJson(orderId)).append("\",");
        json.append("\"gross_amount\":").append(grossAmount);
        json.append("},");
        json.append("\"credit_card\":{\"secure\":true}");
        if (customerName != null && !customerName.isBlank()) {
            json.append(",\"customer_details\":{\"first_name\":\"").append(escapeJson(customerName)).append("\"}");
        }
        json.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            String err = extractJsonVal(response.body(), "error_messages");
            if (err.isEmpty()) {
                err = response.body();
            }
            throw new Exception("Midtrans HTTP " + response.statusCode() + ": " + err);
        }

        String token = extractJsonVal(response.body(), "token");
        String redirectUrl = extractJsonVal(response.body(), "redirect_url");
        if (token.isEmpty() || redirectUrl.isEmpty()) {
            throw new Exception("Respon Midtrans tidak valid: " + response.body());
        }

        return new SnapResponse(token, redirectUrl);
    }

    public StatusResponse checkStatus(String orderId) throws Exception {
        String url = getCoreApiUrl() + "/" + URI.create(orderId).toASCIIString() + "/status";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Authorization", authHeader())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        String statusCode = extractJsonVal(body, "status_code");
        String statusMessage = extractJsonVal(body, "status_message");
        String transactionStatus = extractJsonVal(body, "transaction_status");
        String paymentType = extractJsonVal(body, "payment_type");

        return new StatusResponse(transactionStatus, paymentType, statusCode, statusMessage);
    }

    public boolean testConnection() throws Exception {
        String url = getCoreApiUrl() + "/DUMMY-TEST-CONN/status";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("Authorization", authHeader())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new Exception("Server Key Midtrans tidak valid (HTTP 401 Unauthorized).");
        }
        return true;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private String extractJsonVal(String json, String key) {
        if (json == null) return "";
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx + pattern.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : "";
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }
}
