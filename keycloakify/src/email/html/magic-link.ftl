<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("magicLinkEmailBody", linkExpiration))?no_esc}

<a href="${link}">${link}</a>

${kcSanitize(msg("magicLinkSecurity"))?no_esc}
</@layout.emailLayout>
