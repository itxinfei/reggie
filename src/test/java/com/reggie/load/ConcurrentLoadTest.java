package com.reggie.load;

import com.reggie.ReggieApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 300用户并发负载测试
 * 测试核心接口在并发场景下的表现
 */
@SpringBootTest(classes = ReggieApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrentLoadTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;
    private String loginToken;

    private static final int CONCURRENCY = 300;
    private static final int TOTAL_REQUESTS = 1000;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();

    @org.junit.jupiter.api.BeforeAll
    public static void staticSetup() {
        // static context, no auto-injection
    }

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        restTemplate = new TestRestTemplate();
        loginToken = doLogin();
    }

    private String doLogin() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(
                    "{\"username\":\"admin\",\"password\":\"123456\"}", headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/employee/login", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return "test-session-token";
            }
        } catch (Exception e) {
            System.err.println("登录失败: " + e.getMessage());
        }
        return null;
    }

    @Test
    public void testConcurrentLoad() throws Exception {
        String baseUrl = "http://localhost:" + port;

        System.out.println("\n========== 300用户并发负载测试开始 ==========");
        System.out.println("并发用户数: " + CONCURRENCY);
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("服务地址: " + baseUrl);
        System.out.println("==========================================\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CONCURRENCY, CONCURRENCY,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(TOTAL_REQUESTS),
                new ThreadPoolExecutor.CallerRunsPolicy());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(TOTAL_REQUESTS);

        long startTime = System.currentTimeMillis();

        String[] endpoints = {
                "/employee/page?page=1&pageSize=10",
                "/dish/page?page=1&pageSize=10",
                "/category/list",
                "/order/page?page=1&pageSize=10"
        };

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            final String endpoint = endpoints[i % endpoints.length];

            executor.submit(() -> {
                try {
                    startLatch.await();
                    long requestStart = System.currentTimeMillis();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Authorization", "Bearer " + loginToken);
                    headers.set("X-Tenant-Id", "1");

                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.exchange(
                            baseUrl + endpoint, HttpMethod.GET, entity, String.class);

                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - requestStart;

                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        System.err.println("请求 " + requestId + " 返回状态: " + response.getStatusCode());
                    }

                    totalResponseTime.addAndGet(responseTime);
                    responseTimes.add(responseTime);

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("请求 " + requestId + " 异常: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(120, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        executor.shutdown();
        printResults(totalDuration, completed);
    }

    private void printResults(long totalDurationMs, boolean completed) {
        System.out.println("\n========== 测试结果 ==========");
        System.out.println("总耗时: " + (totalDurationMs / 1000) + " 秒");
        System.out.println("并发用户数: " + CONCURRENCY);
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求数: " + successCount.get());
        System.out.println("失败请求数: " + failCount.get());
        System.out.println("成功率: " + String.format("%.2f%%",
                (double) successCount.get() / TOTAL_REQUESTS * 100));
        System.out.println("测试是否完成: " + (completed ? "是" : "否（超时）"));

        if (TOTAL_REQUESTS > 0) {
            double avgResponseTime = (double) totalResponseTime.get() / TOTAL_REQUESTS;
            System.out.println("平均响应时间: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("吞吐量: " + String.format("%.2f req/s",
                    (double) TOTAL_REQUESTS / (totalDurationMs / 1000.0)));
        }

        if (!responseTimes.isEmpty()) {
            long[] times = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .sorted()
                    .toArray();

            System.out.println("\n响应时间分布:");
            System.out.println("  最小: " + times[0] + " ms");
            System.out.println("  最大: " + times[times.length - 1] + " ms");
            System.out.println("  P50:  " + times[times.length / 2] + " ms");
            System.out.println("  P90:  " + times[(int) (times.length * 0.9)] + " ms");
            System.out.println("  P95:  " + times[(int) (times.length * 0.95)] + " ms");
            System.out.println("  P99:  " + times[(int) (times.length * 0.99)] + " ms");
        }

        System.out.println("\n================================\n");
    }
}
