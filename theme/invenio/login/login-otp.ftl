<#--
  Copy of theme/base/login/login-otp.ftl from Keycloak 26.8.2, reworded so that
  password managers recognise it as a two-factor step and offer the code: a line
  of instructions ("Enter a verification code from authenticator application.")
  and a "Submit" button instead of "Sign In" (Proton Pass, for one, ignores the
  code field of a form that reads like a sign-in form), and inputmode="numeric"
  on the field. The heading "Authenticator Application" shows only where the
  layout does not show the username in its place.
  The messages are Keycloak's own, translated into every language. On a
  Keycloak upgrade, diff this file against the new base template.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section="header">
        ${msg("otp-display-name")}
    <#elseif section="form">
        <form id="kc-otp-form" class="${properties.kcFormClass!}" onsubmit="verify.disabled = true; return true;" action="${url.loginAction}"
            method="post">
            <p class="instruction">${msg("otp-help-text")}</p>
            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcInputWrapperClass!}">
                        <#list otpLogin.userOtpCredentials as otpCredential>
                            <input id="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListInputClass!}" type="radio" name="selectedCredentialId" value="${otpCredential.id}" <#if otpCredential.id == otpLogin.selectedCredentialId>checked="checked"</#if>>
                            <label for="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListClass!}" tabindex="${otpCredential?index}">
                                <span class="${properties.kcLoginOTPListItemHeaderClass!}">
                                    <span class="${properties.kcLoginOTPListItemIconBodyClass!}">
                                      <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                                    </span>
                                    <span class="${properties.kcLoginOTPListItemTitleClass!}">${otpCredential.userLabel}</span>
                                </span>
                            </label>
                        </#list>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                </div>

            <div class="${properties.kcInputWrapperClass!}">
                <input id="otp" name="otp" autocomplete="one-time-code" type="text" inputmode="numeric" class="${properties.kcInputClass!}"
                       autofocus aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"
                       dir="ltr" />

                <#if messagesPerField.existsError('totp')>
                    <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                          aria-live="polite">
                        ${kcSanitize(messagesPerField.get('totp'))?no_esc}
                    </span>
                </#if>
            </div>
        </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        name="verify" id="kc-verify" type="submit" value="${msg("doSubmit")}" />
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>