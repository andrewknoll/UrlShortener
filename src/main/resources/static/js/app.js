$(document).ready( function () {

    var safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";

    var originalUrl = $('#urlInput').val();

    $("#searchButton").click(function (event) {

        alert("Clicked");
        var googleCheckData = `
                { 
                    client: { 
                        clientId : urlshortener-295117, 
                        clientVersion : 1.5.2 
                    },  
                    threatInfo: { 
                        threatTypes: [ 
                            THREAT_TYPE_UNSPECIFIED, 	
                            MALWARE, 
                            THREAT_TYPE_UNSPECIFIED, 	
                            MALWARE, 
                            SOCIAL_ENGINEERING, 
                            UNWANTED_SOFTWARE, 
                            POTENTIALLY_HARMFUL_APPLICATION  
                        ], 
                        platformTypes: [ 
                            ANY_PLATFORM 
                        ], 
                        threatEntryTypes: [ 
                            URL 
                        ], 
                        threatEntries: [ 
                            {
                                url: ${originalUrl}
                            },
                        ]
                    }
                }`;
                console.log("Ejecutando submit ...");

                var json = JSON.stringify(googleCheckData);
                console.log(`Se enviara ${json}`);

    });


    /* // Google safe browsing call
                $.ajax({
                    type: "POST",
                    url: safeBrowsingUrl,
                    data: JSON.stringify(googleCheckData),
                    beforeSend: function () {
                        $("#safeBrowsingCheck").text("Checking URL against Google safe browsing ...");
                    },


                });




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
                }); */
    

    
});