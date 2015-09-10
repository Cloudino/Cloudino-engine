//******* DataStores ***************

eng.config={
    devicePort:9595,
    arduinoPath:"/Applications/Arduino.app/Contents/Java",
    arduinoLib:"/Applications/Arduino.app/Contents/Java",
    usersWorkPath:"/cloudino/users",
    smtptransport: "smtps", //smtp, smtps
    ssltrust: "*",
    starttls: "true",
    smtpHost: "mail.cloudino.sergiomartinez.mx",
    smtpPort:465,
    smtpUser: "cloudino",
    smtpPassword: "********",
    fromEmail: "cloudino@mail.cloudino.sergiomartinez.mx",
    fromName: "Cloudino Admin",
};

//******* DataStores ***************
eng.dataStores["mongodb"]={
    host:"localhost",
    port:27017,
    class: "org.semanticwb.datamanager.datastore.DataStoreMongo",
    //envhost:"MONGO_PORT_27017_TCP_ADDR",
    //envport:"MONGO_PORT_27017_TCP_PORT",
};

eng.routes={
    loginFallback: "login",
    routeList:[
        { routePath: "login", routeHandler: "io.cloudino.servlet.router.LoginHandler", isRestricted: "false", template: "login" },
        { routePath: "register", routeHandler: "io.cloudino.servlet.router.RegisterHandler", isRestricted: "false", template: "register" },
        //{ routePath: "panel", routeHandler: "io.cloudino.servlet.router.PanelHandler", isRestricted: "true", template: "panel" },
        { routePath: "", routeHandler: "io.cloudino.servlet.router.ROOTHandler", isRestricted: "false", template: "index"},
        { routePath: "work", isRestricted: "true"},
        { routePath: "panel", forwardTo: "/work/panel/index.jsp", isRestricted: "true" },
        { routePath: "panel/*", jspMapTo: "/work/panel/", isRestricted: "true"},
        { routePath: "profile", routeHandler: "io.cloudino.servlet.router.ProfileHandler", isRestricted: "true", template: "profile" },
        { routePath: "photo", routeHandler: "io.cloudino.servlet.router.PhotoHandler", isRestricted: "false" },
        { routePath: "validator/*", routeHandler: "io.cloudino.servlet.router.ValidatorHandler", isRestricted: "false" },
    ],
};

//******* DataSorices ************
eng.dataSources["User"]={
    scls: "User",
    modelid: "Cloudino",
    dataStore: "mongodb",   
    fields:[
        {name:"fullname",title:"Nombre",type:"string"},
        //{name:"username",title:"Usuario",type:"string"},
        {name:"email",title:"Correo electrónico",type:"string"},
        {name:"password",title:"Contraseña",type:"password"},
    ],
};

//******* DataSources ************
eng.dataSources["Device"]={
    scls: "Device",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    fields:[
        {name:"name",title:"Name",type:"string"},
        //{name:"username",title:"Usuario",type:"string"},
        {name:"description",title:"Description",type:"string"},
        {name:"type",title:"Type",type:"string"},
        {name:"user",title:"User",type:"string"},
        {name:"sketcher",title:"Sketcher",type:"string"},
    ],    
};

//******* DataSorices ************
eng.dataSources["Datasource"]={
    scls: "Datasource",
    modelid: "Cloudino",
    dataStore: "mongodb",    
};

eng.dataSources["Control"]={
    scls: "Control",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"title",title:"Title",type:"string"},
        {name:"device",title:"Device",type:"string"},
        {name:"user",title:"User",type:"string"},
        {name:"type",title:"Type",type:"string"},
        {name:"data",title:"Data",type:"object", 
            fields:[
                {name:"topic",title:"Topic",type:"string"},
                {name:"msg",title:"Message",type:"string"},
            ]
        },
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
