import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModule,
  NativeModules,
  Platform,
} from 'react-native';

const LINKING_ERROR =
  `The package 'reactnativeopenvpn' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RNOpenVpn =
  NativeModules.RNOpenVpn ??
  new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    }
  );
export interface RNOpenVpnInterface extends NativeModule {
  connect: (
    config: string,
    username: string,
    password: string
  ) => Promise<null>;
  disconnect: () => Promise<null>;
  getMyIP: () => Promise<string>;
  prepare: () => Promise<null>;
  getCurrentState: () => Promise<{ state: VPN_STATE; msg: string }>;
}

declare module 'react-native' {
  interface NativeModulesStatic {
    RNOpenVpn: RNOpenVpnInterface;
  }
}

export const eventEmitted: NativeEventEmitter = new NativeEventEmitter(
  RNOpenVpn
);

export enum VPN_STATE {
  /***************** PAUSED *********************/
  /* VPN paused requested by user */
  USER_PAUSE = 'USERPAUSE',
  /* VPN paused because screen is off */
  SCREEN_OFF = 'SCREENOFF',

  /**************** CONNECTING *******************/
  /******** PENDING REPLY ********/
  /* VPN is connecting */
  CONNECTING = 'CONNECTING',
  /* Waiting for server reply */
  WAIT = 'WAIT',
  /* Reconnecting */
  RECONNECTING = 'RECONNECTING',
  /* Resolving host names */
  RESOLVE = 'RESOLVE',
  /* Connecting (TCP) */
  TCP_CONNECT = 'TCP_CONNECT',
  /* Waiting for usable network */
  NO_NETWORK = 'NONETWORK',
  /* Waiting between connection attempt */
  CONNECT_RETRY = 'CONNECTRETRY',
  /* Waiting for Orbot to start */
  WAIT_ORBOT = 'WAIT_ORBOT',

  /******** GOT REPLY ********/
  /* building configration */
  VPN_GENERATE_CONFIG = 'VPN_GENERATE_CONFIG',
  /* Getting client configuration */
  GET_CONFIG = 'GET_CONFIG',
  /* Assigning IP addresses */
  ASSIGN_IP = 'ASSIGN_IP',
  /* Adding routes */
  ADD_ROUTES = 'ADD_ROUTES',
  /* Authentication pending */
  AUTH_PENDING = 'AUTH_PENDING',
  /* Authenticating */
  AUTH = 'AUTH',

  /***************** CONNECTED *******************/
  /* Connected */
  CONNECTED = 'CONNECTED',

  /**************** DISCONNECTED *****************/
  /* Not running */
  NO_PROCESS = 'NOPROCESS',
  /* Disconnected */
  DISCONNECTED = 'DISCONNECTED',
  /* Exiting */
  EXITING = 'EXITING',
  /* Authentication failed */
  AUTH_FAILED = 'AUTH_FAILED',
  /* Cannot connect because it needs something */
  NEED = 'NEED',
}

// receive state change from VPN service.
export const STATE_CHANGED_EVENT_NAME: string = 'STATE_CHANGED';
// receive ip change from VPN service.
export const IP_CHANGED_EVENT_NAME: string = 'IP_CHANGED';
// receive ip change from VPN service.
export const LOG_EVENT_NAME: string = 'LOG';

// remove change listener
export const removeOnStateChangeListener: (
  eventEmitterSubscription: EmitterSubscription
) => void = (eventEmitterSubscription) => {
  eventEmitterSubscription.remove();
};

// set a state change listener
export const onStateChangeListener: (
  callback: (state: VPN_STATE, msg: string) => void
) => EmitterSubscription = (callback) => {
  return eventEmitted.addListener(
    STATE_CHANGED_EVENT_NAME,
    (event: { state: VPN_STATE; msg: string }) =>
      callback(event.state, event.msg)
  );
};

// set an ip change listener
export const onIpChangeListener: (
  callback: (ip: string) => void
) => EmitterSubscription = (callback) => {
  return eventEmitted.addListener(
    IP_CHANGED_EVENT_NAME,
    (event: { ip: string }) => callback(event.ip)
  );
};

// set an ip change listener
export const onLogListener: (
  callback: (log: string) => void
) => EmitterSubscription = (callback) => {
  return eventEmitted.addListener(LOG_EVENT_NAME, (event: { log: string }) =>
    callback(event.log)
  );
};

// prepare vpn.
// must call on useEffect hook or componentDidMount lifecycle.
export const prepare = NativeModules.RNOpenVpn.prepare;

// connect to VPN.
// this will create a background VPN service.
export const connect = NativeModules.RNOpenVpn.connect;

// get your ip
export const getMyIP = NativeModules.RNOpenVpn.getMyIP;

// disconnect and stop VPN service.
export const disconnect = NativeModules.RNOpenVpn.disconnect;

// get current state
export const getCurrentState = NativeModules.RNOpenVpn.getCurrentState;

export default NativeModules.RNOpenVpn;
