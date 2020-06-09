function initVersionSelector($, thisVersion, projectUrl) {
    var hostName = this.document.URL.split("://")[1].split("/")[0];
    if (hostName === "localhost") {
        baseUrl = this.document.URL.split("://")[0] + "://" + hostName
    } else{
        baseUrl = projectUrl;
    }
    console.log(baseUrl);
    if (baseUrl && baseUrl !== "") {
        var schemeLessUrl = baseUrl;
        if (baseUrl.startsWith("http://")){ 
            schemeLessUrl = schemeLessUrl.substring(5);
        } else if (baseUrl.startsWith("https://")) {
            schemeLessUrl = schemeLessUrl.substring(6);
        }
        const url = schemeLessUrl + (schemeLessUrl.endsWith("\/") ? "" : "/") + "versions.json";
        const target = $('.version-selector');
        $.get(url)
        .success(function (versionData) {
             populateVersionSelector(target, thisVersion, versionData);
             // Set the version redirect handler
             target.change(function(){
                 const newUrl = baseUrl + "/" + $(this).val();
                 console.debug(newUrl);
                 location.href = newUrl;
             });
         })
        .fail(function() {
            console.error("Can't get versions from "+ url);
            populateFallback(target, thisVersion);
        });
    }
}

function populateVersionSelector(target, thisVersion, versions) {
    if (!Array.isArray(versions) || versions.length === 0) {
        populateFallback(target, thisVersion)
        return false;
    } else {
        versions.forEach(function(item) {
                var domResult = "Nothing here";
                if (item === thisVersion) {
                    domResult = "<option value=\""+ item +"\" selected>"+ item +"</option>";
                } else {
                    domResult = "<option value=\""+ item +"\">"+ item +"</option>";
                }
                target.append(domResult);
            });
        return true;
    }
}

function populateFallback(target, thisVersion) {
    target.append("<option value=\""+ thisVersion +"\" selected>"+ thisVersion +"</option>");
}
