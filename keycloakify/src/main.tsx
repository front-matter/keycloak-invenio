import { createRoot } from "react-dom/client";
import { StrictMode } from "react";
import { KcPage } from "./kc.gen";

function renderFatalError(title: string, error: unknown) {
    const root = document.getElementById("root");

    const safeContext = (() => {
        const kcContext = (window as any).kcContext as any;

        if (!kcContext || typeof kcContext !== "object") {
            return { hasKcContext: false };
        }

        return {
            hasKcContext: true,
            pageId: kcContext.pageId,
            realmName: kcContext.realm?.name,
            clientId: kcContext.client?.clientId,
            hasRealm: Boolean(kcContext.realm),
            hasLocale: Boolean(kcContext.locale)
        };
    })();

    const safeError = (() => {
        if (error instanceof Error) {
            return { name: error.name, message: error.message };
        }

        if (typeof error === "string") {
            return { message: error };
        }

        return { message: "Unknown error" };
    })();

    const message = `${title}\n\n${JSON.stringify(
        { error: safeError, context: safeContext },
        null,
        2
    )}`;

    if (root) {
        root.innerHTML = "";
        const pre = document.createElement("pre");
        pre.style.whiteSpace = "pre-wrap";
        pre.style.fontFamily =
            "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace";
        pre.style.padding = "16px";
        pre.style.margin = "16px";
        pre.style.border = "1px solid #ccc";
        pre.style.borderRadius = "8px";
        pre.textContent = message;
        root.appendChild(pre);
        return;
    }

    // Fallback: last resort
    document.body.textContent = message;
}

window.addEventListener("error", event => {
    renderFatalError("JavaScript error", (event as any).error ?? (event as any).message);
});

window.addEventListener("unhandledrejection", event => {
    renderFatalError(
        "Unhandled promise rejection",
        (event as PromiseRejectionEvent).reason
    );
});

// The following block can be uncommented to test a specific page with `yarn dev`
// Don't forget to comment back or your bundle size will increase
/*
import { getKcContextMock } from "./login/KcPageStory";

if (import.meta.env.DEV) {
    window.kcContext = getKcContextMock({
        pageId: "register.ftl",
        overrides: {}
    });
}
*/

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        {!window.kcContext ? (
            <h1>No Keycloak Context</h1>
        ) : (
            <KcPage kcContext={window.kcContext} />
        )}
    </StrictMode>
);
