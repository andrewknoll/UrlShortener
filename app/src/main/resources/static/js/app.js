let openConnection = false;
let socket;

$(document).ready(function() {

    $("#webSocketConnect").click(function(event) { // Open websocket connection
        socket = new WebSocket('ws://localhost:50000/link');
        socket.addEventListener('open', function(event) {
            openConnection = true;
            $("#wsButtonLabel").text("✅");

        });

        socket.addEventListener('message', function(msg) {
            // msg.data contains the short url
            // WS message reception

            if (msg.data == "Invalid url") {
                $("#result").html("<div class='alert alert-danger lead'> ERROR: " + msg.data + "</div>");
                $('#urlInputWS').val('');
            } else {
                var res = msg.data.split("/");
                // Save hash
                shortenedHash = res[res.length - 1];
                $("#safeBrowsingCheck").text("The website will be verified by Google Safe Browsing");
                $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + msg.data + "'>" + msg.data + "</a></div>");
                $("#notifyShortening").text("");
                $('#urlInputWS').val('');
                $("#qrButton").html("<button id='searchButton' class='btn btn-lg btn-primary'>Generate QR Code</button>");
            }

        });
    })

    $("#searchButtonWS").click(function(event) {
        if (openConnection) {
            var url = $("#urlInputWS").val();
            socket.send(url);
        } else {
            alert("Please, open the connection with the WebSocket first")
        }
    })


    var shortenedHash = null;

    $("#searchButton").click(function(event) {
        $("#safeBrowsingCheck").text("");
        $("#result").html("<div</div>");
        var introducedUrl = $('#urlInput').val();
        if (introducedUrl == "") {
            $("#safeBrowsingCheck").text("Please insert a url !👆🏼");
            return false;
        }

        $.ajax({
            type: "POST",
            url: "/link",
            data: {
                "url": introducedUrl
            },
            success: function(msg) {
                shortenedHash = msg.hash;
                $("#safeBrowsingCheck").text("The website will be verified by Google Safe Browsing");
                $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + msg.uri + "'>" + msg.uri + "</a></div>");
                $("#notifyShortening").text("");
                $('#urlInput').val('');
                $("#qrButton").html("<button id='searchButton' class='btn btn-lg btn-primary'>Generate QR Code</button>");
            },
            error: function(output) { // output.status para statuscode
                $("#result").html("<div class='alert alert-danger lead'> ERROR: " + JSON.parse(output.responseText).error + "</div>");
                $('#urlInput').val('');
            }
        });
    });


    $("#csvButton").click(function(event) {
        event.preventDefault();
        var urlsNum = $("#urlsNum").val();

        $("#safeBrowsingCheck").text("");
        $("#result").html("<div</div>");
        var csv = $('#filename');
        var csvFile = csv[0].files[0];
        var ext = csv.val().split(".").pop().toLowerCase();

        if ($.inArray(ext, ["csv"]) === -1) {
            alert('Please upload a CSV file');
            return false;
        } else {
            var form = $('#csvForm')[0];
            var formDataUrls = new FormData(form);

            $.ajax({
                type: "POST",
                url: "/multipleLinks",
                data: formDataUrls,
                processData: false,
                enctype: 'multipart/form-data',
                contentType: false,
                success: function(csvContent, status, xhr) {

                    $("#safeBrowsingCheck").text("Verifying and shortening urls...");
                    $("#notifyShortening").text("Success shortening");

                    var encodedUri = encodeURI(csvContent);
                    var link = document.createElement("a");
                    link.setAttribute("href", encodedUri);
                    var filename = xhr.getResponseHeader('Content-Disposition').split("filename=")[1];
                    filename = filename.replace(/['"]+/g, '')
                    link.setAttribute("download", filename);
                    document.body.appendChild(link);
                    link.click();
                    $("#notifyShortening").text("File downloaded ✅");

                },
                error: function(output, status, xhr) {
                    $("#safeBrowsingCheck").text("Sorry got statuscode " + output.status);


                }
            });;
        }
        // reader.readAsText(csvFile);

    });

    $("#qrButton").click(function(event) {
        var xhr = new XMLHttpRequest();
        xhr.open('get', `${
            document.URL
        }/qr/${shortenedHash}`)
        xhr.onload = function() {
            var img = new Image();
            var response = xhr.responseText;
            var binary = new Uint8Array(response.length);

            for (var i = 0; i < response.length; i++) {
                binary[i] = response.charCodeAt(i) & 0xff;
            }

            img.src = URL.createObjectURL(new Blob([binary.buffer]));
            document.body.appendChild(img)

        }
        xhr.overrideMimeType('text/plain; charset=x-user-defined');
        xhr.send();
    });

    function _arrayBufferToBase64(rawData) {
        return btoa(unescape(encodeURIComponent(rawData)));
    }
    // event.preventDefault();

});