# gateway
Spring Cloud Gateway 网关，实现动态配置路由和Spring Security Oauth2 + JWT鉴权
## 动态路由
目前网关中并没有任何路由，可在nacos配置中心中动态配置路由。
在nacos配置中心，新建gateway-router.yml文件，文件格式选择json，文件内容为：
```$xslt
[{
    "id": "oauth",
    "uri": "lb://oauth",
    "predicates":[{
        "name": "Path",
        "args": {
            "pattern": "/oauth/**"
        }
    }]
},{
    "id": "user-center",
    "uri": "lb://user-center",
    "predicates":[{
        "name": "Path",
        "args": {
            "pattern": "/user-center/**"
        }
    }]
},{
    "id": "client",
    "uri": "lb://oauth-client",
    "predicates":[{
        "name": "Path",
        "args": {
            "pattern": "/client/**"
        }
    }]
},{
    "id": "test",
    "uri": "lb://test",
    "predicates":[{
        "name": "Path",
        "args": {
            "pattern": "/test/**"
        }
    }]
}]
```
#JWT鉴权
白名单以外的每个请求都会从header中获取token，解析token中携带的`uid`和`authorities`，然后对比当前请求的url是否与authorities匹配，
若匹配则路由到实际请求的服务，否则直接返回401。
白名单是通过spring.security.ignore.urls配置的。token是由microservices-oauth项目的oauth服务生成的。
