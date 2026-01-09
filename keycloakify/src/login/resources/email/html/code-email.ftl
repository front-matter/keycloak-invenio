<#import "template.ftl" as layout>
<@layout.emailLayout>
<p>Hello ${username!},</p>

<p>Your verification code is: <strong>${code}</strong></p>

<p>This code will expire in ${ttl} seconds.</p>

<p>If you did not request this code, please ignore this email.</p>

<p>Best regards,<br>
${realmName}</p>
</@layout.emailLayout>
