package Dto  ;

public class response {

    private String serviceName;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double errorRate;
    private double averageLatencyMs;

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public long getSuccessfulRequests() { return successfulRequests; }
    public void setSuccessfulRequests(long successfulRequests) { this.successfulRequests = successfulRequests; }
    public long getFailedRequests() { return failedRequests; }
    public void setFailedRequests(long failedRequests) { this.failedRequests = failedRequests; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public double getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }
}
