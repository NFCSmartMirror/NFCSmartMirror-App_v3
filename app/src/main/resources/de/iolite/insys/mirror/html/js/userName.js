
function loadUserName() {
  ModelAPIProfiles.get(ModelAPIProfiles.storageId, {
      success: function(storageAPI) {
        console.log("Test: get function 1 success");
        storageAPI.action({
          request: new ActionRequest(null, null, ".", "loadString", [new ValueParameter("uName")]),
          success: function(username) {
            console.info('Username: '+username);
              $(document).ready(function() {
                $('#name').attr('value', username);
              });
          },
          error: function(storageAPI, responseRequestID, responseErrorCode, responseError) {
            console.error("Requesting value update of ", device, " failed due to ",
              responseErrorCode, ": ", responseError);
          }
        });

      }
 });


  console.log(form.name.value);
}


function saveUserName(form) {
/*
  ModelAPIProfiles.get(ModelAPIProfiles.storageId, {
      success: function(storageAPI) {
        storageAPI.action({
          request: new ActionRequest(null, null, ".", "saveString", [new ValueParameter("uName"), new ValueParameter(form.name.value)]),
          success: function() {
            console.info('Successfully executed requestValueUpdate');
          },
          error: function(storageAPI, responseRequestID, responseErrorCode, responseError) {
            console.error("Requesting value update of ", device, " failed due to ",
              responseErrorCode, ": ", responseError);
          }
        });
       }
      });
      */
      
	$.get("updateUsername?username="+form.name.value+"&SID="+window.Client.SID);

    console.log(form.name.value);
  }
