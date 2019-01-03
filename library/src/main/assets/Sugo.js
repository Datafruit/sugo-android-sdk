(function (sugo) {
               sugo.enable_page_event = $sugo_enable_page_event$;

               if (window.sugo && sugo.enable_page_event) {
                   var tmp_duration = (new Date().getTime() - sugo.enter_time) / 1000;
                   var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));
                   tmp_props.duration = tmp_duration;
                   sugo.track('停留', tmp_props);
               }

               sugo.single_code = '';
               sugo.relative_path = window.location.pathname.replace(/$sugo_webroot$/g, '');
               sugo.relative_path = sugo.relative_path.replace(/$sugo_remove_path$/g, '');
               sugo.hash = window.location.hash;
               sugo.hash = sugo.hash.indexOf('?') < 0 ? sugo.hash : sugo.hash.substring(0, sugo.hash.indexOf('?'));
               sugo.relative_path += sugo.hash;
               sugo.all_page_info = $all_page_info$;
               sugo.init = {
                    "code ":  "$sugo_init_code$ ",
                    "page_name ":  "$sugo_init_page_name$ ",
                    "page_category ":  "$sugo_init_page_category$ "
               };
               sugo.view_props = {};

               sugo.rawTrack = function (event_id, event_name, props) {
                   if (!props) {
                       props = {};
                   }
            if(sugo.single_code) {
                     props.$sugo_path_name$ = sugo.relative_path +  "## " + sugo.single_code;
                   } else {
                     props.$sugo_path_name$ = sugo.relative_path;
                   }

                   if (!props.$sugo_page_name$ && sugo.init.page_name) {
                       props.$sugo_page_name$ = sugo.init.page_name;
                   }
                   if (!props.$sugo_page_category_key$ && sugo.init.page_category) {
                       props.$sugo_page_category_key$ = sugo.init.page_category;
                   }
                   window.sugoEventListener.track(event_id, event_name, JSON.stringify(props));
               };
               sugo.track = function (event_name, props) {
                   sugo.rawTrack('', event_name, props);
               };
               sugo.timeEvent = function (event_name) {
                   window.sugoEventListener.timeEvent(event_name);
               };

               var sugoio = {
                   track: sugo.track,
                   time_event: sugo.timeEvent
               };

               if (sugo.init.code) {
                   try {
                       var sugo_init_code = new Function('sugo', sugo.init.code);
                       sugo_init_code(sugo);
                   } catch (e) {
                       console.log(sugo.init.code);
                   }
               }
               if (sugo.enable_page_event) {
                   var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));
                   sugo.track('浏览', tmp_props);
               }
               sugo.enter_time = new Date().getTime();

               if (!window.sugo && sugo.enable_page_event) {
                   window.addEventListener('unload', function (e) {
                       var tmp_duration = (new Date().getTime() - sugo.enter_time) / 1000;
                       var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));
                       tmp_props.duration = tmp_duration;
                       sugo.track('停留', tmp_props);
                   });
               }

               sugo.current_page = '$sugo_activity_name$::' + sugo.relative_path;
               sugo.h5_event_bindings = $sugo_h5_event_bindings$;
               sugo.current_event_bindings = {};
               for (var i = 0; i < sugo.h5_event_bindings.length; i++) {
                   var b_event = sugo.h5_event_bindings[i];
                   if (b_event.target_activity === sugo.current_page || b_event.cross_page) {
                       var b_key = JSON.stringify(b_event.path);
                       sugo.current_event_bindings[b_key] = b_event;
                   }
               }

               sugo.isElementInViewport = function (rect) {
                   return ( rect.top >= 0 && rect.left >= 0 && rect.bottom <= sugo.clientHeight && rect.right <= sugo.clientWidth);
               };

               sugo.handleNodeChild = function (childrens, nodeJSONArray, parent_path) {
                   for (var i = 0; i < childrens.length; i++) {
                       var nodeChildren = childrens[i];
                       var childPath = sugoioKit.cssPath(nodeChildren);
                       var htmlNode = {};
                       htmlNode.innerText = nodeChildren.innerText;
                       htmlNode.path = childPath;
                       htmlNode.classList = nodeChildren.classList;
                       var rect = nodeChildren.getBoundingClientRect();
                       if (sugo.isElementInViewport(rect) === true) {
                           var temp_rect = {
                               top: rect.top,
                               left: rect.left,
                               width: rect.width,
                               height: rect.height
                           };
                           htmlNode.rect = temp_rect;
                           nodeJSONArray.push(htmlNode);
                       }
                       if (nodeChildren.children) {
                           sugo.handleNodeChild(nodeChildren.children, nodeJSONArray, childPath);
                       }
                   }
               };

               sugo.reportNodes = function () {
                   var nodeJSONArray = [];
                   var body = document.getElementsByTagName('body')[0];
                   var childrens = body.children;
                   var parent_path = '';
                   sugo.clientWidth = (window.innerWidth || document.documentElement.clientWidth);
                   sugo.clientHeight = (window.innerHeight || document.documentElement.clientHeight);
                   sugo.handleNodeChild(childrens, nodeJSONArray, parent_path);
                   if (window.sugoWebNodeReporter) {
                       window.sugoWebNodeReporter.reportNodes(sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : ''), JSON.stringify(nodeJSONArray), sugo.clientWidth, sugo.clientHeight, document.title);
                   }
               };

               sugo.delegate = function (eventType) {
                   function handle(e) {
                       var evt = window.event ? window.event : e;
                       var target = evt.target || evt.srcElement;
                       var currentTarget = e ? e.currentTarget : this;
                       var paths = Object.keys(sugo.current_event_bindings);
                       for (var idx = 0; idx < paths.length; idx++) {
                           var path_str = paths[idx];
                           var event = sugo.current_event_bindings[path_str];
                           if (event.event_type != eventType) {
                               continue;
                           }
                           var path = event.path.path;
                           if (event.similar === true) {
                               path = event.similar_path ? event.similar_path : path.replace(/:nth-child\\([0-9]*\\)/g, '');
                           }
                           var eles = document.querySelectorAll(path);
                           if (eles) {
                               for (var eles_idx = 0; eles_idx < eles.length; eles_idx++) {
                                   var ele = eles[eles_idx];
                                   var parentNode = target;
                                   while (parentNode) {
                                       if (parentNode === ele) {
                                           var custom_props = {};
                                           if (event.code && event.code.replace(/(^\\s*)|(\\s*$)/g, '') != '') {
                                               try {
                                                   var sugo_props = new Function('e', 'element', 'conf', 'instance', event.code);
                                                   custom_props = sugo_props(e, ele, event, sugo);
                                               } catch (e) {
                                                   console.log(event.code);
                                               }
                                           }
                                           custom_props.from_binding = true;
                                           custom_props.event_type = eventType;
                                           custom_props.event_label = ele.innerText;
                                           sugo.rawTrack(event.event_id, event.event_name, custom_props);
                                           break;
                                       }
                                       parentNode = parentNode.parentNode;
                                   }
                               }
                           }
                       }
                   }

                   document.addEventListener(eventType, handle, true);
               };

               sugo.bindEvent = function () {
                   sugo.delegate('click');
                   sugo.delegate('focus');
                   sugo.delegate('change');
                   sugo.delegate('touchstart');
                   sugo.delegate('touchend');
               };

               if (!window.sugo) {
                   sugo.bindEvent();
               }

               sugo.showHeatMap = function () {
                   var isShowHeatMap = window.sugoEventListener.isShowHeatMap();
                   console.log('isShowHeatMap:' + isShowHeatMap);
                   if (!isShowHeatMap) {
                       return;
                   }
                   var pathsOfCurrentEventBindings = Object.keys(sugo.current_event_bindings);
                   for (var idx = 0; idx < pathsOfCurrentEventBindings.length; idx++) {
                       var hm_path_str = pathsOfCurrentEventBindings[idx];
                       var hm_event = sugo.current_event_bindings[hm_path_str];
                       var hm_eventId = hm_event.event_id;
                       var heatColor = window.sugoEventListener.getEventHeatColor(hm_eventId);
                       hm_event.heatColor = heatColor;
                   }

                   var idOfHeatMap = 'sugo_heat_map';
                   var hm_defaultZIndex = 1000;
                   var hmDiv = document.getElementById(idOfHeatMap);
                   if (hmDiv) {
                       document.body.removeChild(document.getElementById(idOfHeatMap));
                   }
                   hmDiv = document.createElement('div');
                   hmDiv.id = idOfHeatMap;
                   hmDiv.style.position = 'absolute';
                   hmDiv.style.pointerEvents = 'none';
                   hmDiv.style.top = '0px';
                   hmDiv.style.left = '0px';
                   document.body.appendChild(hmDiv);
                   for (var i = 0; i < pathsOfCurrentEventBindings.length; i++) {
                       var path_str = pathsOfCurrentEventBindings[i];
                       var event = sugo.current_event_bindings[path_str];
                       var path = event.path.path;
                       var eles = document.querySelectorAll(path);
                       if (eles && event.heatColor) {
                           var color = event.heatColor;
                           var r_color = (color >> 4) & 0x000000ff;
                           var g_color = (color >> 2) & 0x000000ff;
                           var b_color = color & 0x000000ff;

                           for (var index = 0; index < eles.length; index++) {
                               var div = document.createElement('div');
                               div.id = event.event_id;
                               div.style.position = 'absolute';
                               div.style.pointerEvents = 'none';
                               div.style.opacity = 0.8;
                               var z = eles[index].style.zIndex;
                               div.style.zIndex = z ? parseInt(z) + 1 : hm_defaultZIndex;
                               var rect = eles[index].getBoundingClientRect();
                               div.style.top = rect.top + 'px';
                               div.style.left = rect.left + 'px';
                               div.style.width = rect.width + 'px';
                               div.style.height = rect.height + 'px';
                               div.style.background = 'radial-gradient(rgb(${r_color}, ${g_color}, ${b_color}), white)';
                               hmDiv.appendChild(div);
                           }
                       }
                   }
               };

               if (sugo.showHeatMap) {
                   sugo.showHeatMap();
               }
            sugo.load = function(code,cite) {
                   sugo.unLoad();
                    if(cite.options && cite.options.openPolicy === 1) {
                   if(sugo.appendHistory && sugo.appendHistory.length) {
                       sugo.appendHistory.push(code);
                   } else {
                       sugo.appendHistory = [sugo.single_code, code];
                   }
               } else {
                   sugo.appendHistory = null;
               }
                   sugo.single_code = code;
                   sugo.init_path();
                   var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));
                   sugo.track('浏览', tmp_props);
                   sugo.enter_time = new Date().getTime();
               }
               ;
               sugo.unLoad = function() {
                   if (sugo.single_code && sugo.enter_time) {
                       var duration = (new Date().getTime() - sugo.enter_time) / 1000;
                       var tmp_props = JSON.parse(JSON.stringify(sugo.view_props));
                       tmp_props.duration = duration;
                       sugo.track('停留', tmp_props);
                       sugo.enter_time = null ;
                   }
               };

               sugo.unMount = function(code, cite) {
                   if(cite.options && cite.options.openPolicy === 1) {
                       if(sugo.appendHistory && sugo.appendHistory.length) {
                   var hLenght = sugo.appendHistory.findIndex(function(x) { return x === code});
                   sugo.appendHistory = sugo.appendHistory.slice(0, hLenght > 0 ? hLenght : 0);
                   if (sugo.appendHistory.length > 0) {
                       var single_code = sugo.appendHistory[sugo.appendHistory.length - 1];
                       sugo.load(single_code, {});
                   }
                   }
                 }
               };
            sugo.init_path = function () {
                      sugo.current_page = '$sugo_activity_name$::' + sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : '');
                      sugo.current_event_bindings = {};
                      for (var i = 0; i < sugo.h5_event_bindings.length; i++) {
                       var b_event = sugo.h5_event_bindings[i];
                       if (b_event.target_activity === sugo.current_page || b_event.cross_page) {
                           var b_key = JSON.stringify(b_event.path);
                           sugo.current_event_bindings[b_key] = b_event;
                       }
                     }

                    var page_obj = sugo.all_page_info[sugo.relative_path + (sugo.single_code ? '##' + sugo.single_code : '')];
                    if(page_obj){
                    sugo.init = {
                        "code ":page_obj.code,
                        "page_name ": page_obj.page_name,
                        "page_category ": page_obj.page_category
                   };
                   }else{
                       sugo.init = {
                            "code ": '',
                            "page_name ": '',
                            "page_category ": ''
                       };
                   }

               };



               window.sugo = sugo;
            })(window.sugo || {});;