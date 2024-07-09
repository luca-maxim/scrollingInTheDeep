package com.interventions.infinite_scrolling;

import org.json.JSONObject;

public interface VolleyCallBack {
    void onSuccess(JSONObject response);
    void onFailure(JSONObject response);
}
