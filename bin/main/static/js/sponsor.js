const timeoutLim = 10000;        //Timeout limit in ms
var sponsorSec = 9;

function redirectTo(dest){
    clearInterval(y);
    window.location.replace(dest);
}
const y = setInterval(function(){
    document.getElementById("counter").innerHTML = "You are going to be redirected to the page in " + sponsorSec + " s";
    sponsorSec = sponsorSec - 1;
}, 1000);
setTimeout(redirectTo, timeoutLim, "https://www.google.com");

    /*function () {
        $.ajax({
            type: "POST",
            url: "/link",
            data: $(this).serialize(),
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
    });*/