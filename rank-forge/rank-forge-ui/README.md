# RankForge UI

React + TypeScript + Vite frontend for the RankForge CS2 player rankings and game analytics platform.

## Features

- Player rankings with detailed statistics
- Game details and round-by-round analysis
- Player profiles with performance history
- Accolades and achievements tracking
- Responsive design with modern UI

## Development

### Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080`

### Setup

```bash
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

### Build

```bash
npm run build
```

### Lint

```bash
npm run lint
```

## E2E Testing

This project includes comprehensive end-to-end tests using Playwright. See [e2e/README.md](./e2e/README.md) for detailed documentation.

### Quick Start

1. Install dependencies: `npm install`
2. Install Playwright browsers: `npx playwright install`
3. Ensure staging backend is running on `localhost:8080`
4. Run tests: `npm run test:e2e`

### Available Commands

- `npm run test:e2e` - Run all E2E tests
- `npm run test:e2e:ui` - Run tests in interactive UI mode
- `npm run test:e2e:debug` - Run tests in debug mode
- `npm run test:e2e:capture-api` - Capture API responses from staging to generate test fixtures

For more information, see the [E2E Testing Guide](./e2e/README.md).

---

## React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
