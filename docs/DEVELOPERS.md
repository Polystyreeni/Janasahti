# Developer instructions
If you wish to contribute or develop your own fork of this application, here are some instructions on how to get the app running from source.

## Prerequisities
- [Android Studio](https://developer.android.com/studio) (app is build with version Arctic Fox 2022.3.1, but newer version will likely work)
- A physical Android device with Android 6.0 (API level 23) or newer
  - An emulator may work, but it will require some setup to get Firebase working. This has not been tested!
- A [Firebase](https://firebase.google.com/) user account
- An online service to host game files (for example Google Drive, Github Gist, etc.)

## Getting started
1. Clone the repository using Git or download the source code directly.
2. Configure the application using the below instructions

## Configuring the app
Application properties are provided as key-value pairs. Set your application properties in `/app/src/main/assets/app_config.properties`:
- appVersion (MANDATORY)
  - Version of the application. This version is compared to remote configuration files and will trigger the update prompt if versions are not matching.
  - **Format**: X.Y.Z(.B), beta version is optional. Versions are handled so that Z number maintains version compatibility. If X or Y changes, version update is mandatory in order to use the application.
- configurationUrls (MANDATORY)
  - Comma separated list of web URLs where to fetch app configuration from
  - NOTE: Make sure to separate URLs with comma and space (", ") so the parser can distinguish between URL-included commas and list separators!
- localFileCacheExpirationTimeMs (MANDATORY)
  - Max time to use a cached remote configuration in the app. After expiration, remote configuration is fetched again from web URL.
  - **Format**: Long value
- supportEmail (OPTIONAL)
  - Support email. This is used in the application About page
  - **Format**: Email address (for example: `john.doe@email.com`)
- supportGitHub (OPTIONAL)
  - Support Github user. This is used in the application About page.
  - **Format**: User name string (NOTE: User name only, GitHub prefix is added automatically)
- gameSourceLink (OPTIONAL)
  - Link where users can download the application (used in version update popups)
  - **Format**: Web URL

## Remote app configuration
The following properties are recognized in remote configuration
- latestVersion
  - Version of the configuration file (or the latest app version)
  - This version will trigger an update prompt if it differs from the app version
  - **Format**: X.Y.Z(.B)
- boardListUrl
  - List of URLs where to fetch board files from. See board file format here.
  - **Format**: Web URL
- boardsCompressed
  - If true, board files a zip compressed. If false, board files are assumed to be plain XML text. See board file format below.
  - **Format**: (true/false)
- boardsPerLoad
  - Number of boards to load in memory from one file read.
  - **Format**: Integer number
- useFirebase
  - Whether to use Firebase leaderboards in the application
  - **Format**: true/false
- scoreBoardMaxPlayers
  - UNUSED
- messageUrl
  - Url where to fetch message notifications. See message file format below.
  - **Format**: Web URL

Remote configuration can be used to override local properties. When you provide a property in the remote config that has the same key as a local property, an override local property is created, which will be prioritized over the property in app_config.properties.


## Firebase configuration
The application uses Firebase for storing leaderboards data and registering new users. See instructions [here](https://firebase.google.com/docs/android/setup) on how to set up an Android project.
- The package name of the application by default is `com.example.wordgame` (I know, fancy right?)


## Board file format
Uncompressed board file format is the following XML:
- count
  - Number of board elements in the file
- board
  - One board element containing the letters (tiles) in a board and all possible words
- board>tiles
  - String where the game board is created. Board is populated from left to right, top to bottom order. 
- board>words
  - word element of a board file
- board>words>word
  - word element of a board file. Contains the actual word, and the maximum coordinates of the word on the game board.

Word coordinate info `xMax` and `yMax` are used to determine if the word is present in 4x4-based gamemodes. By default, all board files are 5x5 and are reduced at runtime to smaller boards depending on the game mode.

Below is an example of an XML containing one board:
```
<?xml version="1.0" encoding="UTF-8"?>
<count>1</count>
<board>
  <tiles>rmaanahlgeongvtnnojplubvl</tiles>
  <words>
    <word word="halma" xMax="2" yMax="1" />
    <word word="noh" xMax="1" yMax="2" />
    <word word="luo" xMax="2" yMax="4" />
    <word word="haa" xMax="3" yMax="1" />
    <word word="nuo" xMax="2" yMax="4" />
    <word word="maa" xMax="3" yMax="0" />
    <word word="laho" xMax="2" yMax="2" />
    <word word="harmaa" xMax="3" yMax="1" />
    <word word="luona" xMax="2" yMax="4" />
    <word word="luonnonharmaa" xMax="3" yMax="4" />
    <word word="arho" xMax="1" yMax="2" />
    <word word="ahma" xMax="2" yMax="1" />
    <word word="ala" xMax="3" yMax="1" />
    <word word="ane" xMax="4" yMax="1" />
    <word word="naama" xMax="4" yMax="1" />
    <word word="lahna" xMax="2" yMax="2" />
    <word word="raha" xMax="2" yMax="1" />
    <word word="mar" xMax="1" yMax="1" />
    <word word="alho" xMax="2" yMax="2" />
    <word word="nega" xMax="4" yMax="1" />
    <word word="mahla" xMax="2" yMax="1" />
    <word word="maho" xMax="2" yMax="2" />
    <word word="aho" xMax="2" yMax="2" />
    <word word="ahaa" xMax="3" yMax="1" />
    <word word="magna" xMax="4" yMax="1" />
    <word word="mango" xMax="2" yMax="3" />
    <word word="jono" xMax="3" yMax="3" />
    <word word="hamaan" xMax="4" yMax="1" />
    <word word="lama" xMax="2" yMax="1" />
    <word word="ohra" xMax="1" yMax="2" />
    <word word="maha" xMax="2" yMax="1" />
    <word word="nunna" xMax="1" yMax="4" />
    <word word="laama" xMax="3" yMax="1" />
  </words>
</board>
```
Compressed board files are simply these xml-files zip-compressed. For example in Windows, these files can be created by using `Send to > Compressed/Zipped folder`.

A Java application for generating board files exists and can be downloaded HERE TODO LINK.


## Daily message format
Daily messages can be used to send information popups for app users. The popup is displayed in the app main menu, when remote configuration is fetched.

Message files are plain text files with the following format:
`<id>|<version>|<header>|<content>`, where
`id` = unique message id. Used to check whether the user has already seen this message. Change this to make the message be considered 'new'.
`version` = Game version this message applies to.
`header` = Header of the message, shown in the popup header
`content` = Text content of the message. 

The messaging system supports multiple messages in the same file. This is useful when you need to have separate messages for different game versions. For example, the following message would show different messages for users of versions 2.0.0 and 2.0.1:
`id1|2.0.0|Hello 2.0.0|Message for 2.0.0|id1|2.0.1|Hello 2.0.1|Message for 2.0.1`

Message content text can be formatted with limited markdown support (that is supported by Android Spannable). Some examples include:
- `<br>`: Line break
- `<b></b>`: Bold text
- `<i></i>`: Italic text
- `<b>`: Bold text


