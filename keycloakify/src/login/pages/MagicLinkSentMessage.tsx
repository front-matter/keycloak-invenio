import type { PageProps } from "keycloakify/login/pages/PageProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { clsx } from "keycloakify/tools/clsx";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export type MagicLinkSentMessageContext = Extract<KcContext, { pageId: "info.ftl" | "error.ftl" }>;

export default function MagicLinkSentMessage(props: PageProps<MagicLinkSentMessageContext, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { msg } = i18n;

    // On info/error pages we generally only have a link back (no reliable form action).
    const backHref =
        ("pageRedirectUri" in kcContext ? kcContext.pageRedirectUri : undefined) ??
        ("actionUri" in kcContext ? kcContext.actionUri : undefined) ??
        kcContext.client?.baseUrl;

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

                    <div
                        style={{
                            marginTop: "30px",
                            padding: "20px",
                            backgroundColor: "#f5f5f5",
                            borderRadius: "4px"
                        }}
                    >
                        <h3 style={{ marginTop: 0, fontSize: "16px", color: "#333333" }}>{msg("magicLinkSentNextSteps")}</h3>
                        <ol style={{ marginBottom: 0, paddingLeft: "20px", color: "#666666" }}>
                            <li>{msg("magicLinkSentStep1")}</li>
                            <li>{msg("magicLinkSentStep2")}</li>
                            <li>{msg("magicLinkSentStep3")}</li>
                        </ol>
                    </div>

                    <p style={{ marginTop: "20px", color: "#666666" }}>{msg("magicLinkSentCloseHint")}</p>

                    {backHref && (
                        <p style={{ marginTop: "20px" }}>
                            <a href={backHref}>{msg("magicLinkSentBackToApplication")}</a>
                        </p>
                    )}
                </div>
            </div>
        </Template>
    );
}
