import { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/core' {
  interface PluginRegistry {
    EventSource: EventSourcePlugin;
  }
}

export interface EventSourceOptions {
  url: string;
  reconnectTime?: number;
  maxReconnectTime?: number;
  backoffResetThreshold?: number;
  idleTimeout?: number;
}

export interface OpenResult {
  value?: string;
}

export interface MessageResult {
  message?: string;
}

export interface ErrorResult {
  error?: string;
}

export enum READY_STATE {
  CONNECTING = 0,
  OPEN = 1,
  CLOSED = 2,
}

export interface ReadyStateChangedResult {
  state?: number;
}

export interface EventSourcePlugin {
  configure(options: EventSourceOptions): Promise<void>;
  open(): Promise<void>;
  close(): Promise<void>;

  removeAllListeners(): void;
  addListener(
    eventName: 'open',
    listenerFunc: (result: OpenResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'message',
    listenerFunc: (result: MessageResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'error',
    listenerFunc: (result: ErrorResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'readyStateChanged',
    listenerFunc: (result: ReadyStateChangedResult) => void,
  ): PluginListenerHandle;
}
