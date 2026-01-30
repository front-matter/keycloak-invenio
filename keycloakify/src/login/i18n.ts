/* eslint-disable @typescript-eslint/no-unused-vars */
import { i18nBuilder } from "keycloakify/login";
import type { ThemeName } from "../kc.gen";

/** @see: https://docs.keycloakify.dev/features/i18n */
const { useI18n, ofTypeI18n } = i18nBuilder
    .withThemeName<ThemeName>()
    .withExtraLanguages({ /* ... */ })
    .withCustomTranslations({
        // WARNING: You can't import the translation from external files
        en: {
            usernameOrEmail: "Email",
            "magic-link-authenticator-display-name": "Email link",
            "magic-link-authenticator-help-text": "Get a one-time sign-in link by email.",
            magicLinkLoginTitle: "Sign in via email link",
            magicLinkLoginInstructions: "Enter your email address and we will send you a one-time sign-in link.",
            magicLinkLoginStep1: "Check your inbox (and spam folder)",
            magicLinkLoginStep2: "Open the email and click the sign-in link",
            magicLinkLoginStep3: "You will be signed in automatically",
            magicLinkLoginSubmit: "Send sign-in link",
            magicLinkSentTitle: "Check Your Email",
            magicLinkSentHeader: "Login link sent!",
            magicLinkSentInstructions: "We have sent a login link to your email address. Click the link in the email to log in.",
            magicLinkSentNextSteps: "What to do next:",
            magicLinkSentStep1: "Check your email inbox (and spam folder)",
            magicLinkSentStep2: "Click the login link in the email",
            magicLinkSentStep3: "You will be logged in automatically",
            magicLinkSentNoEmail: "Did not receive the email?",
            magicLinkSentResend: "Request New Link",
            magicLinkSentCloseHint: "You can close this window after clicking the link in the email.",
            magicLinkSentBackToApplication: "Back to application"
        },
        // cspell: disable
        de: {
            usernameOrEmail: "Email",
            "magic-link-authenticator-display-name": "Login-Link per E-Mail",
            "magic-link-authenticator-help-text": "Erhalten Sie einen einmaligen Anmeldelink per E-Mail.",
            magicLinkLoginTitle: "Anmeldung per E-Mail-Link",
            magicLinkLoginInstructions: "Geben Sie Ihre E-Mail-Adresse ein. Wir senden Ihnen einen einmaligen Anmeldelink.",
            magicLinkLoginStep1: "Posteingang (und Spam-Ordner) prüfen",
            magicLinkLoginStep2: "E-Mail öffnen und auf den Anmeldelink klicken",
            magicLinkLoginStep3: "Sie werden automatisch angemeldet",
            magicLinkLoginSubmit: "Anmeldelink senden",
            magicLinkSentTitle: "Überprüfen Sie Ihre E-Mail",
            magicLinkSentHeader: "Anmeldelink gesendet!",
            magicLinkSentInstructions: "Wir haben einen Anmeldelink an Ihre E-Mail-Adresse gesendet. Klicken Sie auf den Link in der E-Mail, um sich anzumelden.",
            magicLinkSentNextSteps: "Nächste Schritte:",
            magicLinkSentStep1: "Überprüfen Sie Ihren E-Mail-Posteingang (und Spam-Ordner)",
            magicLinkSentStep2: "Klicken Sie auf den Anmeldelink in der E-Mail",
            magicLinkSentStep3: "Sie werden automatisch angemeldet",
            magicLinkSentNoEmail: "Haben Sie die E-Mail nicht erhalten?",
            magicLinkSentResend: "Neuen Link anfordern",
            magicLinkSentCloseHint: "Sie können dieses Fenster schließen, nachdem Sie den Link in der E-Mail angeklickt haben.",
            magicLinkSentBackToApplication: "Zurück zur Anwendung"
        },
        fr: {
            usernameOrEmail: "Email",
            "magic-link-authenticator-display-name": "Lien par email",
            "magic-link-authenticator-help-text": "Recevez un lien de connexion à usage unique par email.",
            magicLinkLoginTitle: "Connexion via lien email",
            magicLinkLoginInstructions: "Saisissez votre adresse email et nous vous enverrons un lien de connexion à usage unique.",
            magicLinkLoginStep1: "Vérifiez votre boîte de réception (et les spams)",
            magicLinkLoginStep2: "Ouvrez l'email et cliquez sur le lien",
            magicLinkLoginStep3: "Vous serez connecté automatiquement",
            magicLinkLoginSubmit: "Envoyer le lien",
            magicLinkSentTitle: "Vérifiez votre email",
            magicLinkSentHeader: "Lien de connexion envoyé !",
            magicLinkSentInstructions: "Nous avons envoyé un lien de connexion à votre adresse email. Cliquez sur le lien dans l'email pour vous connecter.",
            magicLinkSentNextSteps: "Que faire ensuite :",
            magicLinkSentStep1: "Vérifiez votre boîte de réception (et votre dossier spam)",
            magicLinkSentStep2: "Cliquez sur le lien de connexion dans l'email",
            magicLinkSentStep3: "Vous serez connecté automatiquement",
            magicLinkSentNoEmail: "Vous n'avez pas reçu l'email ?",
            magicLinkSentResend: "Demander un nouveau lien",
            magicLinkSentCloseHint: "Vous pouvez fermer cette fenêtre après avoir cliqué sur le lien dans l'email.",
            magicLinkSentBackToApplication: "Retour à l'application"
        },
        es: {
            usernameOrEmail: "Email",
            "magic-link-authenticator-display-name": "Enlace por correo",
            "magic-link-authenticator-help-text": "Recibe un enlace de inicio de sesión de un solo uso por correo.",
            magicLinkLoginTitle: "Iniciar sesión con enlace por correo",
            magicLinkLoginInstructions: "Introduce tu correo y te enviaremos un enlace de inicio de sesión de un solo uso.",
            magicLinkLoginStep1: "Revisa tu bandeja de entrada (y spam)",
            magicLinkLoginStep2: "Abre el correo y haz clic en el enlace",
            magicLinkLoginStep3: "Iniciarás sesión automáticamente",
            magicLinkLoginSubmit: "Enviar enlace",
            magicLinkSentTitle: "Revisa tu correo electrónico",
            magicLinkSentHeader: "¡Enlace de inicio de sesión enviado!",
            magicLinkSentInstructions: "Hemos enviado un enlace de inicio de sesión a tu dirección de correo electrónico. Haz clic en el enlace del correo para iniciar sesión.",
            magicLinkSentNextSteps: "Qué hacer a continuación:",
            magicLinkSentStep1: "Revisa tu bandeja de entrada (y la carpeta de spam)",
            magicLinkSentStep2: "Haz clic en el enlace de inicio de sesión en el correo",
            magicLinkSentStep3: "Iniciarás sesión automáticamente",
            magicLinkSentNoEmail: "¿No recibiste el correo?",
            magicLinkSentResend: "Solicitar nuevo enlace",
            magicLinkSentCloseHint: "Puedes cerrar esta ventana después de hacer clic en el enlace del correo.",
            magicLinkSentBackToApplication: "Volver a la aplicación"
        }
        // cspell: enable
    })
    .build();

type I18n = typeof ofTypeI18n;

export { useI18n, type I18n };
