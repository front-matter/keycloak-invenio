<#import "template.ftl" as layout>
<@layout.emailLayout>
    <p style="font-size: 14px; color: #333333;">${kcSanitize(msg("magicLinkEmailBody", linkExpiration))?no_esc}</p>

    <table role="presentation" border="0" cellpadding="0" cellspacing="0" style="margin: 24px 0;">
        <tr>
            <td align="center" bgcolor="#0066cc" style="border-radius: 6px;">
                <a href="${link}" target="_blank" style="display: inline-block; padding: 14px 32px; font-family: Arial, sans-serif; font-size: 16px; line-height: 20px; font-weight: bold; color: #ffffff; text-decoration: none; border-radius: 6px; border: 1px solid #0066cc;">${msg("magicLinkButton")}</a>
            </td>
        </tr>
    </table>

    <p style="font-size: 14px; color: #333333;">${msg("magicLinkSecurity")}</p>
</@layout.emailLayout>
