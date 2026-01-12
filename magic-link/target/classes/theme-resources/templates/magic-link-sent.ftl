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
                
                <div style="margin-top: 30px; padding: 20px; background-color: #f5f5f5; border-radius: 4px;">
                    <h3 style="margin-top: 0; font-size: 16px; color: #333333;">
                        ${msg("magicLinkSentNextSteps")}
                    </h3>
                    <ol style="margin-bottom: 0; padding-left: 20px; color: #666666;">
                        <li>${msg("magicLinkSentStep1")}</li>
                        <li>${msg("magicLinkSentStep2")}</li>
                        <li>${msg("magicLinkSentStep3")}</li>
                    </ol>
                </div>
                
                <div style="margin-top: 20px; text-align: center;">
                    <p style="font-size: 14px; color: #999999;">
                        ${msg("magicLinkSentNoEmail")}
                    </p>
                    <form id="kc-form-login" action="${url.loginAction}" method="post">
                        <input type="hidden" name="restart" value="true" />
                        <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" style="margin-top: 10px;">
                            ${msg("magicLinkSentResend")}
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
