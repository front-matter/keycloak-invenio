import type { Preview } from "@storybook/react-vite";

import { INITIAL_VIEWPORTS } from 'storybook/viewport';
 
const preview: Preview = {
  parameters: {
    viewport: {
      options: INITIAL_VIEWPORTS,
    },
  },
  initialGlobals: {
    viewport: { value: 'ipad', isRotated: false },
  },
};

export default preview;
