$(document).ready(function () {

    var safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";


    // https://mrnxajqdmrmdwkrpenecpvmkozeusfipwpgw-dot-offgl8876899977678.uk.r.appspot.com/xxx


    $("#searchButton").click(function (event) {
        $("#safeBrowsingCheck").text("");
        $("#result").html("<div</div>");
        var introducedUrl = $('#urlInput').val();
        if (introducedUrl == "") {
            $("#safeBrowsingCheck").text("Please insert a url !👆🏼");
            return false;
        }

        $.ajax({
            type: "POST",
            url: "/safeBrowsing",
            data: {
                "url": introducedUrl
            },
            success: function (msg) {
                console.log("Success");
                $("#safeBrowsingCheck").text("This website is verified by google safe browsing ✅");
                $("#notifyShortening").text("Shortening url ... ⏳");

                $.ajax({
                    type: "POST",
                    url: "/link",
                    data: {
                        "url": introducedUrl
                    },
                    success: function (msg) {
                        $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='" + msg.uri + "'>" + msg.uri + "</a></div>");
                        $("#notifyShortening").text("");
                        $('#urlInput').val('');
                    },
                    error: function () {
                        $("#result").html("<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            },
            error: function (output, status, xhr) {

                if (output.status == 404) {
                    $("#safeBrowsingCheck").text("Sorry, Google Safe Browsing couldn't be reached 😔");
                } else if (output.status == 400) {
                    $("#safeBrowsingCheck").text("Bad request, please check inserted url 🤨");
                } else {
                    var urlAndType = output.responseText.split(";");
                    $("#safeBrowsingCheck").text(`❌ Be careful! The introduced url is marked by Google Safe browsing as a ${
                        urlAndType[1]
                    } website ❌`);
                    $('#urlInput').val('');

                }

                console.log(`Error en peticion responsetext ${
                    output.responseText
                } and statuscode ${
                    output.status
                }`);

            }
        });


    });


    $("#csvButton").click(function (event) {
        $("#safeBrowsingCheck").text("");
        $("#result").html("<div</div>");
        event.preventDefault();
        var csv = $('#filename');
        var csvFile = csv[0].files[0];
        var ext = csv.val().split(".").pop().toLowerCase();

        if ($.inArray(ext, ["csv"]) === -1) {
            alert('Please upload a CSV file');
            return false;
        }
        if (csvFile != undefined) {
            reader = new FileReader();
            reader.onload = function (e) {
                csvResult = e.target.result.split("\n");
                var urlsString = "";
                csvResult.forEach(elem => {
                    urlsString = urlsString + elem + ",";
                });
                $.ajax({
                    type: "POST",
                    url: "/safeBrowsing",
                    data: {
                        "url": urlsString,
                        "multiple": true
                    },
                    success: function (msg) {
                        console.log("Successfuly checked against google safe browsing");
                        $("#safeBrowsingCheck").text("Thess websites are verified by google safe browsing ✅");
                        $("#notifyShortening").text("Shortening urls ... ⏳");
                        sendMultipleUrls();
                    },
                    error: function (output, status, xhr) {
                        if (output.status == 404) {
                            $("#safeBrowsingCheck").text("Sorry, Google Safe Browsing couldn't be reached 😔");
                        } else if (output.status == 400) {
                            $("#safeBrowsingCheck").text("Bad request, please check inserted url 🤨");
                        } else {
                            var urlsAndTypes = output.responseText.split(";");

                            $("#safeBrowsingCheck").text(`❌ Be careful! The introduced urls ${
                                urlsAndTypes[0]
                            } are marked by Google Safe browsing as a ${
                                urlsAndTypes[1]
                            } website ❌`);
                            $('#urlInput').val('');

                        }
                    }
                });
            }
            // Asyncronous
            reader.readAsText(csvFile);
        }
    });

});


function sendMultipleUrls() {

    var form = $('#csvForm')[0];
    var formDataUrls = new FormData(form);

    $.ajax({
        type: "POST",
        url: "/multiplelLinks",
        data: formDataUrls,
        processData: false,
        enctype: 'multipart/form-data',
        contentType: false,
        success: function (msg) {
            $("#safeBrowsingCheck").text("Thess websites are verified by google safe browsing ✅");
            $("#notifyShortening").text("Shortening urls ... ⏳");
            $("#notifyShortening").text("Success shortening");
            msg.forEach(shortUrl => {
                console.log(`Reply url: ${shortUrl}`)
            });
            let csvContent = "data:text/csv;charset=utf-8," + msg.map(shortUrl => shortUrl).join("\n");
            var encodedUri = encodeURI(csvContent);
            var link = document.createElement("a");
            link.setAttribute("href", encodedUri);
            link.setAttribute("download", "Short-urls.csv");
            document.body.appendChild(link);
            link.click();
            $("#notifyShortening").text("File downloaded ✅");

        },
        error: function (output, status, xhr) {
            $("#safeBrowsingCheck").text("Sorry got statuscode " + output.status);


        }
    });;
}
