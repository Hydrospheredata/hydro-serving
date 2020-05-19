function initVersionSelector($, thisVersion, projectUrl) {
    if (projectUrl && projectUrl !== "") {
        var schemeLessUrl = projectUrl;
        if (projectUrl.startsWith("http://")) projectUrl = schemeLessUrl.substring(5);
        else if (projectUrl.startsWith("https://")) projectUrl = schemeLessUrl.substring(6);
        const url = schemeLessUrl + (schemeLessUrl.endsWith("\/") ? "" : "/") + "versions.json";
        const target = $('.version-selector');
        $.get(url)
        .success(function (versionData) {
             populateVersionSelector(target, thisVersion, versionData);
             // Set the version redirect handler
             target.change(function(){
                 const newUrl = projectUrl + "/" + $(this).val();
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
