package io.sugo.android.mpmetrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Administrator on 2017/2/16.
 */

public class SugoDimensionManager {

    private static final SugoDimensionManager sSugoDimensionManager = new SugoDimensionManager();

    private SugoDimensionManager() {
    }

    public static SugoDimensionManager getInstance() {
        return sSugoDimensionManager;
    }

    private final HashMap<String, String> mDimensions = new HashMap<>();

    public void setDimensions(JSONArray dimensions) {
        if (dimensions == null || dimensions.length() == 0) {
            mDimensions.clear();
            return;
        }
        mDimensions.clear();
        JSONObject dimensionObj = null;
        String dimensionName = null;
        int dataType = 0;
        for (int i = 0; i < dimensions.length(); i++) {
            try {
                dimensionObj = dimensions.getJSONObject(i);
                dimensionName = dimensionObj.getString("name");
                dataType = dimensionObj.getInt("type");
                if (dataType == 3) {
                    continue;
                }
                String typeStr = "s";
                switch (dataType) {      /** 维度类型：0=long,1=float,2=string;3=dateString;4=date; 5=int */
                    case 0:
                        typeStr = "l";
                        break;
                    case 1:
                        typeStr = "f";
                        break;
                    case 2:
                        typeStr = "s";
                        break;
                    case 4:
                        typeStr = "d";
                        break;
                    case 5:
                        typeStr = "i";
                    default:
                        break;
                }
                mDimensions.put(dimensionName, typeStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public HashMap<String, String> getDimensionTypes() {
        return mDimensions;
    }

    public void setDimensionsDefault() {
        try {
            setDimensions(new JSONArray("[\n" +
                    "    {\n" +
                    "      \"name\": \"__time\",\n" +
                    "      \"type\": 4\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_nation\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_province\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_city\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_district\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_area\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_latitude\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_longitude\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_city_timezone\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_timezone\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_phone_code\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_nation_code\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_continent\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_administrative\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_operator\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_ip\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_http_forward\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_http_refer\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_user_agent\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"browser\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"browser_version\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_args\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_http_cookie\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"app_name\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"app_version\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"app_build_number\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"session_id\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"network\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"device_id\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"bluetooth_version\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"has_bluetooth\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"device_brand\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"device_model\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"system_name\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"system_version\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"radio\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"carrier\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"screen_dpi\",\n" +
                    "      \"type\": 5\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"screen_height\",\n" +
                    "      \"type\": 5\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"screen_width\",\n" +
                    "      \"type\": 5\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"event_time\",\n" +
                    "      \"type\": 4\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"current_url\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"referrer\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"referring_domain\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"host\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"distinct_id\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"has_nfc\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"has_telephone\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"has_wifi\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"manufacturer\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"duration\",\n" +
                    "      \"type\": 1\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sdk_version\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"page_name\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"path_name\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"event_id\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"event_name\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"event_type\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"event_label\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"sugo_lib\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"token\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"from_binding\",\n" +
                    "      \"type\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"google_play_services\",\n" +
                    "      \"type\": 2\n" +
                    "    }\n" +
                    "  ]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
