package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.sugo.android.mpmetrics.SugoWebEventListener;

import org.json.JSONArray;

/**
 * Created by fengxj on 10/31/16.
 */

public class SugoWebViewClient extends WebViewClient {
    private String mToken;
    private String script= ";\n" +
            "sugo.current_event_bindings = {};\n" +
            "for (var i = 0; i < sugo.h5_event_bindings.length; i++) {\n" +
            "  var b_event = sugo.h5_event_bindings[i];\n" +
            "  if (b_event.target_activity === sugo.current_page) {\n" +
            "    var key = JSON.stringify(b_event.path);\n" +
            "    sugo.current_event_bindings[key] = b_event;\n" +
            "  }\n" +
            "\n" +
            "};\n" +
            "sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "sugo.isElementInViewport = function (rect) {\n" +
            "  return (\n" +
            "      rect.top >= 0 &&\n" +
            "      rect.left >= 0 &&\n" +
            "      rect.bottom <= sugo.clientHeight &&\n" +
            "      rect.right <= sugo.clientWidth\n" +
            "  );\n" +
            "};\n" +
            "sugo.get_node_name = function (node) {\n" +
            "  var path = '';\n" +
            "  var name = node.localName;\n" +
            "\n" +
            "  if (name == 'script') {\n" +
            "    return '';\n" +
            "  }\n" +
            "\n" +
            "  if (name == 'link') {\n" +
            "    return '';\n" +
            "  }\n" +
            "\n" +
            "  path = name;\n" +
            "  id = node.id;\n" +
            "  if (id && id.length > 0) {\n" +
            "    path += '#' + id;\n" +
            "  }\n" +
            "  return path;\n" +
            "};\n" +
            "sugo.handleNodeChild = function (childrens, jsonArry, parent_path, type) {\n" +
            "  var index_map = {};\n" +
            "  for (var i = 0; i < childrens.length; i++) {\n" +
            "    var children = childrens[i];\n" +
            "    var node_name = sugo.get_node_name(children);\n" +
            "    if (node_name == '') {\n" +
            "      continue;\n" +
            "    }\n" +
            "    if (index_map[node_name] == null) {\n" +
            "      index_map[node_name] = 0;\n" +
            "    } else {\n" +
            "      index_map[node_name] = index_map[node_name] + 1;\n" +
            "    }\n" +
            "    var htmlNode = {};\n" +
            "    var path = parent_path + '/' + node_name + '[' + index_map[node_name] + ']';\n" +
            "    htmlNode.path = path;\n" +
            "    if (type === 'report') {\n" +
            "      var rect = children.getBoundingClientRect();\n" +
            "      if (sugo.isElementInViewport(rect) == true) {\n" +
            "        htmlNode.rect = rect;\n" +
            "        jsonArry.push(htmlNode);\n" +
            "      }\n" +
            "    }\n" +
            "    if (type === 'bind') {\n" +
            "      var b_event = sugo.current_event_bindings[JSON.stringify(htmlNode)];\n" +
            "      if (b_event) {\n" +
            "        var event = JSON.parse(JSON.stringify(b_event));\n" +
            "        sugo.addEvent(children, event);\n" +
            "      }\n" +
            "\n" +
            "    }\n" +
            "    if (children.children) {\n" +
            "      sugo.handleNodeChild(children.children, jsonArry, path, type);\n" +
            "    }\n" +
            "  }\n" +
            "};\n" +
            "sugo.addEvent = function (children, event) {\n" +
            "  children.addEventListener(event.event_type, function (e) {\n" +
            "    var custom_props = {};\n" +
            "    if(event.code && event.code.replace(/(^\\s*)|(\\s*$)/g, \"\") != ''){\n" +
            "        eval(event.code);\n" +
            "        custom_props = sugo_props();\n" +
            "    }\n" +
            "    custom_props.from_binding = true;\n" +
            "    window.sugoEventListener.eventOnAndroid(event.event_id, event.event_name, JSON.stringify(custom_props));\n" +
            "  });\n" +
            "};" +
            "sugo.bindEvent = function () {\n" +
            "  var jsonArry = [];\n" +
            "  var body = document.getElementsByTagName('body')[0];\n" +
            "  var childrens = body.children;\n" +
            "  var parent_path = '';\n" +
            "  sugo.handleNodeChild(childrens, jsonArry, parent_path, 'bind');\n" +
            "\n" +
            "\n" +
            "};\n" +
            "sugo.bindEvent();\n" +
            "sugo.reportNodes = function () {\n" +
            "  var jsonArry = [];\n" +
            "  var body = document.getElementsByTagName('body')[0];\n" +
            "  var childrens = body.children;\n" +
            "  var parent_path = '';\n" +
            "  sugo.handleNodeChild(childrens, jsonArry, parent_path, 'report');\n" +
            "\n" +
            "  window.sugoWebNodeReporter.reportNodes(window.location.pathname, JSON.stringify(jsonArry), sugo.clientWidth, sugo.clientHeight);\n" +
            "};";

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.setWebContentsDebuggingEnabled(true);
        }
        Context context = view.getContext();
        String activityName = null;
        if (context instanceof Activity)
        {
            Activity activity = (Activity)context;
            activityName = activity.getClass().getName();
        }
        JSONArray eventBindings = SugoWebEventListener.getBindEvents(mToken);
        StringBuffer scriptBuf = new StringBuffer();
        scriptBuf.append("var sugo={}; sugo.current_page ='");
        scriptBuf.append(activityName);
        scriptBuf.append("::' + window.location.pathname;");
        scriptBuf.append(" sugo.h5_event_bindings =");
        scriptBuf.append(eventBindings.toString());
        scriptBuf.append(script);
        view.loadUrl("javascript:" + scriptBuf.toString());

    }

    public void setmToken(String mToken) {
        this.mToken = mToken;
    }
}
