import { createRoot } from "react-dom/client";
import { StrictMode, Suspense, lazy } from "react";

const KcPageLazy = lazy(async () => {
    const { KcPage } = await import("./kc.gen");

    return { default: KcPage };
});

function getSafeKcContextDiagnostics() {
    const kcContext = (window as any).kcContext as any;

    const safeLocation = (() => {
        try {
            const url = new URL(window.location.href);

            const redactedKeys = new Set([
                "session_code",
                "code",
                "access_token",
                "refresh_token",
                "id_token",
                "token",
                "client_data",
                "kcContext",
                "state"
            ]);

            url.searchParams.forEach((value, key) => {
                if (redactedKeys.has(key) || value.length > 100) {
                    url.searchParams.set(key, "<redacted>");
                }
            });

            return {
                origin: url.origin,
                pathname: url.pathname,
                search: url.search,
                hash: url.hash,
                referrer: document.referrer || undefined
            };
        } catch {
            return {
                pathname: window.location.pathname,
                referrer: document.referrer || undefined
            };
        }
    })();

    if (!kcContext || typeof kcContext !== "object") {
        return { hasKcContext: false as const };
    }

    return {
        hasKcContext: true as const,
        location: safeLocation,
        pageId: kcContext.pageId,
        hasRealm: Boolean(kcContext.realm),
        hasLocale: Boolean(kcContext.locale),
        realmName: kcContext.realm?.name,
        clientId: kcContext.client?.clientId,
        messageSummary: kcContext.message?.summary,
        messageType: kcContext.message?.type,
        statusCode: kcContext.statusCode,
        kcContextKeys: Object.keys(kcContext).sort()
    };
}

function PreflightErrorView(props: { title: string; details?: unknown }) {
    const diagnostics = getSafeKcContextDiagnostics();

    const message = `${props.title}\n\n${JSON.stringify(
        {
            details: props.details,
            diagnostics
        },
        null,
        2
    )}`;

    return (
        <pre
            style={{
                whiteSpace: "pre-wrap",
                fontFamily:
                    "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
                padding: 16,
                margin: 16,
                border: "1px solid #ccc",
                borderRadius: 8
            }}
        >
            {message}
        </pre>
    );
}

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
            hasLocale: Boolean(kcContext.locale),
            messageSummary: kcContext.message?.summary,
            messageType: kcContext.message?.type,
            statusCode: kcContext.statusCode,
            kcContextKeys: Object.keys(kcContext).sort()
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
        ) : !(window.kcContext as any).realm ? (
            <PreflightErrorView
                title="Keycloak context is missing 'realm' (theme cannot render this page)"
                details={{
                    hint: "This often happens on error pages like error.ftl; the theme should still provide realm."
                }}
            />
        ) : (
            <Suspense fallback={<h1>Loading…</h1>}>
                <KcPageLazy kcContext={window.kcContext} />
            </Suspense>
        )}
    </StrictMode>
);
