import { useState } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function LoginUsername(props: PageProps<Extract<KcContext, { pageId: "login-username.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { msg } = i18n;
    const msgStrUnsafe = i18n.msgStr as unknown as (key: string, ...args: Array<string | undefined>) => string;

    const { realm, url, login, messagesPerField, auth } = kcContext;

    const [isSubmitDisabled, setIsSubmitDisabled] = useState(false);

    const isMagicLink = (() => {
        type AuthWithSelections = {
            authenticationSelections?: Array<{
                displayName?: string;
                helpText?: string;
            }>;
        };

        const selections = (auth as unknown as AuthWithSelections).authenticationSelections;

        if (!Array.isArray(selections)) {
            return false;
        }

        return selections.some(s =>
            ["magic-link-authenticator-display-name", "magic-link-authenticator-help-text"].some(key => s?.displayName === key || s?.helpText === key)
        );
    })();

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={!messagesPerField?.existsError?.("username")}
            headerNode={isMagicLink ? msgStrUnsafe("magicLinkLoginTitle") : msg("loginAccountTitle")}
        >
            <div id="kc-form">
                <div id="kc-form-wrapper">
                    {isMagicLink && (
                        <div
                            className={kcClsx("kcFormGroupClass")}
                            style={{
                                padding: "12px",
                                border: "1px solid rgba(0,0,0,0.12)",
                                borderRadius: 8,
                                marginBottom: 12
                            }}
                        >
                            <p style={{ margin: 0 }}>{msgStrUnsafe("magicLinkLoginInstructions")}</p>
                            <ul style={{ margin: "10px 0 0 18px" }}>
                                <li>{msgStrUnsafe("magicLinkLoginStep1")}</li>
                                <li>{msgStrUnsafe("magicLinkLoginStep2")}</li>
                                <li>{msgStrUnsafe("magicLinkLoginStep3")}</li>
                            </ul>
                        </div>
                    )}

                    <form
                        id="kc-form-login"
                        onSubmit={() => {
                            setIsSubmitDisabled(true);
                            return true;
                        }}
                        action={url.loginAction}
                        method="post"
                    >
                        <div className={kcClsx("kcFormGroupClass")}>
                            <label htmlFor="username" className={kcClsx("kcLabelClass")}>
                                {realm?.loginWithEmailAllowed ? msg("usernameOrEmail") : msg("username")}
                            </label>

                            <input
                                tabIndex={2}
                                id="username"
                                className={kcClsx("kcInputClass")}
                                name="username"
                                defaultValue={login?.username ?? ""}
                                type="text"
                                autoFocus
                                autoComplete={realm?.loginWithEmailAllowed ? "email" : "username"}
                                aria-invalid={Boolean(messagesPerField?.existsError?.("username"))}
                                inputMode={realm?.loginWithEmailAllowed ? "email" : undefined}
                            />

                            {messagesPerField?.existsError?.("username") && (
                                <span
                                    id="input-error"
                                    className={kcClsx("kcInputErrorMessageClass")}
                                    aria-live="polite"
                                    dangerouslySetInnerHTML={{
                                        __html: kcSanitize(messagesPerField.getFirstError("username"))
                                    }}
                                />
                            )}
                        </div>

                        <div id="kc-form-buttons" className={kcClsx("kcFormGroupClass")}>
                            <input
                                tabIndex={7}
                                disabled={isSubmitDisabled}
                                className={kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass")}
                                name="login"
                                id="kc-login"
                                type="submit"
                                value={isMagicLink ? msgStrUnsafe("magicLinkLoginSubmit") : msgStrUnsafe("doContinue")}
                            />
                        </div>
                    </form>
                </div>
            </div>
        </Template>
    );
}
