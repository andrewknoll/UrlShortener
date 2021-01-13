const hash = window.location.pathname;
const eventURL = "http://localhost:8080/redirect" + hash;
let eventSource = new EventSource(eventURL);

eventSource.onopen = function(){
    console.log('Connection is live!');
};

eventSource.onmessage = function(event) {
    const finalURI = event.data;
    console.log(finalURI);
    window.open(finalURI, "_self");
};

eventSource.onerror = function(event) {
    if(event.readyState == EventSource.CLOSED){
        console.log('Connection is closed.');
    }
    else{
        console.log('Error occurred', event);
    }
    event.target.close();
};


const MS = 1000;
const timeoutLim = 5;        //Timeout limit in ms
let sponsorSec = timeoutLim-1;
const y = setInterval(function(){
    document.getElementById("counter").innerHTML = "You are going to be redirected to the page in " + sponsorSec + " s";
    sponsorSec = sponsorSec - 1;
}, MS);

function redirectTo(){
    clearInterval(y);
}

setTimeout(redirectTo, timeoutLim*MS);

