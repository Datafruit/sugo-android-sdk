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
            "for(var i=0;i<sugo.h5_event_bindings.length;i++){\n" +
            "\tvar b_event = sugo.h5_event_bindings[i];\n" +
            "\tif(b_event.target_activity === sugo.current_page){\n" +
            "\t\tvar key = JSON.stringify(b_event.path);\n" +
            "\t\tsugo.current_event_bindings[key] = b_event;\n" +
            "\t}\n" +
            "\t\n" +
            "};\n" +
            "\n" +
            "sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "sugo.isElementInViewport = function(rect) {\n" +
            "        return (\n" +
            "                rect.top >= 0 &&\n" +
            "                rect.left >= 0 &&\n" +
            "                rect.bottom <= sugo.clientHeight && \n" +
            "                rect.right <= sugo.clientWidth\n" +
            "        );\n" +
            "};\n" +
            "\n" +
            "\n" +
            "sugo.get_node_name = function(node){\n" +
            "\tvar path = '';\n" +
            "\tvar name = node.localName;\n" +
            "\t\n" +
            "\tif(name == 'script'){return '';}\n" +
            "\t\n" +
            "\tif(name == 'link'){return '';}\n" +
            "\t\n" +
            "\tpath = name;\n" +
            "\tid = node.id;\n" +
            "\tif(id && id.length>0){\n" +
            "\t\tpath += '#' + id;\n" +
            "\t}\n" +
            "\treturn path;\n" +
            "};\n" +
            "\n" +
            "\n" +
            "sugo.handleNodeChild = function(childrens,jsonArry,parent_path,type){\n" +
            "\t\tvar index_map={};\n" +
            "\t\tfor(var i=0;i<childrens.length;i++){\n" +
            "\t\t\tvar children = childrens[i];\n" +
            "\t\t\tvar node_name = sugo.get_node_name(children);\n" +
            "\t\t\tif (node_name == ''){ continue;}\n" +
            "\t\t\tif(index_map[node_name] == null){\n" +
            "\t\t\t\tindex_map[node_name] = 0;\n" +
            "\t\t\t}else{\n" +
            "\t\t\t\tindex_map[node_name] = index_map[node_name]  + 1;\n" +
            "\t\t\t}\n" +
            "\t\t\tvar htmlNode={};\n" +
            "\t\t\tvar path=parent_path + '/' + node_name + '[' + index_map[node_name] + ']';\n" +
            "\t\t\thtmlNode.path=path;\n" +
            "\t\t\tif(type === 'report'){\n" +
            "\t\t\t    var rect = children.getBoundingClientRect();\n" +
            "\t\t\t    if(sugo.isElementInViewport(rect) == true){ \n" +
            "\t\t\t\t  htmlNode.rect=rect;\n" +
            "\t\t\t\t  jsonArry.push(htmlNode);\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}\n" +
            "\t\t\tif(type === 'bind'){\n" +
            "\t\t\t\tvar b_event = sugo.current_event_bindings[JSON.stringify(htmlNode)];\n" +
            "\t\t\t\tif(b_event){\n" +
            "\t\t\t\t\tvar event = JSON.parse(JSON.stringify(b_event));\n" +
            "\t\t\t\t\tchildren.addEventListener(event.event_type,function(e){\n" +
            "\t\t\t\t\t\twindow.sugoEventListener.eventOnAndroid(event.event_id,event.event_name,'{}');\n" +
            "\t\t\t\t\t});\n" +
            "\t\t\t\t}\n" +
            "\t\t\t\t\n" +
            "\t\t\t}\n" +
            "\t\t\t\n" +
            "\t\t\tif(children.children){\n" +
            "\t\t\t\tsugo.handleNodeChild(children.children,jsonArry,path,type);\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "};\n" +
            "\n" +
            "sugo.bindEvent = function(){\n" +
            "\tvar jsonArry=[];\n" +
            "\tvar body = document.getElementsByTagName('body')[0];\n" +
            "\tvar childrens = body.children;\n" +
            "\tvar parent_path='';\n" +
            "\tsugo.handleNodeChild(childrens,jsonArry,parent_path,'bind');\n" +
            "\t\n" +
            "\n" +
            "};\n" +
            "sugo.bindEvent();\n" +
            "\n" +
            "\n" +
            "sugo.reportNodes = function(){\n" +
            "\n" +
            "\tvar jsonArry=[];\n" +
            "\tvar body = document.getElementsByTagName('body')[0];\n" +
            "\tvar childrens = body.children;\n" +
            "\tvar parent_path='';\n" +
            "\tsugo.handleNodeChild(childrens,jsonArry,parent_path,'report');\n" +
            "\n" +
            "\twindow.sugoWebNodeReporter.reportNodes(window.location.pathname,JSON.stringify(jsonArry),sugo.clientWidth,sugo.clientHeight);\n" +
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
