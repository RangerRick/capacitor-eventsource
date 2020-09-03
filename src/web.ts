import { WebPlugin } from '@capacitor/core';
import { EventSourcePlugin, READY_STATE } from './definitions';

export class EventSourceWeb extends WebPlugin implements EventSourcePlugin {
  private url?: string;
  private eventSource?: EventSource;

  constructor() {
    super({
      name: 'EventSource',
      platforms: ['web'],
    });
  }

  async configure(options: { url: string }): Promise<void> {
    console.debug(`EventSourceWeb.configure(${options.url})`);
    if (options.url) {
      this.url = options.url;
    } else {
      throw new Error('url is required');
    }
  }

  async open(): Promise<void> {
    console.debug(`EventSourceWeb.open()`);
    if (!this.url) {
      throw new Error('call configure first');
    }

    this.eventSource = new window.EventSource(this.url);
    this.notifyListeners('readyStateChanged', {
      state: READY_STATE.CONNECTING,
    });

    this.eventSource.addEventListener('error', (ev: any) => {
      this.notifyListeners('error', { error: ev?.message });
    });
    this.eventSource.addEventListener('message', (ev: MessageEvent) => {
      this.notifyListeners('message', { message: ev?.data });
    });
    this.eventSource.addEventListener('open', (ev: any) => {
      this.notifyListeners('open', { message: ev?.message });
      this.notifyListeners('readyStateChanged', { state: READY_STATE.OPEN });
    });
  }

  async close(): Promise<void> {
    console.debug(`EventSourceWeb.close()`);
    if (this.eventSource) {
      this.eventSource.close();
      this.notifyListeners('readyStateChanged', {
        state: READY_STATE.CLOSED,
      });
    }
  }
}

const EventSource = new EventSourceWeb();

export { EventSource };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(EventSource);
