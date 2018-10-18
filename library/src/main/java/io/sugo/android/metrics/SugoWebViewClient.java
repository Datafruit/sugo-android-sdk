package io.sugo.android.metrics;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import io.sugo.android.util.JSONUtils;
import io.sugo.android.util.StringEscapeUtils;
import io.sugo.android.viewcrawler.ViewCrawler;

/**
 *
 * @author fengxj
 * @date 10/31/16
 */

public class SugoWebViewClient extends WebViewClient {

    private static final String cssUtil = "var sugoioKit=function(e){function t(n){if(r[n])return r[n].exports;var i=r[n]={i:n,l:!1,exports:{}};return e[n].call(i.exports,i,i.exports,t),i.l=!0,i.exports}var r={};return t.m=e,t.c=r,t.d=function(e,r,n){t.o(e,r)||Object.defineProperty(e,r,{configurable:!1,enumerable:!0,get:n})},t.n=function(e){var r=e&&e.__esModule?function(){return e[\"default\"]}:function(){return e};return t.d(r,\"a\",r),r},t.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},t.p=\"\",t(t.s=0)}([function(e,t,r){\"use strict\";function n(e,t,r,n){if(e.nodeType!==N.ELEMENT_NODE)return null;var i=e.getAttribute(\"id\");if(t&&i&&!n)return new s({value:a.idSelector(i),optimized:!0});var o=e.nodeName.toLowerCase();return\"body\"===o||\"head\"===o||\"html\"===o?new s({value:e.nodeName.toLowerCase(),optimized:!0}):function(e,t,r){if(e.nodeType!==N.ELEMENT_NODE)return null;var n=e.getAttribute(\"id\");if(t&&n)return new s({value:e.nodeName.toLowerCase()+a.idSelector(n),optimized:!0});var i=e.parentNode,o=e.nodeName.toLowerCase();if(!i||i.nodeType===N.DOCUMENT_NODE)return new s({value:o.toLowerCase(),optimized:!0});var u=a.prefixedElementClassNames(e),f=!1,d=!1,E=-1,l=i.children,c=0,O=[];if(t)for(c=0;(-1===E||!d)&&c<l.length;++c){var p=l[c];if(p!==e){if(!d&&p.nodeName.toLowerCase()===o.toLowerCase()){f=!0,O=u;for(var T=0,_=0;_<O.length;_++)++T;if(0!==T)for(var C=a.prefixedElementClassNames(p),v=0;v<C.length;++v){var I=C[v],D=O.indexOf(I);if(-1!==D&&(O.splice(D,1),!--T)){d=!0;break}}else d=!0}}else E=c}else if(l.length>1)for(c=0;c<l.length;c++)if(l[c]===e){E=c,d=!0;break}var h=o.toLowerCase();if(r&&\"input\"===o.toLowerCase()&&e.getAttribute(\"type\")&&!e.getAttribute(\"id\")&&!e.getAttribute(\"class\")&&(h+='[type=\"'+e.getAttribute(\"type\")+'\"]'),d)h+=\":nth-child(\"+(E+1)+\")\";else if(f)for(var m=0;m<O.length;m++)h+=\".\"+a.escapeIdentifierIfNeeded(O[m].substr(1));return new s({value:h,optimized:!1})}(e,t,r)}function i(e,t,r){if(e.nodeType!==N.ELEMENT_NODE)return\"\";for(var i=[],o=e;o;){var u=n(o,t,o===e,r);if(!u)break;if(i.push(u),u.optimized)break;o=o.parentNode}return i.reverse().join(\" > \")}function o(e){return i(e,!0,!0)}function u(e){return i(e,!1,!1)}Object.defineProperty(t,\"__esModule\",{value:!0});var a=r(1),f={ELEMENT_NODE:1,ATTRIBUTE_NODE:2,TEXT_NODE:3,CDATA_SECTION_NODE:4,ENTITY_REFERENCE_NODE:5,ENTITY_NODE:6,PROCESSING_INSTRUCTION_NODE:7,COMMENT_NODE:8,DOCUMENT_NODE:9,DOCUMENT_TYPE_NODE:10,DOCUMENT_FRAGMENT_NODE:11,NOTATION_NODE:12,DOCUMENT_POSITION_DISCONNECTED:1,DOCUMENT_POSITION_PRECEDING:2,DOCUMENT_POSITION_FOLLOWING:4,DOCUMENT_POSITION_CONTAINS:8,DOCUMENT_POSITION_CONTAINED_BY:16,DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC:32},N=a.isBrowser()?window.Node||f:f,s=a.DOMNodePathStep;t.optimized=o,t.entire=u,t.cssPath=function(e){var t=o(e);return function(e,t){return(void 0||document).querySelectorAll(e)}(t).length>1&&(t=u(e)),t}},function(e,t,r){\"use strict\";function n(e){var t=e.charCodeAt(0).toString(16);return 1===t.length&&(t=\"0\"+t),t}function i(e){return!!/[a-zA-Z0-9_-]/.test(e)||e.charCodeAt(0)>=160}function o(e){return/^-?[a-zA-Z_][a-zA-Z0-9_-]*$/.test(e)}function u(e,t){return\"\\\\\"+n(e)+(t?\"\":\" \")}function a(e){if(o(e))return e;var t=/^(?:[0-9]|-[0-9-]?)/.test(e),r=e.length-1;return e.replace(/./g,function(e,n){return t&&0===n||!i(e)?u(e,n===r):e})}Object.defineProperty(t,\"__esModule\",{value:!0});var f=function(){function e(e){this.value=e.value,this.optimized=e.optimized||!1}return e.prototype.toString=function(){return this.value},e}();t.DOMNodePathStep=f,t.toHexByte=n,t.isCSSIdentChar=i,t.isCSSIdentifier=o,t.escapeAsciiChar=u,t.escapeIdentifierIfNeeded=a,t.idSelector=function(e){return\"#\"+a(e)},t.prefixedElementClassNames=function(e){var t=e.getAttribute(\"class\");if(!t)return[];var r=t.split(/\\s+/g);return(r=r.filter(Boolean)).map(function(e){return\"$\"+e})},t.isBrowser=function(){var e=!1;try{e=\"undefined\"!=typeof window}catch(t){e=!1}return e}}]);";

