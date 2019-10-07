//******* DataStores ***************
eng.config={
    devicePort:9494,
    //arduinoPath:"/opt/arduino-1.6.5",
    //arduinoLib: "/opt/arduino-1.6.5/lib",
    arduinoPath:"/Applications/Cloudino.app/Contents/Java",
    arduinoLib:"/Applications/Cloudino.app/Contents/Java",
    usersWorkPath:"/cloudino/users",
    
    sms:{
        baseUrl:"https://rest.nexmo.com/sms/json?api_key=**********&api_secret=************&from=Cloudino",
        toParam:"to",
        textParam:"text"
    },

    mail:{
        from:"cloudinomail@gmail.com",
        fromName:"Cloudino Admin",
        host:"smtp.gmail.com",
        user:"email.gmail.com",
        passwd:"************",
        port:465,
        transport: "smtps", //smtp, smtps        
        ssltrust: "*",
        starttls: "true"
    }      
};


//******* DataStores ***************
eng.dataStores["mongodb"]={
    host:"localhost",
    port:27017,
    class: "org.semanticwb.datamanager.datastore.DataStoreMongo",
    //envhost:"MONGO_PORT_27017_TCP_ADDR",
    //envport:"MONGO_PORT_27017_TCP_PORT",
};

eng.routes["cloudino"]={
    loginFallback: "login",
    routeList:[
        { routePath: "login", routeHandler: "io.cloudino.servlet.router.LoginHandler", isRestricted: "false", template: "login" },
        { routePath: "register", routeHandler: "io.cloudino.servlet.router.RegisterHandler", isRestricted: "false", template: "register" },
        { routePath: "passwordRecovery", routeHandler: "io.cloudino.servlet.router.PasswordRecoveryHandler", isRestricted: "false", template: "passwordRecovery" },
        //{ routePath: "panel", routeHandler: "io.cloudino.servlet.router.PanelHandler", isRestricted: "true", template: "panel" },
        { routePath: "", forward: "/index.jsp", isRestricted: "false"},
        { routePath: "work", isRestricted: "true"},
        //{ routePath: "panel", forwardTo: "/work/panel/index.jsp", isRestricted: "true" },
        { routePath: "panel/*", jspMapTo: "/work/panel/", isRestricted: "true"},
        { routePath: "profile", routeHandler: "io.cloudino.servlet.router.ProfileHandler", isRestricted: "true", template: "profile" },
        { routePath: "photo", routeHandler: "io.cloudino.servlet.router.PhotoHandler", isRestricted: "false" },
        { routePath: "validator/*", routeHandler: "io.cloudino.servlet.router.ValidatorHandler", isRestricted: "false" },
        { routePath: "confirm/*", routeHandler: "io.cloudino.servlet.router.ConfirmationHandler", isRestricted: "false", template: "confirm" },
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
        {name:"user",title:"User",type:"string"},                   //index
        {name:"sketcher",title:"Sketcher",type:"string"},
        {name:"jscode",title:"JSCode",type:"string"},
        {name:"public",title:"Public",type:"boolean"},        
        {name:"dataModel",title:"Data Model",type:"object", 
            fields:[
                {name:"topic",title:"Topic",type:"string"},
                {name:"title",title:"Title",type:"string"},
                {name:"type",title:"Type",type:"string"},
                {name:"minValue",title:"Type",type:"number"},
                {name:"maxValue",title:"Type",type:"number"},
            ]
        },        
    ],    
};

//******* DataStreamData ************
eng.dataSources["DeviceData"]={
    scls: "DeviceData",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    fields:[
        {name:"device",title:"Device",type:"string"},
        {name:"timestamp",title:"TimeStamp",type:"date"},           //index -1
        {name:"data",title:"Data",type:"object"},
    ],      
};

