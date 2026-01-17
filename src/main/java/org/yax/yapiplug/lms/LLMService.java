package org.yax.yapiplug.lms;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
public class LLMService {
    // 1. 替换为 DeepSeek 的 API Key
    private static final String API_KEY = "sk-04f29052677b4397a73148b3e45e554f";
    // 2. 替换为 DeepSeek 的标准 API 地址
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    public static void sendToAI(Project project, String context) {
        System.out.println(context);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "DeepSeek 正在深入分析中...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String response = callDeepSeekAPI(context);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // 使用可滚动的对话框，因为 DeepSeek 返回内容可能很长
                        Messages.showInfoMessage(project, response, "DeepSeek 代码分析报告");
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "DeepSeek 调用失败: " + e.getMessage(), "网络请求错误");
                    });
                }
            }
        });
    }

    private static String callDeepSeekAPI(String promptContent) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);

        // 构建请求体
        JsonObject root = new JsonObject();

        root.addProperty("model", "deepseek-chat");

        JsonArray messages = new JsonArray();
        
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "你是一个 Spring Boot 专家。请分析 Controller 代码，" +
                "提取：1. 请求 URL 2. HTTP 方法 3. 入参字段(含 DTO 内部字段) 4. 返回值字段。请用 Markdown 表格展示。 5.接口方法简介尽量简短");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", promptContent);
        messages.add(userMsg);

        root.add("messages", messages);
        root.addProperty("stream", false);
        root.addProperty("temperature", 0.7); // 采样温度

        // 发送数据
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = new Gson().toJson(root).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 解析响应
        int code = conn.getResponseCode();
        if (code == 200) {
            Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
            String responseStr = scanner.useDelimiter("\\A").next();
            scanner.close();

            JsonObject responseJson = new Gson().fromJson(responseStr, JsonObject.class);
            return responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } else {
            // 获取错误流信息
            Scanner errorScanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
            String errorMsg = errorScanner.hasNext() ? errorScanner.useDelimiter("\\A").next() : "Unknown Error";
            throw new RuntimeException("HTTP " + code + ": " + errorMsg);
        }
    }
}
