import { useEffect } from "react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function TurnstileForm(props: PageProps<Extract<KcContext, { pageId: "turnstile-form.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

    const { url, turnstileSiteKey, turnstileMode, turnstileTheme, enableDebugLogging } = kcContext;

    const { msg } = i18n;

    useEffect(() => {
        const w = window as unknown as Record<string, unknown>;

        // Expose Turnstile callbacks globally for the Cloudflare script
        w["TURNSTILE_DEBUG_LOGGING"] = enableDebugLogging;

        w["onTurnstileSuccess"] = (token: string) => {
            if (enableDebugLogging) console.log("[Turnstile] Verification successful");
            const hiddenInput = document.getElementById("turnstile-response") as HTMLInputElement | null;
            if (hiddenInput) hiddenInput.value = token;
            if (turnstileMode === "invisible") {
                (document.getElementById("kc-turnstile-form") as HTMLFormElement | null)?.submit();
            } else {
                const btn = document.getElementById("kc-login") as HTMLButtonElement | null;
                if (btn) btn.disabled = false;
            }
        };

        w["onTurnstileError"] = (errorCode: string) => {
            if (enableDebugLogging) console.warn("[Turnstile] Error:", errorCode);
            const btn = document.getElementById("kc-login") as HTMLButtonElement | null;
            if (btn) btn.disabled = true;
        };

        w["onTurnstileExpired"] = () => {
            if (enableDebugLogging) console.log("[Turnstile] Token expired");
            const btn = document.getElementById("kc-login") as HTMLButtonElement | null;
            if (btn) btn.disabled = true;
        };

        w["onTurnstileTimeout"] = () => {
            if (enableDebugLogging) console.warn("[Turnstile] Challenge timed out");
            const btn = document.getElementById("kc-login") as HTMLButtonElement | null;
            if (btn) btn.disabled = true;
        };

        if (turnstileMode === "invisible") {
            const btn = document.getElementById("kc-login");
            if (btn) {
                btn.addEventListener("click", e => {
                    e.preventDefault();
                    if (enableDebugLogging) console.log("[Turnstile] Triggering invisible challenge...");
                    const widget = document.getElementById("turnstile-widget");
                    const turnstile = w["turnstile"] as { execute: (el: HTMLElement) => void } | undefined;
                    if (widget && turnstile) {
                        turnstile.execute(widget);
                    }
                });
            }
        }

        const script = document.createElement("script");
        script.src = "https://challenges.cloudflare.com/turnstile/v0/api.js";
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);

        return () => {
            if (document.head.contains(script)) {
                document.head.removeChild(script);
            }
        };
    }, []);

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} headerNode={msg("loginAccountTitle")}>
            <div id="kc-form">
                <div id="kc-form-wrapper">
                    <p style={{ textAlign: "center", marginBottom: "16px" }}>{msg("turnstileCheckingHuman")}</p>

                    <div
                        className="turnstile-container"
                        style={{
                            width: "100%",
                            display: "flex",
                            justifyContent: "center",
                            alignItems: "center",
                            marginBottom: "20px",
                            minHeight: "100px"
                        }}
                    >
                        <div
                            id="turnstile-widget"
                            className="cf-turnstile"
                            style={{ width: "100%", maxWidth: "400px" }}
                            data-sitekey={turnstileSiteKey}
                            data-theme={turnstileTheme}
                            data-size={turnstileMode === "invisible" ? "invisible" : "flexible"}
                            data-callback="onTurnstileSuccess"
                            {...(turnstileMode !== "invisible"
                                ? {
                                      "data-error-callback": "onTurnstileError",
                                      "data-expired-callback": "onTurnstileExpired",
                                      "data-timeout-callback": "onTurnstileTimeout"
                                  }
                                : {})}
                            {...(turnstileMode === "non-interactive" ? { "data-appearance": "interaction-only" } : {})}
                        />
                    </div>

                    <form id="kc-turnstile-form" action={url.loginAction} method="post">
                        <input type="hidden" id="turnstile-response" name="cf-turnstile-response" />
                        <div id="kc-form-buttons" className={kcClsx("kcFormGroupClass")}>
                            <button
                                type="submit"
                                name="login"
                                id="kc-login"
                                className={kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass")}
                                disabled={turnstileMode !== "invisible"}
                            >
                                {msg("doLogIn")}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </Template>
    );
}
