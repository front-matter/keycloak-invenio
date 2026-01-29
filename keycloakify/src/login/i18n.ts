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
            magicLinkSentTitle: "Check Your Email",
            magicLinkSentHeader: "Login link sent!",
            magicLinkSentInstructions: "We have sent a login link to your email address. Click the link in the email to log in.",
            magicLinkSentNextSteps: "What to do next:",
            magicLinkSentStep1: "Check your email inbox (and spam folder)",
            magicLinkSentStep2: "Click the login link in the email",
            magicLinkSentStep3: "You will be logged in automatically",
            magicLinkSentNoEmail: "Did not receive the email?",
            magicLinkSentResend: "Request New Link"
        },
        // cspell: disable
        de: {
            usernameOrEmail: "Email",
            magicLinkSentTitle: "Überprüfen Sie Ihre E-Mail",
            magicLinkSentHeader: "Anmeldelink gesendet!",
            magicLinkSentInstructions: "Wir haben einen Anmeldelink an Ihre E-Mail-Adresse gesendet. Klicken Sie auf den Link in der E-Mail, um sich anzumelden.",
            magicLinkSentNextSteps: "Nächste Schritte:",
            magicLinkSentStep1: "Überprüfen Sie Ihren E-Mail-Posteingang (und Spam-Ordner)",
            magicLinkSentStep2: "Klicken Sie auf den Anmeldelink in der E-Mail",
            magicLinkSentStep3: "Sie werden automatisch angemeldet",
            magicLinkSentNoEmail: "Haben Sie die E-Mail nicht erhalten?",
            magicLinkSentResend: "Neuen Link anfordern"
        },
        fr: {
            usernameOrEmail: "Email",
            magicLinkSentTitle: "Vérifiez votre email",
            magicLinkSentHeader: "Lien de connexion envoyé !",
            magicLinkSentInstructions: "Nous avons envoyé un lien de connexion à votre adresse email. Cliquez sur le lien dans l'email pour vous connecter.",
            magicLinkSentNextSteps: "Que faire ensuite :",
            magicLinkSentStep1: "Vérifiez votre boîte de réception (et votre dossier spam)",
            magicLinkSentStep2: "Cliquez sur le lien de connexion dans l'email",
            magicLinkSentStep3: "Vous serez connecté automatiquement",
            magicLinkSentNoEmail: "Vous n'avez pas reçu l'email ?",
            magicLinkSentResend: "Demander un nouveau lien"
        },
        es: {
            usernameOrEmail: "Email",
            magicLinkSentTitle: "Revisa tu correo electrónico",
            magicLinkSentHeader: "¡Enlace de inicio de sesión enviado!",
            magicLinkSentInstructions: "Hemos enviado un enlace de inicio de sesión a tu dirección de correo electrónico. Haz clic en el enlace del correo para iniciar sesión.",
            magicLinkSentNextSteps: "Qué hacer a continuación:",
            magicLinkSentStep1: "Revisa tu bandeja de entrada (y la carpeta de spam)",
            magicLinkSentStep2: "Haz clic en el enlace de inicio de sesión en el correo",
            magicLinkSentStep3: "Iniciarás sesión automáticamente",
            magicLinkSentNoEmail: "¿No recibiste el correo?",
            magicLinkSentResend: "Solicitar nuevo enlace"
        }
        // cspell: enable
    })
    .build();

type I18n = typeof ofTypeI18n;

export { useI18n, type I18n };
