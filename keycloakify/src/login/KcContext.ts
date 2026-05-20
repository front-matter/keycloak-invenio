/* eslint-disable @typescript-eslint/no-empty-object-type */
import type { ExtendKcContext } from "keycloakify/login";
import type { KcEnvName, ThemeName } from "../kc.gen";

export type KcContextExtension = {
    themeName: ThemeName;
    properties: Record<KcEnvName, string> & {};
    // Extend realm to support custom attributes
    realm: {
        attributes?: {
            logoName?: string;
            logoUrl?: string;
            [key: string]: string | undefined;
        };
    };
};

export type KcContextExtensionPerPage = {
    "magic-link-sent.ftl": {};
    "login.ftl": {
        turnstileRequired?: boolean;
        turnstileSkipped?: boolean;
        turnstileSiteKey?: string;
        turnstileTheme?: "light" | "dark" | "auto";
        turnstileMode?: "managed" | "non-interactive" | "invisible";
    };
};

export type KcContext = ExtendKcContext<KcContextExtension, KcContextExtensionPerPage>;
