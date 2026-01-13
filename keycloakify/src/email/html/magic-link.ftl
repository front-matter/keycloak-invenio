<#import "template.ftl" as layout>
<@layout.emailLayout>
    <p style="font-size: 14px; color: #333333;">${kcSanitize(msg("magicLinkEmailBody"))?no_esc}</p>
    
    <p style="margin-top: 20px;">
        <a href="${link}" style="display: inline-block; padding: 12px 24px; background-color: #0066cc; color: #ffffff; text-decoration: none; border-radius: 4px; font-weight: bold;">
            ${msg("magicLinkButton")}
        </a>
    </p>
    
    <p style="margin-top: 20px; font-size: 14px; color: #333333;">
        ${msg("magicLinkExpiration", linkExpiration)} ${msg("magicLinkSecurity")}
    </p>
</@layout.emailLayout>
