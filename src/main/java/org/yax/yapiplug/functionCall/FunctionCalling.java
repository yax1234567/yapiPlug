package org.yax.yapiplug.functionCall;

import com.alibaba.fastjson.JSONObject;

public interface FunctionCalling {
    String getName();

    String getDescription();

    JSONObject getInput_schema();

    String executeFunction(JSONObject input);
}
