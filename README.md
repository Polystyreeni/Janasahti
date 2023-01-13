# Janasahti

## FI:
Oma versio mobiilipelistä Sanajahti. Sovellus on toteutettu osana yliopiston Mobiiliohjelmointi-kurssia, jossa tavoitteena oli tutustua Android-ympäristöön. Tehtävänantona oli toteuttaa sovellus täysin vapaamuotoisen idean pohjalta. Projekti on toteutettu Android Studio ympäristöllä, Java-kielellä.
Sovelluksen tuetut kielet: suomi

**Ominaisuudet:**
- Pelilaudan haku ulkoiselta palvelimelta
- Parhaat pelaajat -tilasto jokaiselle pelilaudalle Firebasen kautta

**Käyttövaatimukset:**
- Android 6.0 tai uudempi (testattu Android-versioilla 6.0, 7.1.2 & 12)
- Internet-yhteys

Lähdekoodista testaaminen vaatii fyysisen testilaitteen internet-yhteydellä, tai Firebase-emulaattorin. Android-studion tarjoama emulaattori ei toimi Firebase-tietokannan kanssa suoraan.
Lisätietoja: https://firebase.google.com/codelabs/firestore-android/

## EN:
My own version of the mobile game Wordz. The application is made as a part of Mobile Developement university course, where the goal was to learn about the Android environment. The task was to create a mobile app of your choice. Project was made with Android Studio, using Java.
Application supported languages: Finnish

**Features:**
- Game board loading from exterior source
- Highscores unique to each game board (using Firebase)

**Requirements:**
- Android 6.0 or higher (tested with Android versions 6.0, 7.1.2 & 12)
- Internet connection

If testing source code, you need a physical Android-device with internet connection, or a separate Firebase-emulator. Android-studios provided emulator will not work out-of-the box.
More info: https://firebase.google.com/codelabs/firestore-android/