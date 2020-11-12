$(document).ready(function () {

    var safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";

    $("#searchButton").click(function (event) {

        var originalUrl = $('#urlInput').val();

        console.log(`Original url: ${originalUrl}`);

        payload = {
            "client": {
                "clientId": "urlshortener-295117",
                "clientVersion": "1.5.2"
            },
            "threatInfo": {
                "threatTypes": [
                    "THREAT_TYPE_UNSPECIFIED",
                    "MALWARE",
                    "SOCIAL_ENGINEERING",
                    "UNWANTED_SOFTWARE",
                    "POTENTIALLY_HARMFUL_APPLICATION"
                ],
                "platformTypes": ["ANY_PLATFORM"],
                "threatEntryTypes": ["URL"],
                "threatEntries": [
                    {
                        "url": originalUrl
                    }
                ]
            }
        };


        // Google safe browsing call
        $.ajax({
            type: "POST",
            url: safeBrowsingUrl,
            data: JSON.stringify(payload),
            headers: {
                "Content-Type": "application/json"
            },
            dataType: 'json',
            beforeSend: function () {
                $("#safeBrowsingCheck").text("Checking URL against Google safe browsing ...");
            },
            success: function (output, status, xhr) {

                console.log(`Output: ${
                    JSON.stringify(output)
                } and status ${status}`);

                if (JSON.stringify(output) == "{}") {
                    $("#safeBrowsingCheck").text("This website is verified by google safe browsing ✅");
                    $("#notifyShortening").text("Shortening url ...");
                    var url = $('#urlInput').val();
                    console.log(`Sending url: ${url} as data`);
                    $.ajax({
                        type: "POST",
                        url: "/link",
                        data: {
                            "url": url
                        },
                        success: function (msg) {
                            $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + msg.uri + "'>" + msg.uri + "</a></div>");
                            $('#urlInput').val('');
                        },
                        error: function () {
                            $("#result").html("<div class='alert alert-danger lead'>ERROR</div>");
                        }
                    });
                } else {
                    $("#safeBrowsingCheck").text("Can't shorten this url because this url marked as unsafe by Google Safe Browsing ❌");
                }


            },
            error: function (params) {
                alert("Error en peticion");
            }


        });

    });

    // event.preventDefault();

});
