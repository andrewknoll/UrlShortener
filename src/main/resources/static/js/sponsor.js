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
console.log(document.getElementById("urlVal").value());
setTimeout(redirectTo, timeoutLim, document.getElementById("urlVal").value());

$.ajax({
    type: "GET",
    url: "/redirect", // devuelve la URI buena
    data: $(this).serialize(),
    success: function(data) {
        window.open(data, "_self")
    }
});