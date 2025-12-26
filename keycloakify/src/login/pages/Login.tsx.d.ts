// TypeScript declaration merging for local development and Storybook
declare module "../KcContext" {
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
    interface KcContext {
        social?: Social;
    }
}
