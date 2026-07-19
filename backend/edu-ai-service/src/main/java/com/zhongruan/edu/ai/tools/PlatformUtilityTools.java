package com.zhongruan.edu.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class PlatformUtilityTools {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm:ss", Locale.CHINA);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    public PlatformUtilityTools(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool(name = "getCurrentDateTime", description = "获取当前中国标准时间和日期；回答今天几号、星期几、当前时间时必须调用")
    public String currentDateTime() {
        return "当前中国标准时间：" + DATE_TIME.format(ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
    }

    @Tool(name = "getCurrentWeather", description = "查询指定中国城市当前天气；回答天气、温度、风速时必须调用，参数为城市中文名")
    public String currentWeather(String city) {
        String normalized = city == null || city.isBlank() ? "武汉" : city.trim();
        try {
            String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
            JsonNode places = getJson("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=zh&format=json");
            JsonNode results = places.path("results");
            if (!results.isArray() || results.isEmpty()) return "未找到城市“" + normalized + "”，请提供更具体的城市名。";
            JsonNode place = results.get(0);
            double latitude = place.path("latitude").asDouble();
            double longitude = place.path("longitude").asDouble();
            JsonNode weather = getJson("https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=Asia%2FShanghai").path("current");
            if (weather.isMissingNode()) return "天气服务暂未返回“" + normalized + "”的实时数据。";
            return "%s当前天气：%s，气温 %.1f℃，体感 %.1f℃，风速 %.1f km/h；观测时间 %s。".formatted(
                    place.path("name").asText(normalized), weatherLabel(weather.path("weather_code").asInt(-1)),
                    weather.path("temperature_2m").asDouble(), weather.path("apparent_temperature").asDouble(),
                    weather.path("wind_speed_10m").asDouble(), weather.path("time").asText("未知"));
        } catch (Exception exception) {
            return "天气服务暂时不可用，请稍后重试。";
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                .header("User-Agent", "smart-education-ai-assistant/1.0").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("weather http " + response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private String weatherLabel(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "少云";
            case 3 -> "阴";
            case 45, 48 -> "有雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67 -> "有雨";
            case 71, 73, 75, 77 -> "有雪";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95, 96, 99 -> "雷雨";
            default -> "天气代码 " + code;
        };
    }
}