//******* DataStream ************
eng.dataSources["DataStream"]={
    scls: "DataStream",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    fields:[
        {name:"user",title:"User",type:"string"},                   //index
        {name:"name",title:"Name",type:"string"},
        {name:"topic",title:"Topic",type:"string"},
        {name:"active",title:"Active",type:"boolean"},        
        {name:"fields",title:"Fields",type:"object", 
            fields:[
                {name:"name",title:"Name",type:"string"},
                {name:"title",title:"Title",type:"string"},
                {name:"type",title:"Type",type:"string"},
                {name:"minValue",title:"Type",type:"number"},
                {name:"maxValue",title:"Type",type:"number"},
            ]
        },
    ],      
};

//******* DataStreamData ************
eng.dataSources["DataStreamData"]={
    scls: "DataStreamData",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    fields:[
        {name:"dataStream",title:"DataStream",type:"string"},       //index
        {name:"timestamp",title:"TimeStamp",type:"date"},           //index -1
        {name:"values",title:"Fields",type:"object"},
    ],      
};

eng.dataSources["Control"]={
    scls: "Control",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"name",title:"Name",type:"string"},
        {name:"device",title:"Device",type:"string"},               //index
        {name:"user",title:"User",type:"string"},                   //index
        {name:"type",title:"Type",type:"string"},
        {name:"data",title:"Data",type:"object", 
            fields:[
                {name:"topic",title:"Topic",type:"string"},
                {name:"msg",title:"Message",type:"string"},
            ]
        },
    ],      
};

eng.dataSources["UserContext"]={
    scls: "UserContext",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"name",title:"Name",type:"string"},
        {name:"description",title:"Description",type:"string"},
        {name:"user",title:"User",type:"string"},                   //index
        {name:"icon",title:"Icon",type:"string"},
    ],      
};

eng.dataSources["DeviceGroup"]={
    scls: "DeviceGroup",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"name",title:"Name",type:"string"},
        {name:"description",title:"Description",type:"string"},
        {name:"user",title:"User",type:"string"},                   //index
        {name:"icon",title:"Icon",type:"string"},
        {name:"devices", title:"Devices", stype:"select", multiple:true, dataSource:"Device"},
    ],      
};

eng.dataSources["CloudRule"]={
    scls: "CloudRule",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"name",title:"Name",type:"string"},
        {name:"description",title:"Description",type:"string"},
        {name:"user",title:"User",type:"string"},                   //index
        {name:"script",title:"Script",type:"string"},
        {name:"xml",title:"XML",type:"string"},
        {name:"state",title:"State",type:"string"},
    ],      
};

eng.dataSources["CloudRuleEvent"]={
    scls: "CloudRuleEvent",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"cloudRule",title:"CloudRule",type:"string"},         //index
        {name:"user",title:"User",type:"string"},                   //index
        {name:"type",title:"Type",type:"string"},
        {name:"context",title:"Context",type:"string"},
        {name:"funct",title:"Function",type:"string"},
        {name:"params",title:"Params",type:"object"},
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
            request.data.password=this.utils.encodeSHA(request.data.password);
        }
        return request;
    }          
};

/******* FileSources ***************/
eng.fileSources["UserPhoto"]={
    scls: "UserPhoto",
    class: "org.semanticwb.datamanager.filestore.FileSourceMongo",
    modelid: "Cloudino",
    dataStore: "mongodb", 
    maxSize: 100000,
    cachableSize: 5120,
};

eng.dataSources["DeviceLinks"]={
    scls: "DeviceLinks",
    modelid: "Cloudino",
    dataStore: "mongodb",    
    //{user:, device:, title, type:"MsgButton", data:{topic, msg}}
    fields:[
        {name:"name",title:"Name",type:"string"},
        {name:"description",title:"Description",type:"string"},
        {name:"user",title:"User",type:"string"},                       //index
        {name:"type",title:"Type",type:"string"},
        {name:"device", title:"Device", stype:"select", multiple:false, dataSource:"Device"},
        {name:"active",title:"Active",type:"boolean"},
        {name:"data",title:"Data",type:"object"},
    ],      
};