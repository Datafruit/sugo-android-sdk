package io.sugo.android.mpmetrics;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import io.sugo.android.util.FileUtils;
import io.sugo.android.util.StringEscapeUtils;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 * Created by fengxj on 10/31/16.
 */

public class SugoWebViewClient extends WebViewClient {

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
            "};\n";

    private static String trackJS = "(function (sugo) {\n" +
            "    sugo.enable_page_event = $sugo_enable_page_event$;\n" +
            "\n" +
            "    if (window.sugo && sugo.enable_page_event) {\n" +
            "        var tmp_duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "        var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "        tmp_props.duration = tmp_duration;\n" +
            "        sugo.track('停留', tmp_props);\n" +
            "    }\n" +
            "\n" +
            "    sugo.relative_path = window.location.pathname.replace(/$sugo_webroot$/g, '');\n" +
            "    sugo.relative_path = sugo.relative_path.replace('$sugo_remove_path$', '');\n" +
            "    sugo.hash = window.location.hash;\n" +
            "    sugo.hash = sugo.hash.indexOf('?') < 0 ? sugo.hash : sugo.hash.substring(0, sugo.hash.indexOf('?'));\n" +
            "    sugo.relative_path += sugo.hash;\n" +
            "    sugo.relative_path = sugo.relative_path.replace('#/', '#');\n" +
            "    sugo.relative_path = sugo.relative_path.replace('//', '/');\n" +
            "    sugo.init = {\n" +
            "        \"code\": \"$sugo_init_code$\",\n" +
            "        \"page_name\": \"$sugo_init_page_name$\",\n" +
            "        \"page_category\": \"$sugo_init_page_category$\"\n" +
            "    };\n" +
            "    sugo.view_props = {};\n" +
            "\n" +
            "    sugo.rawTrack = function (event_id, event_name, props) {\n" +
            "        if (!props) {\n" +
            "            props = {};\n" +
            "        }\n" +
            "        props.$sugo_path_name$ = sugo.relative_path;\n" +
            "        if (!props.$sugo_page_name$ && sugo.init.page_name) {\n" +
            "            props.$sugo_page_name$ = sugo.init.page_name;\n" +
            "        }\n" +
            "        if (!props.$sugo_page_category_key$ && sugo.init.page_category) {\n" +
            "            props.$sugo_page_category_key$ = sugo.init.page_category;\n" +
            "        }\n" +
            "        window.sugoEventListener.track(event_id, event_name, JSON.stringify(props));\n" +
            "    };\n" +
            "    sugo.track = function (event_name, props) {\n" +
            "        sugo.rawTrack('', event_name, props);\n" +
            "    };\n" +
            "    sugo.timeEvent = function (event_name) {\n" +
            "        window.sugoEventListener.timeEvent(event_name);\n" +
            "    };\n" +
            "\n" +
            "    var sugoio = {\n" +
            "        track: sugo.track,\n" +
            "        time_event: sugo.timeEvent\n" +
            "    };\n" +
            "\n" +
            "    if (sugo.init.code) {\n" +
            "        try {\n" +
            "            var sugo_init_code = new Function('sugo', sugo.init.code);\n" +
            "            sugo_init_code(sugo);\n" +
            "        } catch (e) {\n" +
            "            console.log(sugo.init.code);\n" +
            "        }\n" +
            "    }\n" +
            "    if (sugo.enable_page_event) {\n" +
            "        sugo.track('浏览', sugo.view_props);\n" +
            "    }\n" +
            "    sugo.enter_time = new Date().getTime();\n" +
            "\n" +
            "    if (!window.sugo && sugo.enable_page_event) {\n" +
            "        window.addEventListener('unload', function (e) {\n" +
            "            var tmp_duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "            var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "            tmp_props.duration = tmp_duration;\n" +
            "            sugo.track('停留', tmp_props);\n" +
            "        });\n" +
            "    }\n" +
            "\n" +
            "    sugo.current_page = '$sugo_activity_name$::' + sugo.relative_path;\n" +
            "    sugo.h5_event_bindings = $sugo_h5_event_bindings$;\n" +
            "    sugo.current_event_bindings = {};\n" +
            "    for (var i = 0; i < sugo.h5_event_bindings.length; i++) {\n" +
            "        var b_event = sugo.h5_event_bindings[i];\n" +
            "        if (b_event.target_activity === sugo.current_page || b_event.cross_page) {\n" +
            "            var b_key = JSON.stringify(b_event.path);\n" +
            "            sugo.current_event_bindings[b_key] = b_event;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    sugo.isElementInViewport = function (rect) {\n" +
            "        return ( rect.top >= 0 && rect.left >= 0 && rect.bottom <= sugo.clientHeight && rect.right <= sugo.clientWidth);\n" +
            "    };\n" +
            "\n" +
            "    sugo.handleNodeChild = function (childrens, nodeJSONArray, parent_path) {\n" +
            "        for (var i = 0; i < childrens.length; i++) {\n" +
            "            var nodeChildren = childrens[i];\n" +
            "            var childPath = UTILS.cssPath(nodeChildren);\n" +
            "            var htmlNode = {};\n" +
            "            htmlNode.innerText = nodeChildren.innerText;\n" +
            "            htmlNode.path = childPath;\n" +
            "            htmlNode.classList = nodeChildren.classList;\n" +
            "            var rect = nodeChildren.getBoundingClientRect();\n" +
            "            if (sugo.isElementInViewport(rect) === true) {\n" +
            "                var temp_rect = {\n" +
            "                    top: rect.top,\n" +
            "                    left: rect.left,\n" +
            "                    width: rect.width,\n" +
            "                    height: rect.height\n" +
            "                };\n" +
            "                htmlNode.rect = temp_rect;\n" +
            "                nodeJSONArray.push(htmlNode);\n" +
            "            }\n" +
            "            if (nodeChildren.children) {\n" +
            "                sugo.handleNodeChild(nodeChildren.children, nodeJSONArray, childPath);\n" +
            "            }\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    sugo.reportNodes = function () {\n" +
            "        var nodeJSONArray = [];\n" +
            "        var body = document.getElementsByTagName('body')[0];\n" +
            "        var childrens = body.children;\n" +
            "        var parent_path = '';\n" +
            "        sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "        sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "        sugo.handleNodeChild(childrens, nodeJSONArray, parent_path);\n" +
            "        if (window.sugoWebNodeReporter) {\n" +
            "            window.sugoWebNodeReporter.reportNodes(sugo.relative_path, JSON.stringify(nodeJSONArray), sugo.clientWidth, sugo.clientHeight, document.title);\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    sugo.delegate = function (eventType) {\n" +
            "        function handle(e) {\n" +
            "            var evt = window.event ? window.event : e;\n" +
            "            var target = evt.target || evt.srcElement;\n" +
            "            var currentTarget = e ? e.currentTarget : this;\n" +
            "            var paths = Object.keys(sugo.current_event_bindings);\n" +
            "            for (var idx = 0; idx < paths.length; idx++) {\n" +
            "                var path_str = paths[idx];\n" +
            "                var event = sugo.current_event_bindings[path_str];\n" +
            "                if (event.event_type != eventType) {\n" +
            "                    continue;\n" +
            "                }\n" +
            "                var path = event.path.path;\n" +
            "                if (event.similar === true) {\n" +
            "                    path = event.similar_path ? event.similar_path : path.replace(/:nth-child\\([0-9]*\\)/g, '');\n" +
            "                }\n" +
            "                var eles = document.querySelectorAll(path);\n" +
            "                if (eles) {\n" +
            "                    for (var eles_idx = 0; eles_idx < eles.length; eles_idx++) {\n" +
            "                        var ele = eles[eles_idx];\n" +
            "                        var parentNode = target;\n" +
            "                        while (parentNode) {\n" +
            "                            if (parentNode === ele) {\n" +
            "                                var custom_props = {};\n" +
            "                                if (event.code && event.code.replace(/(^\\s*)|(\\s*$)/g, '') != '') {\n" +
            "                                    try {\n" +
            "                                        var sugo_props = new Function('e', 'element', 'conf', 'instance', event.code);\n" +
            "                                        custom_props = sugo_props(e, ele, event, sugo);\n" +
            "                                    } catch (e) {\n" +
            "                                        console.log(event.code);\n" +
            "                                    }\n" +
            "                                }\n" +
            "                                custom_props.from_binding = true;\n" +
            "                                custom_props.event_type = eventType;\n" +
            "                                custom_props.event_label = ele.innerText;\n" +
            "                                sugo.rawTrack(event.event_id, event.event_name, custom_props);\n" +
            "                                break;\n" +
            "                            }\n" +
            "                            parentNode = parentNode.parentNode;\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        document.addEventListener(eventType, handle, true);\n" +
            "    };\n" +
            "\n" +
            "    sugo.bindEvent = function () {\n" +
            "        sugo.delegate('click');\n" +
            "        sugo.delegate('focus');\n" +
            "        sugo.delegate('change');\n" +
            "    };\n" +
            "\n" +
            "    if (!window.sugo) {\n" +
            "        sugo.bindEvent();\n" +
            "    }\n" +
            "\n" +
            "    sugo.showHeatMap = function () {\n" +
            "        var isShowHeatMap = window.sugoEventListener.isShowHeatMap();\n" +
            "        console.log('isShowHeatMap:' + isShowHeatMap);\n" +
            "        if (!isShowHeatMap) {\n" +
            "            return;\n" +
            "        }\n" +
            "        var pathsOfCurrentEventBindings = Object.keys(sugo.current_event_bindings);\n" +
            "        for (var idx = 0; idx < pathsOfCurrentEventBindings.length; idx++) {\n" +
            "            var hm_path_str = pathsOfCurrentEventBindings[idx];\n" +
            "            var hm_event = sugo.current_event_bindings[hm_path_str];\n" +
            "            var hm_eventId = hm_event.event_id;\n" +
            "            var heatColor = window.sugoEventListener.getEventHeatColor(hm_eventId);\n" +
            "            hm_event.heatColor = heatColor;\n" +
            "        }\n" +
            "\n" +
            "        var idOfHeatMap = 'sugo_heat_map';\n" +
            "        var hm_defaultZIndex = 1000;\n" +
            "        var hmDiv = document.getElementById(idOfHeatMap);\n" +
            "        if (hmDiv) {\n" +
            "            document.body.removeChild(document.getElementById(idOfHeatMap));\n" +
            "        }\n" +
            "        hmDiv = document.createElement('div');\n" +
            "        hmDiv.id = idOfHeatMap;\n" +
            "        hmDiv.style.position = 'absolute';\n" +
            "        hmDiv.style.pointerEvents = 'none';\n" +
            "        hmDiv.style.top = '0px';\n" +
            "        hmDiv.style.left = '0px';\n" +
            "        document.body.appendChild(hmDiv);\n" +
            "        for (var i = 0; i < pathsOfCurrentEventBindings.length; i++) {\n" +
            "            var path_str = pathsOfCurrentEventBindings[i];\n" +
            "            var event = sugo.current_event_bindings[path_str];\n" +
            "            var path = event.path.path;\n" +
            "            var eles = document.querySelectorAll(path);\n" +
            "            if (eles && event.heatColor) {\n" +
            "                var color = event.heatColor;\n" +
            "                var r_color = (color >> 4) & 0x000000ff;\n" +
            "                var g_color = (color >> 2) & 0x000000ff;\n" +
            "                var b_color = color & 0x000000ff;\n" +
            "\n" +
            "                for (var index = 0; index < eles.length; index++) {\n" +
            "                    var div = document.createElement('div');\n" +
            "                    div.id = event.event_id;\n" +
            "                    div.style.position = 'absolute';\n" +
            "                    div.style.pointerEvents = 'none';\n" +
            "                    div.style.opacity = 0.8;\n" +
            "                    var z = eles[index].style.zIndex;\n" +
            "                    div.style.zIndex = z ? parseInt(z) + 1 : hm_defaultZIndex;\n" +
            "                    var rect = eles[index].getBoundingClientRect();\n" +
            "                    div.style.top = rect.top + 'px';\n" +
            "                    div.style.left = rect.left + 'px';\n" +
            "                    div.style.width = rect.width + 'px';\n" +
            "                    div.style.height = rect.height + 'px';\n" +
            "                    div.style.background = `radial-gradient(rgb(${r_color}, ${g_color}, ${b_color}), white)`;\n" +
            "                    hmDiv.appendChild(div);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    if (sugo.showHeatMap) {\n" +
            "        sugo.showHeatMap();\n" +
            "    }\n" +
            "\n" +

