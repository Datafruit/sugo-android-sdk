# Sugo SDK 之 Weex 扩展
**Weex 支持**    
> Weex 是一套简单易用的跨平台开发方案，能以 web 的开发体验构建高性能、可扩展的 native 应用,   
若要支持 Weex 生成的页面，请额外添加   

```Groovy    
    compile 'io.sugo.android:weex:1.0.0'
```

首先，在原生代码中，注册 Sugo SDK Weex 扩展模块
> 必须先确保 Weex SDK 已正确添加到项目中

```
    WXSDKEngine.registerModule("sugo", WXSugoModule.class);
```

接着，在 JS 中引入模块      

```
  var sugo = weex.requireModule('sugo');
```

最后，调用相关的功能   

1. 记录事件：track(eventName, props)   
    例如：
```
sugo.track("buycart", {"productId","p123456"});
```

2. 记录时常事件：timeEvent(eventName)   
    例如：
```
    sugo.timeEvent("play")      // 开始记录时间
    ...
    ...
    sugo.track("play",{});      // 事件记录
```

3. 超级属性   
```
    sugo.registerSuperProperties(props)     // 注册超级属性
    sugo.registerSuperPropertiesOnce(props) // 注册不可覆盖的超级属性
    sugo.unregisterSuperProperty(propName)  // 取消注册
    sugo.getSuperProperties(callback)       // 获取已有的超级属性
    sugo.clearSuperProperties()             // 清除所有超级属性
```

4. 登录统计：sugo.login(userIdKey, userIdValue)

```
    sugo.login("userId", "123456") // "userId" 是一个自定义的字段
    
    sugo.logout()                  // 退出登录
```

5. web 组件埋点支持（仅安卓需要额外的代码，iOS 将自动支持）

```
    <template>
        <web ref="webview" :src="url" @pagestart="start" @pagefinish="finish" @error="error"></web>
    </template>
    <script>
        export default {
            mounted() {
                sugo.initWebView(this.$refs.webview);
            },
            methods: {
                finish() {
                    sugo.handleWebView(url, this.$refs.webview);
                }
            }
        }
    </script>
```
