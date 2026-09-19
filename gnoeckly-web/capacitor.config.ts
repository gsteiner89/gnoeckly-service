import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'at.stoneforge.gnoeckly',
  appName: 'Gnöckly',
  webDir: 'dist/gnoeckly-web/browser',
  server: {
    // https-Schema, damit Secure-Context-APIs und Cookies wie im Web funktionieren;
    // Origin ist dann "https://localhost" (Android) bzw. "capacitor://localhost" (iOS) -
    // beide stehen in midgard.security.cors.allowed-origins des Backends.
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 800,
      launchAutoHide: true,
      backgroundColor: '#5B3DF5',
      androidScaleType: 'CENTER_CROP',
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#5B3DF5',
    },
  },
};

export default config;
