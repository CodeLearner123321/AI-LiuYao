package com.divination.liuyao.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class BrowserScreenshotService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() { };
    private static final List<String> BROWSER_CANDIDATES = Arrays.asList(
        System.getenv("MCP_BROWSER_PATH"),
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
        "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
    );
    private static final long STARTUP_TIMEOUT_MILLIS = 10_000L;
    private static final long COMMAND_TIMEOUT_MILLIS = 10_000L;
    private static final String BROWSER_NO_SANDBOX_ENV = "MCP_BROWSER_NO_SANDBOX";

    private final Object sessionLock = new Object();
    private volatile BrowserSession session;

    public Path captureHtml(Path htmlPath, Path outputImagePath, int width, int height) throws IOException, InterruptedException {
        Path normalizedOutput = outputImagePath.toAbsolutePath().normalize();
        if (normalizedOutput.getParent() != null) {
            Files.createDirectories(normalizedOutput.getParent());
        }

        BrowserSession activeSession = getOrCreateSession();
        TemporaryHttpFileServer fileServer = TemporaryHttpFileServer.start(htmlPath);
        try {
            activeSession.capture(fileServer.entryUri().toString(), normalizedOutput, width, height);
        } catch (IOException ex) {
            invalidateSession(activeSession);
            throw ex;
        } finally {
            fileServer.close();
        }
        return normalizedOutput;
    }

    private BrowserSession getOrCreateSession() throws IOException, InterruptedException {
        BrowserSession current = session;
        if (current != null && current.isAlive()) {
            return current;
        }
        synchronized (sessionLock) {
            current = session;
            if (current != null && current.isAlive()) {
                return current;
            }
            closeQuietly(current);
            session = BrowserSession.start(resolveBrowserPath());
            return session;
        }
    }

    private void invalidateSession(BrowserSession current) {
        synchronized (sessionLock) {
            if (session == current) {
                closeQuietly(session);
                session = null;
            }
        }
    }

    private void closeQuietly(BrowserSession current) {
        if (current == null) {
            return;
        }
        try {
            current.close();
        } catch (Exception ignored) {
            // Stale browser session cleanup is best effort.
        }
    }

    private Path resolveBrowserPath() {
        for (String candidate : BROWSER_CANDIDATES) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path path = Path.of(candidate);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
        }
        throw new IllegalStateException("未找到可用浏览器，请配置 MCP_BROWSER_PATH 或安装 Edge/Chrome");
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("127.0.0.1", 0));
            return socket.getLocalPort();
        }
    }

    private static final class BrowserSession implements AutoCloseable {

        private final Process process;
        private final Path userDataDir;
        private final WebSocket webSocket;
        private final AtomicInteger nextId = new AtomicInteger(1);
        private final Map<Integer, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
        private final LinkedBlockingQueue<Map<String, Object>> eventQueue = new LinkedBlockingQueue<>();
        private final Object commandLock = new Object();

        private BrowserSession(Process process, Path userDataDir, WebSocket webSocket) {
            this.process = process;
            this.userDataDir = userDataDir;
            this.webSocket = webSocket;
        }

        static BrowserSession start(Path browserPath) throws IOException, InterruptedException {
            int port = BrowserScreenshotService.findFreePort();
            Path userDataDir = Files.createTempDirectory("hexagram-browser-profile-");

            List<String> command = new ArrayList<>();
            command.add(browserPath.toString());
            command.add("--headless=new");
            command.add("--disable-gpu");
            command.add("--hide-scrollbars");
            if (isNoSandboxEnabled()) {
                command.add("--no-sandbox");
            }
            command.add("--remote-debugging-port=" + port);
            command.add("--user-data-dir=" + userDataDir.toAbsolutePath().normalize());
            command.add("about:blank");

            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
            drainStream(process.getInputStream());

            URI debuggerUri = waitForDebuggerUri(port);
            DevToolsListener listener = new DevToolsListener();
            WebSocket webSocket = HTTP_CLIENT.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(debuggerUri, listener)
                .join();

            BrowserSession session = new BrowserSession(process, userDataDir, webSocket);
            listener.bind(session);
            Runtime.getRuntime().addShutdownHook(new Thread(session::closeQuietly));
            return session;
        }

        private static boolean isNoSandboxEnabled() {
            String value = System.getenv(BROWSER_NO_SANDBOX_ENV);
            return value != null && (
                "true".equalsIgnoreCase(value)
                    || "1".equals(value)
                    || "yes".equalsIgnoreCase(value)
                    || "on".equalsIgnoreCase(value)
            );
        }

        boolean isAlive() {
            return process.isAlive() && !webSocket.isOutputClosed();
        }

        void capture(String pageUri, Path outputPath, int width, int height) throws IOException, InterruptedException {
            synchronized (commandLock) {
                try {
                    String targetId = requiredString(sendCommand(null, "Target.createTarget", Map.of("url", "about:blank")), "targetId");
                    String sessionId = requiredString(sendCommand(null, "Target.attachToTarget", Map.of(
                        "targetId", targetId,
                        "flatten", true
                    )), "sessionId");
                    try {
                        sendCommand(sessionId, "Page.enable", Map.of());
                        sendCommand(sessionId, "Runtime.enable", Map.of());
                        sendCommand(sessionId, "Emulation.setDeviceMetricsOverride", Map.of(
                            "width", width,
                            "height", height,
                            "deviceScaleFactor", 1,
                            "mobile", false
                        ));
                        sendCommand(sessionId, "Page.navigate", Map.of("url", pageUri));
                        waitForLoadEvent(sessionId);
                        Map<String, Object> result = sendCommand(sessionId, "Page.captureScreenshot", Map.of(
                            "format", "png",
                            "captureBeyondViewport", false
                        ));
                        String encoded = requiredString(result, "data");
                        Files.write(outputPath, Base64.getDecoder().decode(encoded));
                    } finally {
                        safeCommand(null, "Target.closeTarget", Map.of("targetId", targetId));
                    }
                } catch (TimeoutException ex) {
                    throw new IOException("浏览器截图超时", ex);
                }
            }

            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                throw new IOException("浏览器截图未生成输出文件: " + outputPath);
            }
        }

        private Map<String, Object> sendCommand(String sessionId, String method, Map<String, Object> params)
            throws IOException, InterruptedException, TimeoutException {
            int id = nextId.getAndIncrement();
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("method", method);
            payload.put("params", params);
            if (sessionId != null) {
                payload.put("sessionId", sessionId);
            }

            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            pending.put(id, future);
            webSocket.sendText(OBJECT_MAPPER.writeValueAsString(payload), true).join();
            try {
                Map<String, Object> response = future.get(COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Object error = response.get("error");
                if (error != null) {
                    throw new IOException("浏览器命令失败: " + method + ", error=" + error);
                }
                return castMap(response.get("result"));
            } catch (java.util.concurrent.ExecutionException ex) {
                throw new IOException("浏览器命令失败: " + method, ex.getCause());
            } finally {
                pending.remove(id);
            }
        }

        private void waitForLoadEvent(String sessionId) throws InterruptedException, TimeoutException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(COMMAND_TIMEOUT_MILLIS);
            while (System.nanoTime() < deadline) {
                Map<String, Object> event = eventQueue.poll(200, TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }
                if (!Objects.equals(sessionId, event.get("sessionId"))) {
                    continue;
                }
                if ("Page.loadEventFired".equals(event.get("method"))) {
                    return;
                }
            }
            throw new TimeoutException("等待页面加载完成超时");
        }

        private void safeCommand(String sessionId, String method, Map<String, Object> params) {
            try {
                sendCommand(sessionId, method, params);
            } catch (Exception ignored) {
                // Cleanup errors should not shadow the main failure.
            }
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> castMap(Object value) {
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
            return Map.of();
        }

        private static String requiredString(Map<String, Object> source, String key) throws IOException {
            Object value = source.get(key);
            if (value == null) {
                throw new IOException("浏览器响应缺少字段: " + key);
            }
            String text = String.valueOf(value);
            if (text.isBlank()) {
                throw new IOException("浏览器响应字段为空: " + key);
            }
            return text;
        }

        @Override
        public void close() throws Exception {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            } catch (Exception ignored) {
                // Ignore close handshake failures.
            }
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            deleteRecursively(userDataDir);
        }

        private void closeQuietly() {
            try {
                close();
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
        }

        private static void waitForAllBytes(InputStream stream) throws IOException {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) {
                // Drain process output to avoid blocking on a full pipe.
            }
        }

        private static void drainStream(InputStream stream) {
            Thread thread = new Thread(() -> {
                try {
                    waitForAllBytes(stream);
                } catch (IOException ignored) {
                    // Ignore shutdown races.
                }
            }, "browser-screenshot-log-drain");
            thread.setDaemon(true);
            thread.start();
        }

        private static URI waitForDebuggerUri(int port) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/json/version"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STARTUP_TIMEOUT_MILLIS);
            while (System.nanoTime() < deadline) {
                try {
                    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (response.statusCode() == 200) {
                        Map<String, Object> json = OBJECT_MAPPER.readValue(response.body(), MAP_TYPE);
                        Object wsUrl = json.get("webSocketDebuggerUrl");
                        if (wsUrl != null && !String.valueOf(wsUrl).isBlank()) {
                            return URI.create(String.valueOf(wsUrl));
                        }
                    }
                } catch (IOException ignored) {
                    // Browser may still be booting.
                }
                Thread.sleep(100L);
            }
            throw new IOException("浏览器调试端口启动超时: " + port);
        }

        private static void deleteRecursively(Path root) throws IOException {
            if (root == null || !Files.exists(root)) {
                return;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Temp profile cleanup is best effort.
                        }
                    });
            }
        }
    }

    private static final class TemporaryHttpFileServer implements AutoCloseable {

        private final HttpServer server;
        private final Path rootDir;
        private final String entryFileName;

        private TemporaryHttpFileServer(HttpServer server, Path rootDir, String entryFileName) {
            this.server = server;
            this.rootDir = rootDir;
            this.entryFileName = entryFileName;
        }

        static TemporaryHttpFileServer start(Path entryHtmlPath) throws IOException {
            Path normalizedHtml = entryHtmlPath.toAbsolutePath().normalize();
            Path rootDir = normalizedHtml.getParent();
            if (rootDir == null || !Files.exists(normalizedHtml)) {
                throw new IOException("HTML 文件不存在: " + normalizedHtml);
            }

            int port = BrowserScreenshotService.findFreePort();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            TemporaryHttpFileServer fileServer = new TemporaryHttpFileServer(server, rootDir, normalizedHtml.getFileName().toString());
            server.createContext("/", fileServer::handle);
            server.start();
            return fileServer;
        }

        URI entryUri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/" + entryFileName);
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendStatus(exchange, 405);
                    return;
                }

                String rawPath = exchange.getRequestURI().getPath();
                String decodedPath = URLDecoder.decode(rawPath == null || rawPath.isBlank() ? "/" : rawPath, StandardCharsets.UTF_8);
                String relativePath = decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;
                if (relativePath.isBlank()) {
                    relativePath = entryFileName;
                }

                Path target = rootDir.resolve(relativePath).normalize();
                if (!target.startsWith(rootDir) || !Files.exists(target) || !Files.isRegularFile(target)) {
                    sendStatus(exchange, 404);
                    return;
                }

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", contentType(target));
                long contentLength = Files.size(target);
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                exchange.sendResponseHeaders(200, contentLength);
                try (InputStream inputStream = Files.newInputStream(target, StandardOpenOption.READ)) {
                    inputStream.transferTo(exchange.getResponseBody());
                }
            } finally {
                exchange.close();
            }
        }

        private void sendStatus(HttpExchange exchange, int status) throws IOException {
            exchange.sendResponseHeaders(status, -1);
        }

        private String contentType(Path file) {
            String name = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase();
            if (name.endsWith(".html") || name.endsWith(".htm")) {
                return "text/html; charset=UTF-8";
            }
            if (name.endsWith(".png")) {
                return "image/png";
            }
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (name.endsWith(".svg")) {
                return "image/svg+xml";
            }
            if (name.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            }
            if (name.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            }
            return "application/octet-stream";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class DevToolsListener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();
        private volatile BrowserSession session;

        void bind(BrowserSession session) {
            this.session = session;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                routeMessage(message);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            BrowserSession current = session;
            if (current != null) {
                current.pending.values().forEach(future -> future.completeExceptionally(error));
            }
            throw new UncheckedIOException(new IOException("浏览器调试连接异常", error));
        }

        private void routeMessage(String message) {
            BrowserSession current = session;
            if (current == null) {
                return;
            }
            try {
                Map<String, Object> json = OBJECT_MAPPER.readValue(message, MAP_TYPE);
                Object idValue = json.get("id");
                if (idValue instanceof Number) {
                    CompletableFuture<Map<String, Object>> future = current.pending.get(((Number) idValue).intValue());
                    if (future != null) {
                        future.complete(json);
                    }
                    return;
                }
                if (json.containsKey("method")) {
                    current.eventQueue.offer(json);
                }
            } catch (IOException ex) {
                current.pending.values().forEach(future -> future.completeExceptionally(ex));
            }
        }
    }
}
