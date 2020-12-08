$(document).ready(function() { // https://mrnxajqdmrmdwkrpenecpvmkozeusfipwpgw-dot-offgl8876899977678.uk.r.appspot.com/xxx

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
            success: function(shortUrl, status, response) {
                $("#safeBrowsingCheck").text("La página será verificada por Google Safe Browsing... ⏳");
                $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + shortUrl + "'>" + shortUrl + "</a></div>");
                $("#notifyShortening").text("");
                $('#urlInput').val('');
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
                url: "/multiplelLinks",
                data: formDataUrls,
                processData: false,
                enctype: 'multipart/form-data',
                contentType: false,
                success: function(csvContent, status, response) {

                    console.log(`Received headers ${
                        response.getResponseHeader('Location')
                    } , status ${
                        response.status
                    } and content: \n ${csvContent}`);

                    $("#safeBrowsingCheck").text("Verificando y acordando urls... ⏳");
                    $("#notifyShortening").text("Success shortening");


                    var encodedUri = encodeURI(csvContent);
                    var link = document.createElement("a");
                    link.setAttribute("href", encodedUri);
                    link.setAttribute("download", "Short-urls.csv");
                    document.body.appendChild(link);
                    if (confirm('¿Quieres descargar el CSV con las urls acortadas?')) {
                        link.click();
                        $("#notifyShortening").text("Archivo descargado ✅");

                    } else {
                        $("#notifyShortening").text("Operación calcelada ❌");
                    }


                },
                error: function(output, status, xhr) {
                    $("#safeBrowsingCheck").text("Sorry got statuscode " + output.status);


                }
            });;
        }


        reader.readAsText(csvFile);

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
       xhr.open('post',`${document.URL}/qr?hash=${shortenedHash}`)
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
