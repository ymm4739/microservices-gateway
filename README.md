# gateway
Spring Cloud Gateway 网关，实现动态配置路由和统一鉴权
## 使用方法
本项目集成了github actions，fork本项目后，即可使用github提供的ci/cd，自动打包部署。
前置条件：
1. 远程服务器一台，开启22和8200端口，具有ssh登录用户名和密码，**服务器已安装docker**
2. 点击自己fork的仓库中的Setting，选择Secret，新增4个Secret,分别为DEPLOY_HOST、DEPLOY_USERNAME、DEPLOY_PASSWOR、DEPLOY_PORT，
对应的值分别为远程服务器ip地址、ssh登录用户名、ssh登录密码和ssh端口号即22。
3. 点击仓库中的Action，开启workflow功能，若页面无开启按钮代表已开启
4. 运行1nncore/oauth项目，需要与此项目配合实现统一鉴权
访问nacos配置中心，新建gateway-product.yml文件，文件内容为：
```$xslt
discovery:
  ip: 远程服务器ip 
  weight: 1
```
现在只需往仓库push或者pr，Action会自动执行，可点击Action查看具体运行情况。
## 动态路由
目前网关中并没有任何路由，可动态配置路由。
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
    }],
    "filters":[{
        "name": "StripPrefix",
        "args":{
            "parts": 1
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
    }],
    "filters":[{
        "name": "StripPrefix",
        "args":{
            "parts": 1
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
    }],
    "filters":[{
        "name": "StripPrefix",
        "args":{
            "parts": 1
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
    }],
    "filters":[{
        "name": "StripPrefix",
        "args":{
            "parts": 1
        }
    }]
}]
```
配置后，访问IP:8000/user-center即可访问到user-center服务。