    private static final String trackJS = "(function (sugo) {\n" +
            "    sugo.enable_page_event = $sugo_enable_page_event$;\n" +
            "\n" +
            "    if (window.sugo && sugo.enable_page_event) {\n" +
            "        var tmp_duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "        var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "        tmp_props.duration = tmp_duration;\n" +
            "        sugo.track('停留', tmp_props);\n" +
            "    }\n" +
            "\n" +
            "    sugo.single_code = '';\n" +
            "    sugo.relative_path = window.location.pathname.replace(/$sugo_webroot$/g, '');\n" +
            "    sugo.relative_path = sugo.relative_path.replace(/$sugo_remove_path$/g, '');\n" +
            "    sugo.hash = window.location.hash;\n" +
            "    sugo.hash = sugo.hash.indexOf('?') < 0 ? sugo.hash : sugo.hash.substring(0, sugo.hash.indexOf('?'));\n" +
            "    sugo.relative_path += sugo.hash;\n" +
            "    sugo.all_page_info = $all_page_info$;\n" +
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
            "if(sugo.single_code) {\n" +
            "          props.$sugo_path_name$ = sugo.relative_path + \"##\" + sugo.single_code;\n" +
            "        } else {\n" +
            "          props.$sugo_path_name$ = sugo.relative_path;\n" +
            "        }\n" +

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
            "        var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "        sugo.track('浏览', tmp_props);\n" +
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
            "" +
            "sugo.handleNodeChild = function(childrens, nodeJSONArray, parent_path) {\n" +
            "        for (var i = 0; i < childrens.length; i++) {\n" +
            "            var nodeChildren = childrens[i];\n" +
            "            var rect = nodeChildren.getBoundingClientRect();\n" +
            "            if (sugo.isElementInViewport(rect) !== true) {\n" +
            "                continue;\n" +
            "            }\n" +
            "            var temp_rect = {\n" +
            "                    top: rect.top,\n" +
            "                    left: rect.left,\n" +
            "                    width: rect.width,\n" +
            "                    height: rect.height\n" +
            "            };\n" +
            "            htmlNode.rect = temp_rect;\n" +
            "            var childPath = UTILS.cssPath(nodeChildren);\n" +
            "            var htmlNode = {};\n" +
            "            htmlNode.innerText = nodeChildren.innerText;\n" +
            "            htmlNode.path = childPath;\n" +
            "            htmlNode.classList = nodeChildren.classList;\n" +
            "            \n" +
            "            nodeJSONArray.push(htmlNode);\n" +
            "            if (nodeChildren.children) {\n" +
            "                sugo.handleNodeChild(nodeChildren.children, nodeJSONArray, childPath);\n" +
            "            }\n" +
            "        }\n" +
            "    };" +
            ""  +
            "    sugo.reportNodes = function () {\n" +
            "        var nodeJSONArray = [];\n" +
            "        var body = document.getElementsByTagName('body')[0];\n" +
            "        var childrens = body.children;\n" +
            "        var parent_path = '';\n" +
            "        sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);\n" +
            "        sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);\n" +
            "        sugo.handleNodeChild(childrens, nodeJSONArray, parent_path);\n" +
            "        if (window.sugoWebNodeReporter) {\n" +
            "            window.sugoWebNodeReporter.reportNodes(sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : ''), JSON.stringify(nodeJSONArray), sugo.clientWidth, sugo.clientHeight, document.title);\n" +
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
            "        sugo.delegate('touchstart');\n" +
            "        sugo.delegate('touchend');\n" +
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
            "                    div.style.background = 'radial-gradient(rgb(${r_color}, ${g_color}, ${b_color}), white)';\n" +
            "                    hmDiv.appendChild(div);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    if (sugo.showHeatMap) {\n" +
            "        sugo.showHeatMap();\n" +
            "    }\n" +
            "sugo.load = function(code,cite) {\n" +
            "        sugo.unLoad();\n" +
            "         if(cite.options && cite.options.openPolicy === 1) {\n" +
            "        if(sugo.appendHistory && sugo.appendHistory.length) {\n" +
            "            sugo.appendHistory.push(code);\n" +
            "        } else {\n" +
            "            sugo.appendHistory = [sugo.single_code, code];\n" +
            "        }\n" +
            "    } else {\n" +
            "        sugo.appendHistory = null;\n" +
            "    }\n" +
            "        sugo.single_code = code;\n" +
            "        sugo.init_path();\n" +
            "        var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "        sugo.track('浏览', tmp_props);\n" +
            "        sugo.enter_time = new Date().getTime();\n" +
            "    }\n" +
            "    ;\n" +
            "    sugo.unLoad = function() {\n" +
            "        if (sugo.single_code && sugo.enter_time) {\n" +
            "            var duration = (new Date().getTime() - sugo.enter_time) / 1000;\n" +
            "            var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));\n" +
            "            tmp_props.duration = duration;\n" +
            "            sugo.track('停留', tmp_props);\n" +
            "            sugo.enter_time = null ;\n" +
            "        }\n" +
            "    };\n" +
            "    \n" +
            "    sugo.unMount = function(code, cite) {\n" +
            "        if(cite.options && cite.options.openPolicy === 1) {\n" +
            "            if(sugo.appendHistory && sugo.appendHistory.length) {\n" +
            "        var hLenght = sugo.appendHistory.findIndex(function(x) { return x === code});\n" +
            "        sugo.appendHistory = sugo.appendHistory.slice(0, hLenght > 0 ? hLenght : 0);\n" +
            "        if (sugo.appendHistory.length > 0) {\n" +
            "            var single_code = sugo.appendHistory[sugo.appendHistory.length - 1];\n" +
            "            sugo.load(single_code, {});\n" +
            "        }\n" +
            "        }\n" +
            "      }\n" +
            "    };\n" +
            "sugo.init_path = function () {\n" +
            "           sugo.current_page = '$sugo_activity_name$::' + sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : '');\n" +
            "           sugo.current_event_bindings = {};\n" +
            "           for (var i = 0; i < sugo.h5_event_bindings.length; i++) {\n" +
            "            var b_event = sugo.h5_event_bindings[i];\n" +
            "            if (b_event.target_activity === sugo.current_page || b_event.cross_page) {\n" +
            "                var b_key = JSON.stringify(b_event.path);\n" +
            "                sugo.current_event_bindings[b_key] = b_event;\n" +
            "            }\n" +
            "          }\n" +
            "          \n" +
            "         var page_obj = sugo.all_page_info[sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : '')];\n" +
            "         if(page_obj){\n" +
            "         sugo.init = {\n" +
            "            \"code\":page_obj.code,\n" +
            "            \"page_name\": page_obj.page_name,\n" +
            "            \"page_category\": page_obj.page_category\n" +
            "        };\n" +
            "        }else{\n" +
            "            sugo.init = {\n" +
            "                \"code\": '',\n" +
            "                \"page_name\": '',\n" +
            "                \"page_category\": ''\n" +
            "            };\n" +
            "        }" +

