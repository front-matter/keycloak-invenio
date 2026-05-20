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
};

export type KcContext = ExtendKcContext<KcContextExtension, KcContextExtensionPerPage>;
