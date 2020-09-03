import { PluginListenerHandle } from '@capacitor/core';

declare module '@capacitor/core' {
  interface PluginRegistry {
    EventSource: EventSourcePlugin;
  }
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
  configure(options: { url: string }): Promise<void>;
  open(): Promise<void>;
  close(): Promise<void>;

  addListener(
    eventName: 'onOpen',
    listenerFunc: (result: OpenResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'onMessage',
    listenerFunc: (result: MessageResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'onError',
    listenerFunc: (result: ErrorResult) => void,
  ): PluginListenerHandle;
  addListener(
    eventName: 'onReadyStateChanged',
    listenerFunc: (result: ReadyStateChangedResult) => void,
  ): PluginListenerHandle;
}
