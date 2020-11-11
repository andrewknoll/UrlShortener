$(document).ready( function () {

    var safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";



    $("#searchButton").click(function (event) {

        var originalUrl = $('#urlInput').val();

        console.log(`Original url: ${originalUrl}`);
                
                payload = 
                {
                    "client": { 
                       "clientId": "urlshortener-295117", 
                       "clientVersion": "1.5.2" }, 
                    "threatInfo": { 
                       "threatTypes": ["THREAT_TYPE_UNSPECIFIED","MALWARE","SOCIAL_ENGINEERING","UNWANTED_SOFTWARE","POTENTIALLY_HARMFUL_APPLICATION"], 
                       "platformTypes": ["ANY_PLATFORM"], 
                       "threatEntryTypes": ["URL"], 
                       "threatEntries": [ {"url": originalUrl } ] } 
                };


                 // Google safe browsing call
                $.ajax({
                    type: "POST",
                    url: safeBrowsingUrl,
                    data: JSON.stringify(payload),
                    headers: {"Content-Type": "application/json"},
                    dataType: 'json',
                    beforeSend: function () {
                        $("#safeBrowsingCheck").text("Checking URL against Google safe browsing ...");
                    },
                    success:function (output, status, xhr) {

                        console.log(`Output: ${JSON.stringify(output)} and status ${status}`);


                        $("#safeBrowsingCheck").text("This website is verified by google safe browsing ✅");
                    },
                    error: function (params) {
                        alert("Error en peticion");
                    }


                }); 

    });


     



/* 
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/link",
                    data: $(this).serialize(),
                    beforeSend: function () {
                        // ... your initialization code here (so show loader) ...
                      },
                      complete: function () {
                        // ... your finalization code here (hide loader) ...
                      },
                    success: function (msg) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + msg.uri
                            + "'>"
                            + msg.uri
                            + "</a></div>");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                }); 
 */    

    
});