            "    };" +
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

    public static void handlePageFinished(WebViewDelegate delegate, Activity activity, String url) {
        String script = getInjectScript(activity, url);
        delegate.loadUrl("javascript:" + script);
    }

    public static String getInjectScript(Activity activity, String url) {
        SugoAPI sugoAPI = SugoAPI.getInstance(activity);

        String webRoot = sugoAPI.getConfig().getWebRoot();

        String filePath = activity.getFilesDir().getPath(); // /data/user/0/io.sugo.xxx/files
        String dataPkgPath = filePath.substring(0, filePath.indexOf("/files"));      // /data/user/0/io.sugo.xxx

        String sugoRemovePath = dataPkgPath;

        String esPath = Environment.getExternalStorageDirectory().getPath();
        if (url.contains(esPath)){
            sugoRemovePath = esPath;
        }
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

        String tempTrackJS = trackJS;
        tempTrackJS = tempTrackJS.replace("$sugo_enable_page_event$", sugoAPI.getConfig().isEnablePageEvent() + "");
        tempTrackJS = tempTrackJS.replace("$sugo_webroot$", webRoot);
        tempTrackJS = tempTrackJS.replace("$sugo_remove_path$", sugoRemovePath.replace("/", "\\/"));
        tempTrackJS = tempTrackJS.replace("$sugo_init_code$", initCode);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_name$", pageName);
        tempTrackJS = tempTrackJS.replace("$sugo_init_page_category$", pageCategory);
        tempTrackJS = tempTrackJS.replace("$sugo_activity_name$", activityName);
        tempTrackJS = tempTrackJS.replace("$sugo_path_name$", SGConfig.FIELD_PAGE);
        tempTrackJS = tempTrackJS.replace("$sugo_page_name$", SGConfig.FIELD_PAGE_NAME);
        tempTrackJS = tempTrackJS.replace("$sugo_page_category_key$", SGConfig.FIELD_PAGE_CATEGORY);
        tempTrackJS = tempTrackJS.replace("$sugo_h5_event_bindings$", bindingEvents);
        tempTrackJS = tempTrackJS.replace("$all_page_info$", new JSONObject(getAllPageInfo()).toString());

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
            }
            realPath = realPath.replace(sugoAPI.getConfig().getWebRoot(), "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return SugoPageManager.getInstance().getCurrentPageInfo(realPath);
    }

    private static HashMap<String, JSONObject> getAllPageInfo() {
        return SugoPageManager.getInstance().getPageInfo();
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
