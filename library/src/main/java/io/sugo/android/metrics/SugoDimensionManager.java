package io.sugo.android.metrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * @author Administrator
 * @date 2017/2/16
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
                /** 维度类型：0=long,1=float,2=string;3=dateString;4=date; 5=int */
                switch (dataType) {
                    case 0:
                        typeStr = "l";
                        break;
                    case 1:
                    case 7:
                    case 8:
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
                        break;
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

}
