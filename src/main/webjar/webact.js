/* Webact JS */

var elmApp = Elm.Main.init({
    node: document.getElementById("webact-app"),
    flags: elmFlags
});

elmApp.ports.initElements.subscribe(function() {
    console.log("Initialsing elements …");
    $('.ui.dropdown').dropdown();
    $('.ui.checkbox').checkbox();
    $('.ui.accordion').accordion();
});
