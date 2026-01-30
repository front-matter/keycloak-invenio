import { Suspense, lazy } from "react";
import type { ClassKey } from "keycloakify/login";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";
import DefaultPage from "keycloakify/login/DefaultPage";
import Template from "./Template";
import "./main.css";

const UserProfileFormFields = lazy(
    () => import("keycloakify/login/UserProfileFormFields")
);
const Login = lazy(() => import("./pages/Login"));
const MagicLinkSent = lazy(() => import("./pages/MagicLinkSent"));
const MagicLinkSentMessage = lazy(() => import("./pages/MagicLinkSentMessage"));

const doMakeUserConfirmPassword = true;

export default function KcPage(props: { kcContext: KcContext }) {
    const { kcContext } = props;

    const { i18n } = useI18n({ kcContext });

    const messageSummary = kcContext.message?.summary?.toLowerCase() ?? "";
    const isMagicLinkSentMessage =
        messageSummary.includes("login link") ||
        messageSummary.includes("sign-in link") ||
        messageSummary.includes("anmeldelink") ||
        messageSummary.includes("magic link");

    return (
        <Suspense>
            {(() => {
                if (
                    (kcContext.pageId === "info.ftl" ||
                        kcContext.pageId === "error.ftl") &&
                    isMagicLinkSentMessage
                ) {
                    return (
                        <MagicLinkSentMessage
                            {...{ kcContext, i18n, classes }}
                            Template={Template}
                            doUseDefaultCss={true}
                        />
                    );
                }

                switch (kcContext.pageId) {
                    case "login.ftl":
                        return (
                            <Login
                                {...{ kcContext, i18n, classes }}
                                Template={Template}
                                doUseDefaultCss={true}
                            />
                        );
                    case "magic-link-sent.ftl":
                        return (
                            <MagicLinkSent
                                {...{ kcContext, i18n, classes }}
                                Template={Template}
                                doUseDefaultCss={true}
                            />
                        );
                    default:
                        return (
                            <DefaultPage
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={true}
                                UserProfileFormFields={UserProfileFormFields}
                                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                            />
                        );
                }
            })()}
        </Suspense>
    );
}

const classes = {} satisfies { [key in ClassKey]?: string };
