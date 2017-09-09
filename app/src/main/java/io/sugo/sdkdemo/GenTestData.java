package io.sugo.sdkdemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.UUID;

import io.sugo.android.mpmetrics.SGConfig;

/**
 * Created by Ouwenjie on 2017/7/31.
 */

public class GenTestData {


    public static final String deviceJSON = "[{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米6\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米Max2\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米5c\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米4X\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米Note2\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"小米\",\"device_brand\":\"小米\",\"device_model\":\"小米5\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"三星\",\"device_brand\":\"三星\",\"device_model\":\"GALAXY S8\",\"screen_height\":2960,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"三星\",\"device_brand\":\"三星\",\"device_model\":\"GALAXY S8+\",\"screen_height\":2960,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"三星\",\"device_brand\":\"三星\",\"device_model\":\"GALAXY Note7\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"三星\",\"device_brand\":\"三星\",\"device_model\":\"GALAXY S7\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"VIVO\",\"device_brand\":\"VIVO\",\"device_model\":\"X9\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"VIVO\",\"device_brand\":\"VIVO\",\"device_model\":\"X9PLUS\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"VIVO\",\"device_brand\":\"VIVO\",\"device_model\":\"XPlay6\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"VIVO\",\"device_brand\":\"VIVO\",\"device_model\":\"XPlay5\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"华为\",\"device_brand\":\"华为\",\"device_model\":\"P10\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"华为\",\"device_brand\":\"华为\",\"device_model\":\"Mate9\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"华为\",\"device_brand\":\"华为\",\"device_model\":\"P10PLUS\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"华为\",\"device_brand\":\"华为\",\"device_model\":\"荣耀V9\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"华为\",\"device_brand\":\"华为\",\"device_model\":\"荣耀8\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"OPPO\",\"device_brand\":\"OPPO\",\"device_model\":\"R9s\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"OPPO\",\"device_brand\":\"OPPO\",\"device_model\":\"R11\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"OPPO\",\"device_brand\":\"OPPO\",\"device_model\":\"R9\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"OPPO\",\"device_brand\":\"OPPO\",\"device_model\":\"R9 Plus\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"魅族\",\"device_brand\":\"魅族\",\"device_model\":\"魅蓝 E2\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"魅族\",\"device_brand\":\"魅族\",\"device_model\":\"魅蓝 PRO6s\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"魅族\",\"device_brand\":\"魅族\",\"device_model\":\"魅蓝 PRO6 Plus\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"摩托罗拉\",\"device_brand\":\"摩托罗拉\",\"device_model\":\"Moto Z\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"摩托罗拉\",\"device_brand\":\"摩托罗拉\",\"device_model\":\"Moto X Pro\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"索尼\",\"device_brand\":\"索尼\",\"device_model\":\"Xperia XZ\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"索尼\",\"device_brand\":\"索尼\",\"device_model\":\"Xperia XZs\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"中兴\",\"device_brand\":\"中兴\",\"device_model\":\"天机7 Max\",\"screen_height\":1920,\"screen_width\":1080,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"},{\"manufacturer\":\"中兴\",\"device_brand\":\"中兴\",\"device_model\":\"天机7\",\"screen_height\":2560,\"screen_width\":1440,\"device_id\":\"353347063251878\",\"session_id\":\"873DEDCD-301F-4A04-8AF5-5D3422C0BE06\",\"distinct_id\":\"ab4d08a6-d3e3-426e-9036-5f6ab0640de1\",\"page_name\":\"测试页面名称\",\"event_type\":\"测试事件类型\"}]";


    public static final String[] KEYS = new String[]{"manufacturer", "device_brand", "device_model", "screen_height", "screen_width", "device_id", "session_id",
            "distinct_id", "page_name", "event_type"};

    public static JSONArray mDevices = null;

    public static Random sRandom = new Random();

    public static String distinctId = UUID.randomUUID().toString();

    public static JSONObject getRandomPhone(boolean updateUser) {
        if (updateUser) {
            distinctId = UUID.randomUUID().toString();
        }

        if (mDevices == null) {
            try {
                mDevices = new JSONArray(deviceJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject props = null;
        try {
            props = new JSONObject(mDevices.getJSONObject(sRandom.nextInt(31)), KEYS);
            props.put(SGConfig.FIELD_PAGE, genRandomPath());
            props.put(SGConfig.FIELD_DISTINCT_ID, distinctId);
            props.put(SGConfig.SESSION_ID, UUID.randomUUID().toString().toUpperCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return props;
    }

    static String[] PATH_WORDS = new String[]{
            "AAAAAAAAAA", "BBBBBBBBBB", "CCCCCCCCCC", "DDDDDDDDDD", "EEEEEEEEEE",
            "FFFFFFFFFF", "GGGGGGGGGG", "HHHHHHHHHH", "IIIIIIIIII", "JJJJJJJJJJ", "KKKKKKKKKK",
            "LLLLLLLLLL", "MMMMMMMMMM", "NNNNNNNNNN", "OOOOOOOOOO", "PPPPPPPPPP", "QQQQQQQQQQ",
            "RRRRRRRRRR", "SSSSSSSSSS", "TTTTTTTTTT", "UUUUUUUUUU", "VVVVVVVVVV", "WWWWWWWWWW",
            "XXXXXXXXXX", "YYYYYYYYYY", "ZZZZZZZZZZ"};


    public static String genRandomPath() {
        StringBuilder path = new StringBuilder();
        int deap = sRandom.nextInt(10);
        String p;
        for (int i = 0; i < deap; i++) {
            p = PATH_WORDS[sRandom.nextInt(26)];
            path.append(p);
            if (i < deap - 1) {
                path.append("/");
            } else {
                path.append(".html");
            }
        }
        return path.toString();
    }
}
