
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SwaggerToRap {

    public static void main(String[] args) {

        String exportJson = "{\"modelJSON\":\"{\\\"createDateStr\\\":\\\"2019-10-28\\\",\\\"user\\\":{\\\"name\\\":\\\"xxx\\\",\\\"id\\\":41},\\\"id\\\":36,\\\"version\\\":\\\"0.0.0.16\\\",\\\"introduction\\\":\\\"\\\",\\\"name\\\":\\\"运营调度\\\",\\\"moduleList\\\":[{\\\"id\\\":107,\\\"introduction\\\":\\\"\\\",\\\"name\\\":\\\"xxx\\\",\\\"pageList\\\":[]}]}\",\"code\":200,\"mockjsMap\":{},\"msg\":0}";
        String swaggerJson = HttpClientUtil.get("http://127.0.0.1:8080/v2/api-docs");

        swaggerJson = swaggerJson.replace("$ref","ref");

        JSONObject swagger =  JSON.parseObject(swaggerJson);
        JSONObject paths = swagger.getJSONObject("paths");
        Map pathMap = paths.toJavaObject(Map.class);

        Map<String,JSONArray> resultMap = new HashMap<>();
        JSONArray actionList = new JSONArray();

        for (Object o : pathMap.keySet()) {
            String path = String.valueOf(o);
            Map map = (Map)pathMap.get(o);
            String methodType = "";
            for (Object o1 : map.keySet()) {
                methodType = String.valueOf(o1);
            }
            JSONObject methodContent = paths.getJSONObject(path).getJSONObject(methodType);
            String group = methodContent.getJSONArray("tags").get(0).toString();

            JSONArray actions = resultMap.get(group);

            if(null == actions){
                actions = new JSONArray();
            }


            JSONObject action = new JSONObject();
            action.put("id",null);
            action.put("name",methodContent.getString("summary"));
            action.put("description","");
            String requestType = "1";
            switch (methodType){
                case "get":
                    requestType = "1";
                    break;
                case "post":
                    requestType = "2";
                    break;
                case "put":
                    requestType = "3";
                    break;
                case "delete":
                    requestType = "4";
                    break;
            }

            action.put("requestType",requestType);
            action.put("requestUrl", path);
            action.put("responseTemplate", "");

            JSONArray requestParameterList = new JSONArray();


            JSONArray summary = methodContent.getJSONArray("parameters");
            for (int i=0;i < summary.size();i++) {
                JSONObject jsonObject = summary.getJSONObject(i);
                JSONObject schema = jsonObject.getJSONObject("schema");
                if(null!=schema){
                    if(null !=schema.getString("ref")){
                        String definition = schema.getString("ref").replace("#/definitions/","");
                        ppp(definition,swagger,requestParameterList);
                    }
                }else{
                    String propertieName = jsonObject.getString("name");
                    String propertieType = jsonObject.getString("type");
                    String propertieDescription = jsonObject.getString("description");

                    JSONObject requestParameter = new JSONObject();
                    requestParameter.put("id",null);
                    requestParameter.put("identifier",propertieName);
                    requestParameter.put("name",propertieDescription);
                    requestParameter.put("remark","");
                    requestParameter.put("validator","");
                    JSONArray parameterList = new JSONArray();
                    requestParameter.put("dataType",propertieType);
                    requestParameter.put("parameterList",parameterList);

                    String ref = jsonObject.getString("ref");
                    if(null != ref){
                        String definition1 = ref.replace("#/definitions/", "");
                        ppp(definition1,swagger,parameterList);
                        requestParameter.put("parameterList",parameterList);
                    }
                    requestParameterList.add(requestParameter);
                   /* JSONObject properties = jsonObject.getJSONObject("properties");
                    Set<String> propertiesKey = properties.keySet();
                    for (String s : propertiesKey) {



                    }*/
                }
            }
            action.put("requestParameterList",requestParameterList);

            JSONArray responseParameterList = new JSONArray();

            JSONObject schema = methodContent.getJSONObject("responses").getJSONObject("200").getJSONObject("schema");
            if(null!=schema){
                if(null !=schema.getString("ref")){
                    String definition = schema.getString("ref").replace("#/definitions/","");
                    ppp(definition,swagger,responseParameterList);
                }
            }
            action.put("responseParameterList",responseParameterList);

            //actionList.add(action);
            actions.add(action);
            resultMap.put(group,actions);

        }
        //rap.put("actionList",actionList);


        JSONArray array = new JSONArray();
        for (String s : resultMap.keySet()) {
            JSONObject rap = new JSONObject();
            rap.put("id",null);
            rap.put("introduction", "");
            rap.put("name",s);
            rap.put("actionList",resultMap.get(s));
            array.add(rap);
        }
        JSONObject exp = JSON.parseObject(exportJson);
        String modelJSON = exp.getString("modelJSON");
        JSONObject model = JSON.parseObject(modelJSON);
        model.getJSONArray("moduleList").getJSONObject(0).put("pageList",array);
        exp.put("modelJSON", model.toJSONString());

        System.out.println(JSON.toJSONString(exp, SerializerFeature.WriteMapNullValue));

    }

    private static void ppp(String definition,JSONObject swagger,JSONArray parameterList) {
        JSONObject definitions = swagger.getJSONObject("definitions").getJSONObject(definition);
        JSONObject properties = definitions.getJSONObject("properties");
        Set<String> propertiesKey = properties.keySet();
        for (String s : propertiesKey) {
            String propertieName = s;
            String propertieType = properties.getJSONObject(s).getString("type");
            String propertieDescription = properties.getJSONObject(s).getString("description");

            JSONObject requestParameter = new JSONObject();
            requestParameter.put("id",null);
            requestParameter.put("identifier",propertieName);
            requestParameter.put("name",propertieDescription);
            requestParameter.put("remark","");
            requestParameter.put("validator","");
            JSONArray parameterList1 = new JSONArray();
            requestParameter.put("dataType",propertieType);
            requestParameter.put("parameterList",parameterList1);

            String ref = properties.getJSONObject(s).getString("ref");
            if(null == ref){
                JSONObject items = properties.getJSONObject(s).getJSONObject("items");
                if(null!=items){
                    ref = items.getString("ref");
                }
            }

            if(null != ref){
                String definition1 = ref.replace("#/definitions/", "");
                ppp(definition1,swagger,parameterList1);
                requestParameter.put("parameterList",parameterList1);
            }

            parameterList.add(requestParameter);

        }
    }
}
