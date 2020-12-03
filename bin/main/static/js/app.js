$(document).ready(function() { // https://mrnxajqdmrmdwkrpenecpvmkozeusfipwpgw-dot-offgl8876899977678.uk.r.appspot.com/xxx


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

});