            "   sugo.registerPathName = function(){\n"+
            "        var props = {};\n"+
            "        if(sugo.single_code) {\n"+
            "            props.path_name = sugo.relative_path + \"##\" + sugo.single_code;\n"+
            "        } else {\n"+
            "            props.path_name = sugo.relative_path;\n"+
            "        }\n"+
            "        var path_event = {\n"+
            "            'path_name': props.path_name,\n"+
            "        };\n"+
            "        var tmp_props = JSON.parse(JSON.stringify(path_event));\n"+
            "        sugo.track('path_switching', tmp_props);\n"+
            "    };\n"+
            "    sugo.registerPathName();\n"+

            "    window.sugo = sugo;\n" +
            "})(window.sugo || {});\n";
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
        String script = getInjectScript(activity, url);
        view.loadUrl("javascript:" + script);

        SugoWebEventListener.addCurrentWebView(view);
    }

    public static void handlePageFinished(XWalkView view, String url) {
        Context context = view.getContext();
        Activity activity = (Activity) context;
        String script = getInjectScript(activity, url);
        view.load("javascript:" + script, "");

        SugoWebEventListener.addCurrentXWalkView(view);
    }

    public static void handlePageFinished(WebViewDelegate delegate, Activity activity, String url) {
        String script = getInjectScript(activity, url);
        delegate.loadUrl("javascript:" + script);
    }

    public static String getInjectScript(Activity activity, String url) {
        SugoAPI sugoAPI = SugoAPI.getInstance(activity);

        String webRoot = sugoAPI.getConfig().getWebRoot();

        String filePath = activity.getFilesDir().getPath(); // /data/user/0/io.sugo.xxx/files
        String dataPkgPath = filePath.substring(0, filePath.indexOf("/files"));      // /data/user/0/io.sugo.xxx

        JSONObject pageInfo = getPageInfo(activity, url, dataPkgPath);
        String initCode = "";
        String pageName = "";
        String pageCategory = "";
        if (pageInfo != null) {
            initCode = pageInfo.optString("code", "");
            pageName = pageInfo.optString("page_name", "");
            pageCategory = pageInfo.optString("category", "");
        }
        if (!TextUtils.isEmpty(initCode)) {
            initCode = StringEscapeUtils.escapeJava(initCode);
        }

        String activityName = activity.getClass().getName();

        String bindingEvents = getBindingEvents(activity);

        String tempTrackJS = FileUtils.getAssetsFileContent(activity,"Sugo.js",true);
        tempTrackJS = tempTrackJS.replace("$sugo_enable_page_event$", sugoAPI.getConfig().isEnablePageEvent() + "");
        tempTrackJS = tempTrackJS.replace("$sugo_webroot$", webRoot);
        tempTrackJS = tempTrackJS.replace("$sugo_remove_path$", dataPkgPath);
        tempTrackJS = tempTrackJS.replace("$sugo_init_code$", initCode);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_name$", pageName);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_category$", pageCategory);
        tempTrackJS = tempTrackJS.replace("$sugo_activity_name$", activityName);
        tempTrackJS = tempTrackJS.replace("$sugo_path_name$", SGConfig.FIELD_PAGE);
        tempTrackJS = tempTrackJS.replace("$sugo_page_name$", SGConfig.FIELD_PAGE_NAME);
        tempTrackJS = tempTrackJS.replace("$sugo_page_category_key$", SGConfig.FIELD_PAGE_CATEGORY);
        tempTrackJS = tempTrackJS.replace("$sugo_h5_event_bindings$", bindingEvents);

        StringBuffer scriptBuf = new StringBuffer();
        scriptBuf.append(FileUtils.getAssetsFileContent(activity,"SugoCss.js",true));
        scriptBuf.append(tempTrackJS);
        return scriptBuf.toString();
    }

    private static JSONObject getPageInfo(Activity activity, String url, String dataPkgPath) {
        SugoAPI sugoAPI = SugoAPI.getInstance(activity.getApplicationContext());
        String realPath = "";
        try {
            Pattern pattern = Pattern.compile("^[A-Za-z0-9]*://.*", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(url).matches()) {
                URL urlObj = new URL(url);
                realPath = urlObj.getPath();
                realPath = realPath.replaceFirst(dataPkgPath, "");
                String ref = urlObj.getRef();
                if (!TextUtils.isEmpty(ref)) {
                    if (ref.contains("?")) {
                        int qIndex = ref.indexOf("?");
                        ref = ref.substring(0, qIndex);
                    }
                    realPath = realPath + "#" + ref;
                }
                realPath = realPath.replace("#/", "#");
                realPath = realPath.replace("//", "/");
            }
            realPath = realPath.replace(sugoAPI.getConfig().getWebRoot(), "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return SugoPageManager.getInstance().getCurrentPageInfo(realPath);
    }

    private static String getBindingEvents(Activity activity) {
        String token = SugoAPI.getInstance(activity.getApplicationContext()).getConfig().getToken();
        JSONArray eventBindings = SugoWebEventListener.getBindEvents(token);

        if (eventBindings == null) {
            final String sharedPrefsName = ViewCrawler.SHARED_PREF_EDITS_FILE + token;
            SharedPreferences preferences = activity.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
            final String storedBindings = preferences.getString(ViewCrawler.SHARED_PREF_H5_BINDINGS_KEY, null);
            if (storedBindings != null && !storedBindings.equals("")) {
                try {
                    eventBindings = new JSONArray(storedBindings);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (eventBindings == null) {
            eventBindings = new JSONArray();
        }
        return eventBindings.toString();
    }

}
