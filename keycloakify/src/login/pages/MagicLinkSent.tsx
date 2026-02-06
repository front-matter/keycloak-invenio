import type { PageProps } from "keycloakify/login/pages/PageProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { clsx } from "keycloakify/tools/clsx";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function MagicLinkSent(props: PageProps<Extract<KcContext, { pageId: "magic-link-sent.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { url } = kcContext;
    const { msg } = i18n;

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={false}
            headerNode={msg("magicLinkSentTitle")}
        >
            <div className={clsx("alert-success", kcClsx("kcAlertClass"), "pf-m-success")}>
                <div className="pf-c-alert__icon">
                    <span className={kcClsx("kcFeedbackSuccessIcon")}></span>
                </div>
                <div className={kcClsx("kcAlertTitleClass")}>{msg("magicLinkSentHeader")}</div>
            </div>

            <div id="kc-form">
                <div id="kc-form-wrapper">
                    <p>{msg("magicLinkSentInstructions")}</p>

                    <p>Click the link in your email to complete the login. You can safely close this window.</p>

                    <div style={{ marginTop: "20px", textAlign: "center" }}>
                        <p style={{ fontSize: "14px", color: "#999999" }}>{msg("magicLinkSentNoEmail")}</p>
                        <form id="kc-form-login" action={url.loginAction} method="post">
                            <input type="hidden" name="restart" value="true" />
                            <button
                                type="submit"
                                className={clsx(
                                    kcClsx("kcButtonClass"),
                                    kcClsx("kcButtonDefaultClass"),
                                    kcClsx("kcButtonBlockClass"),
                                    kcClsx("kcButtonLargeClass")
                                )}
                                style={{ marginTop: "10px" }}
                            >
                                {msg("magicLinkSentResend")}
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </Template>
    );
}
