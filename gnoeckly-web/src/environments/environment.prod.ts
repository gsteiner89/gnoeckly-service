/** Produktion: echte Ad-Unit-IDs aus der AdMob-Konsole eintragen, isTesting auf false. */
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.gnoeckly.example',
  admob: {
    rewardedAdIdAndroid: 'ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN',
    rewardedAdIdIos: 'ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN',
    isTesting: false,
  },
};
