package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import org.json.JSONArray;
import org.xwalk.core.XWalkView;

/**
 * Created by fengxj on 10/31/16.
 */

public class SugoWebViewClient extends WebViewClient {
    private static String pageViewScript = "sugo.track('', 'h5_enter_page_event', {" + SGConfig.FIELD_PAGE + ": sugo.relative_path});\n" +
            "sugo.enter_time = new Date().getTime();\n" +
            "\n" +
            "window.addEventListener('beforeunload', function (e) {\n" +
            "\tvar duration = (new Date().getTime() - sugo.enter_time)/1000;\n" +
            "    sugo.track('', 'h5_stay_event', {" + SGConfig.FIELD_PAGE + ": sugo.relative_path, " + SGConfig.FIELD_DURATION + ": duration});\n" +
            "});";
    private static String cssUtil = "var UTILS = {};\n" +
            "UTILS.cssPath = function(node, optimized)\n" +
            "{\n" +
            "    if (node.nodeType !== Node.ELEMENT_NODE)\n" +
            "        return \"\";\n" +
            "    var steps = [];\n" +
            "    var contextNode = node;\n" +
            "    while (contextNode) {\n" +
            "        var step = UTILS._cssPathStep(contextNode, !!optimized, contextNode === node);\n" +
            "        if (!step)\n" +
            "            break; \n" +
            "        steps.push(step);\n" +
            "        if (step.optimized)\n" +
            "            break;\n" +
            "        contextNode = contextNode.parentNode;\n" +
            "    }\n" +
            "    steps.reverse();\n" +
            "    return steps.join(\" > \");\n" +
            "};\n" +
            "UTILS._cssPathStep = function(node, optimized, isTargetNode)\n" +
            "{\n" +
            "    if (node.nodeType !== Node.ELEMENT_NODE)\n" +
            "        return null;\n" +
            " \n" +
            "    var id = node.getAttribute(\"id\");\n" +
            "    if (optimized) {\n" +
            "        if (id)\n" +
            "            return new UTILS.DOMNodePathStep(idSelector(id), true);\n" +
            "        var nodeNameLower = node.nodeName.toLowerCase();\n" +
            "        if (nodeNameLower === \"body\" || nodeNameLower === \"head\" || nodeNameLower === \"html\")\n" +
            "            return new UTILS.DOMNodePathStep(node.nodeName.toLowerCase(), true);\n" +
            "     }\n" +
            "    var nodeName = node.nodeName.toLowerCase();\n" +
            " \n" +
            "    if (id)\n" +
            "        return new UTILS.DOMNodePathStep(nodeName.toLowerCase() + idSelector(id), true);\n" +
            "    var parent = node.parentNode;\n" +
            "    if (!parent || parent.nodeType === Node.DOCUMENT_NODE)\n" +
            "        return new UTILS.DOMNodePathStep(nodeName.toLowerCase(), true);\n" +
            "\n" +
            "\n" +
            "    function prefixedElementClassNames(node)\n" +
            "    {\n" +
            "        var classAttribute = node.getAttribute(\"class\");\n" +
            "        if (!classAttribute)\n" +
            "            return [];\n" +
            "\n" +
            "        return classAttribute.split(/\\s+/g).filter(Boolean).map(function(name) {\n" +
            "            return \"$\" + name;\n" +
            "        });\n" +
            "     }\n" +
            " \n" +
            "\n" +
            "    function idSelector(id)\n" +
            "    {\n" +
            "        return \"#\" + escapeIdentifierIfNeeded(id);\n" +
            "    }\n" +
            "\n" +
            "    function escapeIdentifierIfNeeded(ident)\n" +
            "    {\n" +
            "        if (isCSSIdentifier(ident))\n" +
            "            return ident;\n" +
            "        var shouldEscapeFirst = /^(?:[0-9]|-[0-9-]?)/.test(ident);\n" +
            "        var lastIndex = ident.length - 1;\n" +
            "        return ident.replace(/./g, function(c, i) {\n" +
            "            return ((shouldEscapeFirst && i === 0) || !isCSSIdentChar(c)) ? escapeAsciiChar(c, i === lastIndex) : c;\n" +
            "        });\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    function escapeAsciiChar(c, isLast)\n" +
            "    {\n" +
            "        return \"\\\\\" + toHexByte(c) + (isLast ? \"\" : \" \");\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    function toHexByte(c)\n" +
            "    {\n" +
            "        var hexByte = c.charCodeAt(0).toString(16);\n" +
            "        if (hexByte.length === 1)\n" +
            "          hexByte = \"0\" + hexByte;\n" +
            "        return hexByte;\n" +
            "    }\n" +
            "\n" +
            "    function isCSSIdentChar(c)\n" +
            "    {\n" +
            "        if (/[a-zA-Z0-9_-]/.test(c))\n" +
            "            return true;\n" +
            "        return c.charCodeAt(0) >= 0xA0;\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    function isCSSIdentifier(value)\n" +
            "    {\n" +
            "        return /^-?[a-zA-Z_][a-zA-Z0-9_-]*$/.test(value);\n" +
            "    }\n" +
            "\n" +
            "    var prefixedOwnClassNamesArray = prefixedElementClassNames(node);\n" +
            "    var needsClassNames = false;\n" +
            "    var needsNthChild = false;\n" +
            "    var ownIndex = -1;\n" +
            "    var siblings = parent.children;\n" +
            "    for (var i = 0; (ownIndex === -1 || !needsNthChild) && i < siblings.length; ++i) {\n" +
            "        var sibling = siblings[i];\n" +
            "        if (sibling === node) {\n" +
            "            ownIndex = i;\n" +
            "            continue;\n" +
            "        }\n" +
            "        if (needsNthChild)\n" +
            "            continue;\n" +
            "        if (sibling.nodeName.toLowerCase() !== nodeName.toLowerCase())\n" +
            "            continue;\n" +
            "\n" +
            "        needsClassNames = true;\n" +
            "        var ownClassNames = prefixedOwnClassNamesArray;\n" +
            "        var ownClassNameCount = 0;\n" +
            "        for (var cn_idx = 0; cn_idx < ownClassNames.length; cn_idx++)\n" +
            "            ++ownClassNameCount;\n" +
            "        if (ownClassNameCount === 0) {\n" +
            "            needsNthChild = true;\n" +
            "            continue;\n" +
            "        }\n" +
            "        var siblingClassNamesArray = prefixedElementClassNames(sibling);\n" +
            "        for (var j = 0; j < siblingClassNamesArray.length; ++j) {\n" +
            "            var siblingClass = siblingClassNamesArray[j];\n" +
            "\t\t\tvar o_idx = ownClassNames.indexOf(siblingClass);\n" +
            "            if (o_idx === -1)\n" +
            "                continue;\n" +
            "            ownClassNames.splice(o_idx,1);\n" +
            "            if (!--ownClassNameCount) {\n" +
            "                needsNthChild = true;\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            " \n" +
            "    var result = nodeName.toLowerCase();\n" +
            "    if (isTargetNode && nodeName.toLowerCase() === \"input\" && node.getAttribute(\"type\") && !node.getAttribute(\"id\") && !node.getAttribute(\"class\"))\n" +
            "        result += \"[type=\\\"\" + node.getAttribute(\"type\") + \"\\\"]\";\n" +
            "    if (needsNthChild) {\n" +
            "        result += \":nth-child(\" + (ownIndex + 1) + \")\";\n" +
            "    } else if (needsClassNames) {\n" +
            "        for (var idx = 0;idx < ownClassNames.length; idx++) {\n" +
            "            result += \".\" + escapeIdentifierIfNeeded(ownClassNames[idx].substr(1));\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    return new UTILS.DOMNodePathStep(result, false);\n" +
            "};\n" +
            "\n" +
            "\n" +
            "UTILS.DOMNodePathStep = function(value, optimized)\n" +
            "{\n" +
            "    this.value = value;\n" +
            "    this.optimized = optimized || false;\n" +
            "};\n" +
            "\n" +
            "UTILS.DOMNodePathStep.prototype = {\n" +
            "\n" +
            "    toString: function()\n" +
            "    {\n" +
            "        return this.value;\n" +
            "    }\n" +
            "};";
    private static String script = ";\n" +
            "sugo.current_event_bindings = {};\n" +
            "for (var i = 0; i < sugo.h5_event_bindings.length; i++) {\n" +
            "  var b_event = sugo.h5_event_bindings[i];\n" +
            "  if (b_event.target_activity === sugo.current_page) {\n" +
            "    var key = JSON.stringify(b_event.path);\n" +
            "    sugo.current_event_bindings[key] = b_event;\n" +
            "  }\n" +
            "\n" +
            "};\n" +
            "sugo.isElementInViewport = function (rect) {\n" +
            "  return (\n" +
            "      rect.top >= 0 &&\n" +
            "      rect.left >= 0 &&\n" +
            "      rect.bottom <= sugo.clientHeight &&\n" +
            "      rect.right <= sugo.clientWidth\n" +
            "  );\n" +
            "};" +
            "sugo.handleNodeChild = function (childrens, jsonArry, parent_path, type) {\n" +
            "  var index_map = {};\n" +
            "  for (var i = 0; i < childrens.length; i++) {\n" +
            "    var children = childrens[i];\n" +
            "    var path = UTILS.cssPath(children);\n" +
            "    var htmlNode = {};\n" +
            "    htmlNode.innerText = children.innerText;\n" +
            "    htmlNode.path = path;\n" +
            "    if (type === 'report') {\n" +
            "      var rect = children.getBoundingClientRect();\n" +
            "      if (sugo.isElementInViewport(rect) == true) {\n" +
            "        var temp_rect = {\n" +
            "            top: rect.top,\n" +
            "            left: rect.left,\n" +
            "            width: rect.width,\n" +
            "            height: rect.height\n" +
            "        };\n" +
            "        htmlNode.rect = temp_rect;\n" +
            "        jsonArry.push(htmlNode);\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    if (children.children) {\n" +
            "      sugo.handleNodeChild(children.children, jsonArry, path, type);\n" +
            "    }\n" +
            "  }\n" +
            "};\n" +
            "sugo.delegate = function(eventType)  \n" +
            "{  \n" +
            "    function handle(e){\n" +
            "        var evt = window.event ? window.event : e;\n" +
            "        var target = evt.target || evt.srcElement;  \n" +
            "        var currentTarget= e ? e.currentTarget : this; \n" +
            "        var paths = Object.keys(sugo.current_event_bindings);\n" +
            "        for(var idx = 0;idx < paths.length; idx++) {\n" +
            "            var path_str = paths[idx];\n" +
            "            var event = sugo.current_event_bindings[path_str];\n" +
            "            if(event.event_type != eventType){\n" +
            "                continue;\n" +
            "            }\n" +
            "            var path = event.path.path;\n" +
            "            if(event.similar === true){\n" +
            "              path = path.replace(/:nth-child\\([0-9]*\\)/g, '');\n" +
            "            }\n" +
            "            var eles = document.querySelectorAll(path);\n" +
            "            if(eles){\n" +
            "            for (var eles_idx=0;eles_idx < eles.length; eles_idx ++){\n" +
            "                 var ele = eles[eles_idx];\n" +
            "                 var parentNode = target;\n" +
            "                 while(parentNode){\n" +
            "                     if(parentNode === ele){\n" +
            "                        var custom_props = {};\n" +
            "                        if(event.code && event.code.replace(/(^\\s*)|(\\s*$)/g, '') != ''){\n" +
            "                            var sugo_props = new Function('e', 'element', 'conf', 'instance', event.code);\n" +
            "                            custom_props = sugo_props(e, ele, event, sugo);\n" +
            "                        }\n" +
            "                        custom_props.from_binding = true;\n" +
            "                        custom_props.event_label = ele.innerText\n" +
            "                        sugo.track(event.event_id, event.event_name, custom_props);\n" +
            "                        break;\n" +
            "                     }\n" +
            "                     parentNode = parentNode.parentNode\n" +
            "                 }\n" +
            "                 \n" +
            "              }\n" +
            "            } \n" +
            "        }\n" +
            "        \n" +
            "        \n" +
            "    }  \n" +
            "    \n" +
            "    document.addEventListener(eventType, handle);\n" +
            "};  \n" +
            "sugo.bindEvent = function () {\n" +
            "    sugo.delegate('click'); \n" +
            "    sugo.delegate('focus'); \n" +
            "    sugo.delegate('submit'); \n" +
            "    sugo.delegate('change'); \n" +
            "};" +
            "sugo.bindEvent();\n" +
            "sugo.reportNodes = function () {\n" +
            "  var jsonArry = [];\n" +
            "  var body = document.getElementsByTagName('body')[0];\n" +
            "  var childrens = body.children;\n" +
            "  var parent_path = '';\n" +
            "  sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "  sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "  sugo.handleNodeChild(childrens, jsonArry, parent_path, 'report');\n" +
            "  window.sugoWebNodeReporter.reportNodes(sugo.relative_path, JSON.stringify(jsonArry), sugo.clientWidth, sugo.clientHeight);\n" +
            "};";
    private static String initScript = "var sugo = {};\n" +
            "sugo.track = function(event_id, event_name, props){\n" +
            "    window.sugoEventListener.track(event_id, event_name, JSON.stringify(props));\n" +
            "};\n" +
            "\n" +
            "sugo.timeEvent = function(event_name){\n" +
            "    window.sugoEventListener.timeEvent(event_name);\n" +
            "};";

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        handlePageFinished(view, url);
    }


    public static void handlePageFinished(WebView view, String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.setWebContentsDebuggingEnabled(true);
        }

        Context context = view.getContext();

        Activity activity = (Activity) context;
        String script = getInjectScript(activity);
        view.loadUrl("javascript:" + script);

    }

    public static void handlePageFinished(XWalkView view, String url) {
        Context context = view.getContext();
        Activity activity = (Activity) context;
        String script = getInjectScript(activity);
        view.load("javascript:" + script, "");

    }

    public static void handlePageFinished(WebViewDelegate delegate, Activity activity, String url) {
        String script = getInjectScript(activity);
        delegate.loadUrl("javascript:" + script);
    }

    public static String getInjectScript(Activity activity) {
        SugoAPI sugoInstance = SugoAPI.getInstance(activity);
        String token = sugoInstance.getmConfig().getToken();
        String activityName = activity.getClass().getName();
        JSONArray eventBindings = SugoWebEventListener.getBindEvents(token);
        StringBuffer scriptBuf = new StringBuffer();
        scriptBuf.append(cssUtil);
        scriptBuf.append(initScript);
        scriptBuf.append("sugo.relative_path = window.location.pathname.replace(")
                .append(sugoInstance.getmConfig().getWebRoot())
                .append(", '');");
        scriptBuf.append(pageViewScript);
        scriptBuf.append("sugo.current_page ='");
        scriptBuf.append(activityName);
        scriptBuf.append("::' + sugo.relative_path;");
        scriptBuf.append(" sugo.h5_event_bindings =");
        scriptBuf.append(eventBindings.toString());
        scriptBuf.append(script);
        return scriptBuf.toString();
    }
}
