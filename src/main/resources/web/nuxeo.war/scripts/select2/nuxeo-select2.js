
   function initSelect2Widgets() {
     jQuery("input[type='hidden'][id$=select2]").each(function(idx,el) {

         // retrieve parameters from Html
         var elid = el.id;
         var params = {};

         el=jQuery(el);

         var paramId = (elid + "_params").split(":").join("\\:");
         var paramsHolder = jQuery("#" + paramId);
         params=JSON.parse(paramsHolder.val());

         var initId = (elid + "_init").split(":").join("\\:");
         var initHolder = jQuery("#" + initId);
         var initDoc = null;
         try {
           initDoc =JSON.parse(initHolder.val());
         } catch(err) {}

         // set style on select
         el.css("width",params.width + "px");

         // determine operation name
         var opName = params.operationId;
         if(typeof(opName)=='undefined' || opName==''){
           opName = 'Document.PageProvider';
         }

         // init Automation Operation
         var op = jQuery().automation(opName,{"documentSchemas":params.documentSchemas});
         op.addParameter("query",params.query);
         op.addParameter("page","0");
         op.addParameter("pageSize","20");

         // detect if we need custom result formating
         var customFormaterFunction = null;
         if (params.customFormater && params.customFormater.length>0) {
            customFormaterFunction=eval(params.customFormater);
         }

         // build select2 parameters
         var select2_params = {
          minimumInputLength: params.minimumInputLength,
          query: function (query) {
            op.addParameter("queryParams", query.term + "%");
            op.execute(function(data, textStatus,xhr) {
              var results = [];
              for ( i = 0; i < data.entries.length; i++) {
                var doc = data.entries[i];
                results.push(doc);
              }
              query.callback({results : results});
            });
         }}

         // append custom result formater if needed
         if (customFormaterFunction!=null) {
           select2_params.formatResult = customFormaterFunction;
           select2_params.formatSelection = function(doc)
                                    {if (select2_params.labelProperty!=null) {
                                          return doc.properties[select2_params.labelProperty]; }
                                     else { return doc.title;} };
         } else {
           select2_params.formatResult = function(doc) {return doc.title};
           select2_params.formatSelection = function(doc)
                                    {if (select2_params.labelProperty!=null) {
                                          return doc.properties[select2_params.labelProperty]; }
                                     else { return doc.title;} };
         }

         // append id formater if needed
         if (params.idProperty && params.idProperty.length>0) {
            select2_params.id = function(doc) { return doc.properties[params.idProperty]; };
         } else {
            select2_params.id = function(doc) { return doc.uid; }
         }

         if (initDoc!=null) {
           select2_params.initSelection = function (element, callback) {
             callback(initDoc);
           };
         }

         if (params.multiple=='true') {
             select2_params.maximumSelectionSize=params.maximumSelectionSize;
             select2_params.multiple=true;
         }

         // init select2
         el.select2(select2_params);
       });
   };

   jQuery(document).ready(function() {

     jQuery('head').append('<link rel="stylesheet" href="/nuxeo/css/select2.css" type="text/css" />');
     initSelect2Widgets();

   });