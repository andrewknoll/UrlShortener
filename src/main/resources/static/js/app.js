$(document).ready(function () {

    var safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";

    var shortenedHash = null;

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
                            shortenedHash = msg.hash;
                            $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + msg.uri + "'>" + msg.uri + "</a></div>");
                            $('#urlInput').val('');
                            $("#qrButton").html("<button id='searchButton' class='btn btn-lg btn-primary'>Generate QR Code</button>");
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

    $("#qrButton").click(function (event) {
        /*$.ajax({
            type: "POST",
            url: "/qr",
            data: {
                "hash": shortenedHash
            },
            success: function (msg) {
                var binary = new Uint8Array(msg);
                var blob = new Blob(binary, { 'type': 'image/png' });
                /*var cosa = _arrayBufferToBase64(msg);
                $("#result").html("<div class='alert alert-danger lead'>" + cosa + "</div>");
                


                $("#qrImage").attr('src', `${base64data}`);
            },
            error: function (msg) {
                $("#result").html("<div class='alert alert-danger lead'>ERROR" + msg.status + "</div>");
            }
        });
        */
        var xhr = new XMLHttpRequest();
        xhr.open('get',`${document.URL}/qr/${shortenedHash}`)
           xhr.onload = function(){
               var img = new Image();
               var response = xhr.responseText;
               var binary = new Uint8Array(response.length);
               
               for(var i=0;i<response.length;i++){
                   binary[i] = response.charCodeAt(i) & 0xff;
               }
               
               img.src = URL.createObjectURL(new Blob([binary.buffer]));
               document.body.appendChild(img)
   
           }
           xhr.overrideMimeType('text/plain; charset=x-user-defined');
           xhr.send();
    });

    function _arrayBufferToBase64( rawData ) {
        return btoa(unescape(encodeURIComponent(rawData)));
    }
    // event.preventDefault();

});
