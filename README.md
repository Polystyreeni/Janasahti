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

**Kokoaminen ja projektin konfigurointi:**

Vaatimukset:
  * [Android Studio](https://developer.android.com/studio) (sovellus rakennettu versiolla Arctic Fox 2020.3.1)
  * Android 6.0 (API level 23) tai uudempi, fyysinen testilaite
  * Pelilauta- ja versiotiedot julkisesti ladattavissa URL-osoitteen kautta (esimerkiksi Drive, OneDrive jne.)
    * Pelilautojen tekemiseen on oma ohjelma, jonka voi ladata [Täältä](https://github.com/Polystyreeni/JanasahtiBoardGenerator)
  * [Firebase](https://firebase.google.com/) käyttäjätili
  
Ohjeet kokoamiseen alla:
1. Kloonaa repositorio tai lataa lähdekooditiedostot
2. Aseta omat URL-osoitteet NetworkConfig-tiedostoon, joka löytyy polusta `Janasahti\Wordgame\app\src\main\java\com\example\wordgame\NetworkConfig.java`
    * Kohtaan `VERSION_URL`, lisää URL versiotiedostoon. Tämä tiedoston pitää olla tekstitiedosto, jossa versionumero muodossa `X.X.X`
    * Kohtaan `BOARD_LIST_URL`, lisää URL tiedostoon, joka sisältää linkit pelilautatiedostoihin, yksi linkki per rivi.
    * Pelilautatiedostot ovat XML-muotoisia tekstitiedostoja, joista esimerkki löytyy [Täältä](https://drive.google.com/file/d/1I8nfebuCtRsj0iCMPMlPcpdL22e3Xp7A/view?usp=sharing).
Tiedostojen säilyttämiseen voidaan käyttää esimerkiksi Google Drive tai OneDrive palvelua. Edellä listattujen linkkien on oltava suoria latauslinkkejä. Google Driven tapauksessa tiedostoille on muodostettava suorat latauslinkit, jotka voidaan muodostaa [Tällä palvelulla](https://sites.google.com/site/gdocs2direct/).
3. Sovelluksen rekisteröinti Firebaseen. Tarkat ohjeet sovelluksen lisäämisestä löytyvät [Täältä](https://firebase.google.com/docs/android/setup). Lähdekoodin mukana tuleva google-settings.json on mallitiedosto, joka ei toimi sovelluksessa. Tämän koodin kokoajan on itse rekisteröitävä Firebase-projekti ja korvattava tämä tiedosto itse luodulla json-konfiguraatiotiedosolla. 
    * Sovellus toimii myös ilman Firebase-ominaisuuksia, kunhan vaaditut riippuvuudet on määritetty sovelluksen konfiguraatiotiedostoihin. Firebase-ominaisuudet voi poistaa käytöstä asettamalla `GameSettings.java` tiedostosta vakion `useFirebase` arvoksi `false`. Firebase-ominaisuudet poistamalla pistetilastot eivät toimi.
4. Aja sovellus testilaitteella, varmista että laitteella on Internet-yhteys. 
    * Sovellus ei toimi Android Studion tarjoamalla emulaattorilla. Lisätietoja Firebase-sovelluksen emuloinnista [Täällä](https://firebase.google.com/codelabs/firestore-android/).


## EN:
My own version of the mobile game Wordz. The application is made as a part of Mobile Developement university course, where the goal was to learn about the Android environment. The task was to create a mobile app of your choice. Project was made with Android Studio, using Java.
Application supported languages: Finnish

**Features:**
- Game board loading from exterior source
- Highscores unique to each game board (using Firebase)

**Requirements:**
- Android 6.0 or higher (tested with Android versions 6.0, 7.1.2 & 12)
- Internet connection


**Building ja project configuration:**

Requirements:
  * [Android Studio](https://developer.android.com/studio) (app build using version Arctic Fox 2020.3.1)
  * Android 6.0 (API level 23) or newer supported physical device
  * Game board- and version files publically available via internet (can be hosted for example in Drive, OneDrive etc.)
     * There is a program provided for generating boards, which can be downloaded [Here](https://github.com/Polystyreeni/JanasahtiBoardGenerator)
  * [Firebase](https://firebase.google.com/) account
  
Instructions for compiling below:
1. Clone the repository, or download source files
2. Set your own URL-addresses to the NetworkConfig file located in `Janasahti\Wordgame\app\src\main\java\com\example\wordgame\NetworkConfig.java`
    * In `VERSION_URL`, add your URL to the version file. This URL should download a text file with the version number in format `X.X.X`
    * In `BOARD_LIST_URL`, add your URL to a file that contains links to the game board files, one link per line.
    * Game board files are XML text files, an example file can be found [Here](https://drive.google.com/file/d/1I8nfebuCtRsj0iCMPMlPcpdL22e3Xp7A/view?usp=sharing).
For storing files one can for example use Google Drive or OneDrive. Aforementioned links must be direct download links. When using Google Drive, direct download links can be generated using [This service](https://sites.google.com/site/gdocs2direct/).
3. Adding project to Firebase. Step-by-step instruction can be found [Here](https://firebase.google.com/docs/android/setup). The provided google-settings.json is a dummy file, which does not work. You must register a Firebase project yourself and replace the json-configuration file with a proper file. 
    * The app will also work without Firebase services, as long as the Gradle-dependencies are correct. You can disable Firebase features in the `GameSettings.java` file by setting `useFirebase` to `false`. By removing Firebase features, scoreboards stop working, but the game is still playable.
4. Run the app on the test device. Make sure your device has internet connection.
    * If testing source code, you need a physical Android-device with internet connection, or a separate Firebase-emulator. Android-studios provided emulator will not work out-of-the box. More info [Here](https://firebase.google.com/codelabs/firestore-android/).
