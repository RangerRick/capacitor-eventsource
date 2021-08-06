import { WebPlugin } from '@capacitor/core';
import { EventSourceOptions, EventSourcePlugin, READY_STATE } from './definitions';

export class EventSourceWeb extends WebPlugin implements EventSourcePlugin {
  private url?: string;
  private eventSource?: EventSource | null;
  private opened = false;

  constructor() {
    super({
      name: 'EventSource',
      platforms: ['web'],
    });
  }

  async configure(options: EventSourceOptions): Promise<void> {
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
      throw new Error('You must call configure first!');
    }

    this.notifyListeners('readyStateChanged', {
      state: READY_STATE.CONNECTING,
    });

    this.eventSource = new window.EventSource(this.url);
    this.eventSource.onopen = this.onOpen.bind(this);
    this.eventSource.onmessage = this.onMessage.bind(this);
    this.eventSource.onerror = this.onError.bind(this);
  }

  onError(ev: any) {
    this.notifyListeners('error', { error: ev?.message });
  }

  onMessage(ev: MessageEvent) {
    if (this.opened) {
      this.notifyListeners('message', { message: ev?.data });
    }
  }

  onOpen(ev: any) {
    this.opened = true;
    this.notifyListeners('open', { message: ev?.message });
    this.notifyListeners('readyStateChanged', { state: READY_STATE.OPEN });
  }

  async close(): Promise<void> {
    console.debug(`EventSourceWeb.close()`);
    this.opened = false;
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource.onopen = null;
      this.eventSource.onmessage = null;
      this.eventSource.onerror = null;
      this.eventSource = null;
      this.notifyListeners('readyStateChanged', {
        state: READY_STATE.CLOSED,
      });
    }
  }
}

const EventSourcePluginImpl = new EventSourceWeb();

export { EventSourcePluginImpl as EventSource };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(EventSourcePluginImpl);
