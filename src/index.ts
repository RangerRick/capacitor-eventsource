import { registerPlugin } from '@capacitor/core';

import type { EventSourcePlugin } from './definitions';

const EventSource = registerPlugin<EventSourcePlugin>('EventSource', {
  web: () => import('./web').then((m) => new m.EventSourceWeb()),
});

export * from './definitions';
export { EventSource };
