// This file extends the KcContext social.providers type for Storybook and local development
// so that the 'icon' property is allowed for social providers.

import type { KcContext as OrigKcContext } from "./KcContext";
import type { IconProp } from "@fortawesome/fontawesome-svg-core";

declare module "./KcContext" {
    interface SocialProvider {
        loginUrl: string;
        alias: string;
        providerId: string;
        displayName: string;
        iconClasses?: string;
        icon?: import("@fortawesome/fontawesome-svg-core").IconProp;
    }
    interface Social {
        displayInfo: boolean;
        providers: SocialProvider[];
    }
}

// Extend realm to include custom attributes
declare module "keycloakify/login" {
    interface KcContextBase {
        realm: KcContextBase["realm"] & {
            attributes?: {
                logoName?: string;
                [key: string]: string | undefined;
            };
        };
    }
}
