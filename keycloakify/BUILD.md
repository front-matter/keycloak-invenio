# Keycloak Theme for InvenioRDM

This directory contains the Keycloakify-based theme for the Keycloak InvenioRDM integration.

## Prerequisites

- Node.js 18.x or 20.x+
- pnpm (recommended) or npm

## Installation

Install dependencies:

```bash
pnpm install
```

## Development

Run the development server with hot reload:

```bash
pnpm dev
```

## Building the Theme

Build the Keycloak theme JAR files:

```bash
pnpm run build-keycloak-theme
```

This will generate two JAR files in `dist_keycloak/`:
- `keycloak-theme-for-kc-22-to-25.jar` - For Keycloak 22-25
- `keycloak-theme-for-kc-all-other-versions.jar` - For Keycloak 26+

For Keycloak 26.4, use `keycloak-theme-for-kc-all-other-versions.jar`.

## Using the Theme in Docker

The JAR file needs to be copied to `/opt/keycloak/providers/` in the Keycloak container.

The parent repository's Dockerfile expects the file at:
```
../dist_keycloak/keycloak-theme-front-matter.jar
```

To update the theme used by Docker:

1. Build the theme: `pnpm run build-keycloak-theme`
2. Copy the appropriate JAR to the parent's dist_keycloak folder:
   ```bash
   cp dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar ../dist_keycloak/keycloak-theme-front-matter.jar
   ```
3. Rebuild the Docker image from the parent directory

## Storybook

Run Storybook for component development:

```bash
pnpm run storybook
```

## Project Structure

```
keycloakify/
├── src/              # Source code for theme customizations
├── public/           # Static assets
├── dist/             # Vite build output
├── dist_keycloak/    # Keycloak theme JAR files
└── .storybook/       # Storybook configuration
```

## Customization

The theme is built using Keycloakify 11.x. Key customization points:

- `src/login/` - Login page templates
- `src/account/` - Account console templates
- `vite.config.ts` - Build configuration
- `tailwind.config.js` - Tailwind CSS configuration

## Documentation

- [Keycloakify Documentation](https://docs.keycloakify.dev/)
- [Keycloak Theming Guide](https://www.keycloak.org/docs/latest/server_development/#_themes)
