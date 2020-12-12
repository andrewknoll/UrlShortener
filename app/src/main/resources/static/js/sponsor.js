const timeoutLim = 10000;        //Timeout limit in ms
let sponsorSec = 9;
const hash = window.location.pathname;

const y = setInterval(function(){
    document.getElementById("counter").innerHTML = "You are going to be redirected to the page in " + sponsorSec + " s";
    sponsorSec = sponsorSec - 1;
}, 1000);

function getFinalURI() {
    $.ajax({
        type: "GET",
        url: "/redirect" + hash, // returns correct URI
        data: $(this).serialize(),
        success: function (data) {
            window.open(data.URI, "_self")
        }
    })
};

function redirectTo(){
    clearInterval(y);
    getFinalURI();
}

setTimeout(redirectTo, timeoutLim);

