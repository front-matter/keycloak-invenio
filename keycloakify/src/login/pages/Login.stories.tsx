import type { Meta, StoryObj } from "@storybook/react-vite";
import { createKcPageStory } from "../KcPageStory";
import { socialProviderIconMap } from "../socialProviderIconMap";

const createProvider = (alias: keyof typeof socialProviderIconMap) => ({
    loginUrl: alias,
    alias,
    providerId: alias,
    displayName: alias === "orcid" ? "ORCID" : alias === "github" ? "GitHub" : alias.charAt(0).toUpperCase() + alias.slice(1),
    icon: socialProviderIconMap[alias]
});

const { KcPageStory } = createKcPageStory({ pageId: "login.ftl" });

const meta = {
    title: "login/login.ftl",
    component: KcPageStory
} satisfies Meta<typeof KcPageStory>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
    render: () => <KcPageStory />
};

export const WithInvalidCredential: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                login: {
                    username: "johndoe"
                },
                messagesPerField: {
                    // NOTE: The other functions of messagesPerField are derived from get() and
                    // existsError() so they are the only ones that need to mock.
                    existsError: (fieldName: string, ...otherFieldNames: string[]) => {
                        const fieldNames = [fieldName, ...otherFieldNames];
                        return fieldNames.includes("username") || fieldNames.includes("password");
                    },
                    get: (fieldName: string) => {
                        if (fieldName === "username" || fieldName === "password") {
                            return "Invalid username or password.";
                        }
                        return "";
                    }
                }
            }}
        />
    )
};

export const WithoutRegistration: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                realm: { registrationAllowed: false }
            }}
        />
    )
};

export const WithoutRememberMe: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                realm: { rememberMe: false }
            }}
        />
    )
};
export const WithPresetUsername: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                login: { username: "max.mustermann@mail.com" }
            }}
        />
    )
};

export const WithImmutablePresetUsername: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                auth: {
                    attemptedUsername: "max.mustermann@mail.com",
                    showUsername: true
                },
                usernameHidden: true,
                message: {
                    type: "info",
                    summary: "Please re-authenticate to continue"
                }
            }}
        />
    )
};

export const WithSocialProviders: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: [
                        ...(
                            [
                                "google",
                                "microsoft",
                                "meta",
                                "instagram",
                                "x",
                                "linkedin",
                                "stackoverflow",
                                "github",
                                "gitlab",
                                "bitbucket",
                                "paypal",
                                "orcid"
                            ] as (keyof typeof socialProviderIconMap)[]
                        ).map(createProvider)
                    ]
                }
            }}
        />
    )
};

export const WithoutPasswordField: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                realm: { password: false }
            }}
        />
    )
};

export const WithErrorMessage: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                message: {
                    summary: "The time allotted for the connection has elapsed.<br/>The login process will restart from the beginning.",
                    type: "error"
                }
            }}
        />
    )
};

export const WithOneSocialProvider: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: [createProvider("orcid")]
                }
            }}
        />
    )
};
export const WithoutPasswordFieldWithOneSocialProvider: Story = {
    render: () => (
        <KcPageStory
            kcContext={{
                realm: { password: false },
                social: {
                    displayInfo: true,
                    providers: [createProvider("orcid")]
                }
            }}
        />
    )
};

export const WithTwoSocialProviders: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: [createProvider("google"), createProvider("microsoft")]
                }
            }}
        />
    )
};
export const WithNoSocialProviders: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: []
                }
            }}
        />
    )
};
export const WithMoreThanTwoSocialProviders: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: [createProvider("google"), createProvider("microsoft"), createProvider("meta"), createProvider("x")]
                }
            }}
        />
    )
};
export const WithSocialProvidersAndWithoutRememberMe: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                social: {
                    displayInfo: true,
                    providers: [createProvider("google")]
                },
                realm: { rememberMe: false }
            }}
        />
    )
};
/**
 * WithAuthPassKey:
 * - Purpose: Test usage of Sign In With Pass Key integration
 * - Scenario: Simulates a scenario where the `Sign In with Passkey` button is rendered below `Sign In` button.
 * - Key Aspect: Ensure that it is displayed correctly.
 */
export const WithAuthPassKey: Story = {
    render: args => (
        <KcPageStory
            {...args}
            kcContext={{
                url: {
                    loginAction: "/mock-login-action"
                },
                enableWebAuthnConditionalUI: true
            }}
        />
    )
};
