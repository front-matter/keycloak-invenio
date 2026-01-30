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
                
                <div style="margin-top: 20px; text-align: center; padding: 15px; background-color: #fff3cd; border-radius: 4px; border: 1px solid #ffc107;">
                    <p style="font-size: 14px; color: #856404; margin: 0;">
                        <strong>Important:</strong> You can safely close this window. Click the link in your email to complete the login.
                    </p>
                </div>

                <script>
                    // Allow user to close this window/tab after a few seconds
                    setTimeout(function() {
                        // Add a close button for convenience
                        var closeBtn = document.createElement('button');
                        closeBtn.innerHTML = 'Close this window';
                        closeBtn.className = '${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!}';
                        closeBtn.style.marginTop = '20px';
                        closeBtn.onclick = function() {
                            window.close();
                        };
                        document.getElementById('kc-form-wrapper').appendChild(closeBtn);
                    }, 3000);
                </script>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
