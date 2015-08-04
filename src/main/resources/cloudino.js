//******* DataStores ***************

eng.config={
    devicePort:9595
};

//******* DataStores ***************
eng.dataStores["mongodb"]={
    host:"localhost",
    port:27017,
    class: "org.semanticwb.datamanager.datastore.DataStoreMongo",
    //envhost:"MONGO_PORT_27017_TCP_ADDR",
    //envport:"MONGO_PORT_27017_TCP_PORT",
};

//******* DataSorices ************
eng.dataSources["User"]={
    scls: "User",
    modelid: "Cloudino",
    dataStore: "mongodb",   
    fields:[
        {name:"fullname",title:"Nombre",type:"string"},
        {name:"username",title:"Usuario",type:"string"},
        {name:"password",title:"Contraseña",type:"password"},
        {name:"email",title:"Correo electrónico",type:"string"},
    ],
};

/******* DataProcessors ************/
eng.dataProcessors["UserProcessor"]={
    dataSources: ["User"],
    actions:["fetch","add","update"],
    request: function(request, dataSource, action)
    {
        if(request.data.password)
        {
            request.data.password=this.encodeSHA(request.data.password);
        }
        return request;
    }          
};

//******* DataSorices ************
eng.dataSources["Device"]={
    scls: "Device",
    modelid: "Cloudino",
    dataStore: "mongodb",    
};

//******* DataSorices ************
eng.dataSources["Datasource"]={
    scls: "Datasource",
    modelid: "Cloudino",
    dataStore: "mongodb",    
};

eng.routes={
    loginFallback: "login",
    routeList:[
        { routePath: "login", routeHandler: "io.cloudino.servlet.router.LoginHandler", isRestricted: "false", template: "login" },
        { routePath: "register", routeHandler: "io.cloudino.servlet.router.RegisterHandler", isRestricted: "false", template: "register" },
        { routePath: "panel", routeHandler: "io.cloudino.servlet.router.PanelHandler", isRestricted: "true", template: "panel" },
        { routePath: "", routeHandler: "io.cloudino.servlet.router.ROOTHandler", isRestricted: "false", template: "index"},
        { routePath: "work", isRestricted: "true"},
        { routePath: "panel/device", forwardTo: "/work/device.jsp"},
        { routePath: "panel/content/*", jspMapTo: "/work/content/"},
    ],
};

/******* DataExtractors ************
eng.dataExtractors["SWBSocial1"]={
    dataSource:"SWBSocial",
    class:"org.semanticwb.bigdata.engine.extractors.SWBSocialExtr",
    timer: {time:10,unit:"s"},
    url:"http://swbsocial.infotec.com.mx",
    brand:"infotec",
    stream:"conacyt",
};

eng.dataExtractors["SWBSocial2"]={
    dataSource:"SWBSocial",
    class:"org.semanticwb.bigdata.engine.extractors.SWBSocialExtr",
    
    url:"http://swbsocial.infotec.com.mx",
    brand:"infotec",
    stream:"Sep",
};
*/

/******* DataProcessors ************
eng.dataProcessors["SWBSocialProcessor"]=
{
    dataSources: ["SWBSocial"],
    actions:["add"],
    request: function(request, dataSource, action)
    {
        if(request.data.precio)
        {
            request.data.precio=request.data.precio+1;
        }
        return request;
    }            
}
*/
