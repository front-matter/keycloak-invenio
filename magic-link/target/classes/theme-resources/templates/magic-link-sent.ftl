<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("magicLinkSentTitle")}
    <#elseif section = "form">
        <div class="alert-success ${properties.kcAlertClass!} pf-m-success">
            <div class="pf-c-alert__icon">
                <span class="${properties.kcFeedbackSuccessIcon!}"></span>
            </div>
            <div class="${properties.kcAlertTitleClass!}">
                ${msg("magicLinkSentHeader")}
            </div>
        </div>
        
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <p>${msg("magicLinkSentInstructions")}</p>
                
                <p>Click the link in your email to complete the login. You can safely close this window.</p>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
