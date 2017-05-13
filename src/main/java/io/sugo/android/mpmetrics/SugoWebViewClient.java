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
            "    sugo.enable_page_event = true;\n" +
            "\n" +
            "    if (window.sugo && sugo.enable_page_event) {\n" +
            "        const duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "        let tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "        tmp_props.duration = duration;\n" +
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
            "        \"page_name\": \"$sugo_init_page_name$\"\n" +
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
            "            let init_code = new Function('sugo', sugo.init.code);\n" +
            "            init_code(sugo);\n" +
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
            "            const duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "            let tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "            tmp_props.duration = duration;\n" +
            "            sugo.track('停留', tmp_props);\n" +
            "        });\n" +
            "    }\n" +
            "\n" +
            "    sugo.current_page = '$sugo_activity_name$::' + sugo.relative_path;\n" +
            "    sugo.h5_event_bindings = $sugo_h5_event_bindings$;\n" +
            "    sugo.current_event_bindings = {};\n" +
            "    for (let i = 0; i < sugo.h5_event_bindings.length; i++) {\n" +
            "        let b_event = sugo.h5_event_bindings[i];\n" +
            "        if (b_event.target_activity === sugo.current_page) {\n" +
            "            let key = JSON.stringify(b_event.path);\n" +
            "            sugo.current_event_bindings[key] = b_event;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    sugo.isElementInViewport = function (rect) {\n" +
            "        return ( rect.top >= 0 && rect.left >= 0 && rect.bottom <= sugo.clientHeight && rect.right <= sugo.clientWidth);\n" +
            "    };\n" +
            "\n" +
            "    sugo.handleNodeChild = function (childrens, jsonArry, parent_path) {\n" +
            "        for (let i = 0; i < childrens.length; i++) {\n" +
            "            let children = childrens[i];\n" +
            "            let path = UTILS.cssPath(children);\n" +
            "            let htmlNode = {};\n" +
            "            htmlNode.innerText = children.innerText;\n" +
            "            htmlNode.path = path;\n" +
            "            const rect = children.getBoundingClientRect();\n" +
            "            if (sugo.isElementInViewport(rect) == true) {\n" +
            "                let temp_rect = {\n" +
            "                    top: rect.top,\n" +
            "                    left: rect.left,\n" +
            "                    width: rect.width,\n" +
            "                    height: rect.height\n" +
            "                };\n" +
            "                htmlNode.rect = temp_rect;\n" +
            "                jsonArry.push(htmlNode);\n" +
            "            }\n" +
            "            if (children.children) {\n" +
            "                sugo.handleNodeChild(children.children, jsonArry, path);\n" +
            "            }\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    sugo.reportNodes = function () {\n" +
            "        let jsonArry = [];\n" +
            "        let body = document.getElementsByTagName('body')[0];\n" +
            "        let childrens = body.children;\n" +
            "        let parent_path = '';\n" +
            "        sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "        sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "        sugo.handleNodeChild(childrens, jsonArry, parent_path);\n" +
            "        if (window.sugoWebNodeReporter) {\n" +
            "            window.sugoWebNodeReporter.reportNodes(sugo.relative_path, JSON.stringify(jsonArry), sugo.clientWidth, sugo.clientHeight);\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    sugo.delegate = function (eventType) {\n" +
            "        function handle(e) {\n" +
            "            let evt = window.event ? window.event : e;\n" +
            "            let target = evt.target || evt.srcElement;\n" +
            "            let currentTarget = e ? e.currentTarget : this;\n" +
            "            let paths = Object.keys(sugo.current_event_bindings);\n" +
            "            for (let idx = 0; idx < paths.length; idx++) {\n" +
            "                let path_str = paths[idx];\n" +
            "                let event = sugo.current_event_bindings[path_str];\n" +
            "                if (event.event_type != eventType) {\n" +
            "                    continue;\n" +
            "                }\n" +
            "                let path = event.path.path;\n" +
            "                if (event.similar === true) {\n" +
            "                    path = path.replace(/:nth-child\\([0-9]*\\)/g, '');\n" +
            "                }\n" +
            "                let eles = document.querySelectorAll(path);\n" +
            "                if (eles) {\n" +
            "                    for (let eles_idx = 0; eles_idx < eles.length; eles_idx++) {\n" +
            "                        let ele = eles[eles_idx];\n" +
            "                        let parentNode = target;\n" +
            "                        while (parentNode) {\n" +
            "                            if (parentNode === ele) {\n" +
            "                                let custom_props = {};\n" +
            "                                if (event.code && event.code.replace(/(^\\s*)|(\\s*$)/g, '') != '') {\n" +
            "                                    try {\n" +
            "                                        let sugo_props = new Function('e', 'element', 'conf', 'instance', event.code);\n" +
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
            "        let isShowHeatMap = window.sugoEventListener.isShowHeatMap();\n" +
            "        if (!isShowHeatMap) {\n" +
            "            return;\n" +
            "        }\n" +
            "        const pathsOfCurrentEventBindings = Object.keys(sugo.current_event_bindings);\n" +
            "        for (let idx = 0; idx < pathsOfCurrentEventBindings.length; idx++) {\n" +
            "            const path_str = pathsOfCurrentEventBindings[idx];\n" +
            "            let event = sugo.current_event_bindings[path_str];\n" +
            "            let eventId = event.event_id;\n" +
            "            let heatColor = window.sugoEventListener.getEventHeatColor(eventId);\n" +
            "            event.heatColor = heatColor;\n" +
            "        }\n" +
            "\n" +
            "        let idOfHeatMap = 'sugo_heat_map';\n" +
            "        let defaultZIndex = 1000;\n" +
            "        let hmDiv = document.getElementById(idOfHeatMap);\n" +
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
            "        for (let i = 0; i < pathsOfCurrentEventBindings.length; i++) {\n" +
            "            const path_str = pathsOfCurrentEventBindings[i];\n" +
            "            let event = sugo.current_event_bindings[path_str];\n" +
            "            const path = event.path.path;\n" +
            "            let eles = document.querySelectorAll(path);\n" +
            "            if (eles && event.heatColor) {\n" +
            "                let color = event.heatColor;\n" +
            "                let r_color = (color >> 4) & 0x000000ff;\n" +
            "                let g_color = (color >> 2) & 0x000000ff;\n" +
            "                let b_color = color & 0x000000ff;\n" +
            "\n" +
            "                for (let index = 0; index < eles.length; index++) {\n" +
            "                    let div = document.createElement('div');\n" +
            "                    div.id = event.event_id;\n" +
            "                    div.style.position = 'absolute';\n" +
            "                    div.style.pointerEvents = 'none';\n" +
            "                    div.style.opacity = 0.8;\n" +
            "                    let z = eles[index].style.zIndex;\n" +
            "                    div.style.zIndex = z ? parseInt(z) + 1 : defaultZIndex;\n" +
            "                    let rect = eles[index].getBoundingClientRect();\n" +
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
            "    window.sugo = sugo;\n" +
            "    window.sugoio = sugoio;\n" +
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
        if (pageInfo != null) {
            initCode = pageInfo.optString("code", "");
            pageName = pageInfo.optString("page_name", "");
        }

        String activityName = activity.getClass().getName();

        String bindingEvents = getBindingEvents(activity);

        String tempTrackJS = trackJS;
        tempTrackJS = tempTrackJS.replace("$sugo_webroot$", webRoot);
        tempTrackJS = tempTrackJS.replace("$sugo_remove_path$", dataPkgPath);
        tempTrackJS = tempTrackJS.replace("$sugo_init_code$", initCode);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_name$", pageName);
        tempTrackJS = tempTrackJS.replace("$sugo_activity_name$", activityName);
        tempTrackJS = tempTrackJS.replace("$sugo_path_name$", SGConfig.FIELD_PAGE);
        tempTrackJS = tempTrackJS.replace("$sugo_page_name$", SGConfig.FIELD_PAGE_NAME);
        tempTrackJS = tempTrackJS.replace("$sugo_h5_event_bindings$", bindingEvents);

        StringBuffer scriptBuf = new StringBuffer();
        scriptBuf.append(cssUtil);
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
