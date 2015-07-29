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
