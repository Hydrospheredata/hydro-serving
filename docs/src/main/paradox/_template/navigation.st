<div class="nav-home">
    <a href="$page.home.href$" $if(page.home.active)$class="active"$endif$>
        <span class="home-icon">⌂</span>$page.home.title$
    </a>
     <select class="version-selector version-number">
    </select>
</div>
$groups()$
<div class="nav-toc">
    $page.navigation$
</div>