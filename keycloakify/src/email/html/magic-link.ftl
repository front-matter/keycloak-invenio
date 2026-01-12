<#import "template.ftl" as layout>
<@layout.emailLayout>
    <p>${kcSanitize(msg("magicLinkEmailBody"))?no_esc}</p>
    
    <p>
        <a href="${link}" style="display: inline-block; padding: 12px 24px; background-color: #0066cc; color: #ffffff; text-decoration: none; border-radius: 4px; font-weight: bold;">
            ${msg("magicLinkButton")}
        </a>
    </p>
    
    <p style="margin-top: 20px; font-size: 14px; color: #666666;">
        ${msg("magicLinkExpiration", linkExpiration)}
    </p>
    
    <p style="margin-top: 20px; font-size: 12px; color: #999999;">
        ${msg("magicLinkAlternative")}<br>
        <a href="${link}" style="color: #0066cc; word-break: break-all;">${link}</a>
    </p>
    
    <p style="margin-top: 20px; font-size: 12px; color: #999999;">
        ${msg("magicLinkSecurity")}
    </p>
</@layout.emailLayout>
